/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Modified: ported from Kotlin/Forge 1.16.5 to Java 21/NeoForge 1.21.1 (2026-10-19).
 * Modified: horizontal branch arms added to the existing pipe block (2026-11-02, 1.1.0).
 * Modified: links to every neighbouring pipe when sneak-placed, empty-hand link toggle, hitbox of tube-only pipes (2026-11-03, 1.1.1).
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.block;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thanhttn1.pollutantpump.reforged.core.config.PumpConfig;
import thanhttn1.pollutantpump.reforged.core.tower.BranchNetwork;
import thanhttn1.pollutantpump.reforged.core.tower.PipeTower;

/**
 * One segment of a pump's pipe tower. The topmost segment is drawn half height with a grate, and right-clicking any
 * segment with another pipe extends the tower upwards.
 *
 * <p>Since 1.1.0 a pipe can also carry up to four horizontal arms. Sneak-placing a pipe against the side of another
 * pipe links the two, so a branch can leave the tower at any height and reach another floor. Arms are plain block
 * state flags: a pipe without arms behaves exactly like before.
 */
public class PipeBlock extends Block {

    /** Visual variant: the top of a tower is a short grated segment. */
    public enum PipeSize implements StringRepresentable {
        FULL("full"),
        HALF("half");

        private final String serializedName;

        PipeSize(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final MapCodec<PipeBlock> CODEC = simpleCodec(PipeBlock::new);
    public static final EnumProperty<PipeSize> SIZE = EnumProperty.create("size", PipeSize.class);

    private static final VoxelShape SHAPE_FULL = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape SHAPE_HALF = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);

    /** Arm flags in direction index order: north, east, south, west (same order as the branch graph search). */
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    /**
     * Vertical links, only used to pick the model: a pipe with a pipe above or a pipe/pump below is drawn as the
     * tower pillar, a pipe with arms and no vertical link is drawn as a small core with arms (like a cable).
     */
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape[] ARM_SHAPES = {
            Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 4.0),
            Block.box(12.0, 4.0, 4.0, 16.0, 12.0, 12.0),
            Block.box(4.0, 4.0, 12.0, 12.0, 12.0, 16.0),
            Block.box(0.0, 4.0, 4.0, 4.0, 12.0, 12.0)
    };

    /** Tubes of a pipe without a pillar run from the block edge to the block centre, where a branch tip ends. */
    private static final VoxelShape[] ARM_LONG_SHAPES = {
            Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 8.0),
            Block.box(8.0, 4.0, 4.0, 16.0, 12.0, 12.0),
            Block.box(4.0, 4.0, 8.0, 12.0, 12.0, 16.0),
            Block.box(0.0, 4.0, 4.0, 8.0, 12.0, 12.0)
    };

    /** Shapes of the tube-only look, indexed by arm bit mask (same shape as the model, so the tip can be clicked). */
    private static final VoxelShape[] CORE_SHAPES = buildCoreShapes();

    private static VoxelShape[] buildCoreShapes() {
        VoxelShape[] shapes = new VoxelShape[16];
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = Shapes.empty();
            for (int dir = 0; dir < 4; dir++) {
                if ((mask & (1 << dir)) != 0) {
                    shape = Shapes.or(shape, ARM_LONG_SHAPES[dir]);
                }
            }
            shapes[mask] = shape.optimize();
        }
        return shapes;
    }

    /** Every shape is built once: index = size ordinal * 16 + arm bit mask. */
    private static final VoxelShape[] SHAPES = buildShapes();

    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[PipeSize.values().length * 16];
        for (PipeSize size : PipeSize.values()) {
            for (int mask = 0; mask < 16; mask++) {
                VoxelShape shape = size == PipeSize.FULL ? SHAPE_FULL : SHAPE_HALF;
                for (int dir = 0; dir < 4; dir++) {
                    if ((mask & (1 << dir)) != 0) {
                        shape = Shapes.or(shape, ARM_SHAPES[dir]);
                    }
                }
                shapes[size.ordinal() * 16 + mask] = shape.optimize();
            }
        }
        return shapes;
    }

    /** @return the arm flag for a horizontal direction */
    public static BooleanProperty armProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Pipes only have horizontal arms: " + direction);
        };
    }

    private static int armMask(BlockState state) {
        int mask = 0;
        if (state.getValue(NORTH)) {
            mask |= 1;
        }
        if (state.getValue(EAST)) {
            mask |= 2;
        }
        if (state.getValue(SOUTH)) {
            mask |= 4;
        }
        if (state.getValue(WEST)) {
            mask |= 8;
        }
        return mask;
    }

    private static boolean hasAnyArm(BlockState state) {
        return armMask(state) != 0;
    }

    public PipeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SIZE, PipeSize.FULL)
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIZE, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = armMask(state);
        if (mask != 0 && !state.getValue(UP) && !state.getValue(DOWN)) {
            return CORE_SHAPES[mask];
        }
        return SHAPES[state.getValue(SIZE).ordinal() * 16 + mask];
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placed = defaultBlockState();
        // Sneak-placing against the side of a pipe links the new pipe to every pipe beside it, so a pipe put into the
        // gap of a broken branch rejoins both halves. The other pipes learn about the link through updateShape, so no
        // neighbour has to be modified here.
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis() != Direction.Axis.Y && PumpConfig.BRANCH_PIPES_ENABLED.getAsBoolean()) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockState neighbour = context.getLevel().getBlockState(context.getClickedPos().relative(side));
                if (neighbour.getBlock() instanceof PipeBlock) {
                    placed = placed.setValue(armProperty(side), true);
                }
            }
        }
        return withResolvedSize(context.getLevel(), context.getClickedPos(), placed);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis() != Direction.Axis.Y) {
            BlockState withArm = withResolvedArm(state, direction, neighborState);
            // A pipe with arms is never the short grated top, so the size is re-checked when an arm appears.
            return withArm == state ? state : withResolvedSize(level, pos, withArm);
        }
        // Vertical neighbours decide whether this segment is the grated top.
        // The changed neighbour is already given to us, only the other vertical neighbour needs a lookup.
        BlockState below = direction == Direction.DOWN ? neighborState : level.getBlockState(pos.below());
        BlockState above = direction == Direction.UP ? neighborState : level.getBlockState(pos.above());
        return withResolvedSize(state, below, above);
    }

    /**
     * An arm stays while the pipe next to it exists, and appears when that pipe points an arm at us. It never appears
     * on its own, so placing two pipes side by side without sneaking does not link them.
     */
    private static BlockState withResolvedArm(BlockState state, Direction direction, BlockState neighborState) {
        BooleanProperty arm = armProperty(direction);
        boolean current = state.getValue(arm);
        boolean connected = neighborState.getBlock() instanceof PipeBlock
                && (current || neighborState.getValue(armProperty(direction.getOpposite())));
        return connected == current ? state : state.setValue(arm, connected);
    }

    private static BlockState withResolvedSize(BlockGetter level, BlockPos pos, BlockState state) {
        return withResolvedSize(state, level.getBlockState(pos.below()), level.getBlockState(pos.above()));
    }

    /** A segment is the short top piece when it stands on a pipe or pump and has no pipe above it. */
    private static BlockState withResolvedSize(BlockState state, BlockState below, BlockState above) {
        boolean standsOnTower = PipeTower.isPipe(below) || below.getBlock() instanceof PumpBlock;
        boolean isTop = standsOnTower && !PipeTower.isPipe(above) && armMask(state) == 0;
        return state.setValue(SIZE, isTop ? PipeSize.HALF : PipeSize.FULL)
                .setValue(UP, PipeTower.isPipe(above))
                .setValue(DOWN, standsOnTower);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Property-only changes (full <-> half) also call onPlace; only a newly placed pipe changes the tower.
        if (level.isClientSide || state.is(oldState.getBlock())) {
            return;
        }
        PipeTower.notifyPumpBelow(level, pos);
        if (hasAnyArm(state)) {
            BranchNetwork.notifyPumps(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            PipeTower.notifyPumpBelow(level, pos);
            if (hasAnyArm(state)) {
                BranchNetwork.notifyPumpsAround(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Sneak + empty hand on the side of a pipe that has another pipe next to it on that side links or unlinks the two.
     * This repairs a branch without breaking anything.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        Direction side = hitResult.getDirection();
        if (!player.isShiftKeyDown() || side.getAxis() == Direction.Axis.Y
                || !PumpConfig.BRANCH_PIPES_ENABLED.getAsBoolean()) {
            return InteractionResult.PASS;
        }
        BlockPos otherPos = pos.relative(side);
        BlockState other = level.getBlockState(otherPos);
        if (!(other.getBlock() instanceof PipeBlock)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean link = !(state.getValue(armProperty(side)) || other.getValue(armProperty(side.getOpposite())));
        BlockState newState = withResolvedSize(level, pos, state.setValue(armProperty(side), link));
        BlockState newOther = withResolvedSize(level, otherPos, other.setValue(armProperty(side.getOpposite()), link));
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        level.setBlock(otherPos, newOther, Block.UPDATE_ALL);
        BranchNetwork.notifyPumps(level, pos);
        BranchNetwork.notifyPumps(level, otherPos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(asItem())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockPos topPipe = PipeTower.findTop(level, pos);
        BlockPos extensionPos = (topPipe == null ? pos : topPipe).above();
        if (!canExtendTowerAt(level, player, extensionPos)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockState extension = withResolvedSize(level, extensionPos, defaultBlockState());
        if (!level.setBlock(extensionPos, extension, Block.UPDATE_ALL)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        playPlaceSound(level, extensionPos, state);
        stack.consume(1, player);
        return ItemInteractionResult.SUCCESS;
    }

    private static boolean canExtendTowerAt(Level level, Player player, BlockPos extensionPos) {
        return !level.isOutsideBuildHeight(extensionPos)
                && level.isEmptyBlock(extensionPos)
                && player.mayBuild()
                && level.mayInteract(player, extensionPos);
    }

    private static void playPlaceSound(Level level, BlockPos pos, BlockState state) {
        SoundType soundType = state.getSoundType();
        // Slightly lower pitch, like the original mod, so tower building sounds heavier than normal placement.
        level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide) {
            removeTowerAbove(level, pos, player, willHarvest);
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    /** Breaking a pipe collapses every pipe above it; survival players get them all back at the broken spot. */
    private void removeTowerAbove(Level level, BlockPos brokenPos, Player player, boolean willHarvest) {
        BlockPos.MutableBlockPos cursor = brokenPos.mutable().move(Direction.UP);
        int removedPipes = 0;
        int remainingSteps = level.getHeight();
        while (remainingSteps-- > 0 && level.getBlockState(cursor).is(this)) {
            level.destroyBlock(cursor, false);
            removedPipes++;
            cursor.move(Direction.UP);
        }
        if (removedPipes > 0 && willHarvest && !player.isCreative()) {
            popPipes(level, brokenPos, removedPipes);
        }
    }

    private void popPipes(Level level, BlockPos pos, int pipeCount) {
        int maxStackSize = asItem().getDefaultMaxStackSize();
        for (int remaining = pipeCount; remaining > 0; remaining -= maxStackSize) {
            popResource(level, pos, new ItemStack(this, Math.min(remaining, maxStackSize)));
        }
    }
}
