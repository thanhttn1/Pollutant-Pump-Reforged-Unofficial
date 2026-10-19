/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Modified: ported from Kotlin/Forge 1.16.5 to Java 21/NeoForge 1.21.1 (2026-10-19).
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thanhttn1.pollutantpump.reforged.core.tower.PipeTower;

/**
 * One segment of a pump's pipe tower. The topmost segment is drawn half height with a grate, and right-clicking any
 * segment with another pipe extends the tower upwards.
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

    public PipeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SIZE, PipeSize.FULL));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIZE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SIZE) == PipeSize.FULL ? SHAPE_FULL : SHAPE_HALF;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withResolvedSize(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Only vertical neighbours decide whether this segment is the grated top.
        if (direction.getAxis() != Direction.Axis.Y) {
            return state;
        }
        return withResolvedSize(level, pos, state);
    }

    /** A segment is the short top piece when it stands on a pipe or pump and has no pipe above it. */
    private static BlockState withResolvedSize(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState below = level.getBlockState(pos.below());
        BlockState above = level.getBlockState(pos.above());
        boolean standsOnTower = PipeTower.isPipe(below) || below.getBlock() instanceof PumpBlock;
        boolean isTop = standsOnTower && !PipeTower.isPipe(above);
        return state.setValue(SIZE, isTop ? PipeSize.HALF : PipeSize.FULL);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Property-only changes (full <-> half) also call onPlace; only a newly placed pipe changes the tower.
        if (level.isClientSide || state.is(oldState.getBlock())) {
            return;
        }
        PipeTower.notifyPumpBelow(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            PipeTower.notifyPumpBelow(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
