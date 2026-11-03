/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Added in 1.1.0 (2026-11-02): adapts BranchGraph to a real level.
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.tower;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thanhttn1.pollutantpump.reforged.core.block.PipeBlock;
import thanhttn1.pollutantpump.reforged.core.block.PumpBlock;
import thanhttn1.pollutantpump.reforged.core.blockentity.PumpBlockEntity;
import thanhttn1.pollutantpump.reforged.core.config.PumpConfig;

/**
 * Finds the branches of a pump tower. Everything here only reads blocks in chunks that are already loaded, so a long
 * branch can never make the server load a chunk from inside a block entity tick.
 */
public final class BranchNetwork {

    /** Upper bound of pipes visited when looking for the pumps a changed branch belongs to, tower included. */
    private static final int NOTIFY_EXTRA_NODES = 512;

    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private BranchNetwork() {
    }

    /** A branch suction mouth: where to scan from and how many branch pipes lie between it and the tower. */
    public record Head(BlockPos pos, int pipeCount) {
    }

    public static final Head[] NO_HEADS = new Head[0];

    /** @return the open ends of all branches of the tower above {@code pumpPos}, empty when there are none */
    public static Head[] findHeads(LevelReader level, BlockPos pumpPos, BlockPos towerTop) {
        if (!PumpConfig.BRANCH_PIPES_ENABLED.getAsBoolean() || towerTop.getY() <= pumpPos.getY()) {
            return NO_HEADS;
        }
        List<BranchGraph.Head> found = BranchGraph.findHeads(new LevelProbe(level), pumpPos.getX(), pumpPos.getY(),
                pumpPos.getZ(), towerTop.getY(), PumpConfig.MAX_BRANCH_PIPES.getAsInt());
        if (found.isEmpty()) {
            return NO_HEADS;
        }
        Head[] heads = new Head[found.size()];
        for (int i = 0; i < heads.length; i++) {
            BranchGraph.Head head = found.get(i);
            heads[i] = new Head(new BlockPos(head.x(), head.y(), head.z()), head.pipeCount());
        }
        return heads;
    }

    /**
     * Tells every pump whose network contains {@code start} (or a pipe next to it) to rescan. Called when a pipe with
     * an arm is placed or removed; other branch changes are picked up by the pump's periodic rescan.
     */
    public static void notifyPumps(Level level, BlockPos start) {
        if (!PumpConfig.BRANCH_PIPES_ENABLED.getAsBoolean()) {
            return;
        }
        LevelProbe probe = new LevelProbe(level);
        List<int[]> nodes = BranchGraph.collect(probe, start.getX(), start.getY(), start.getZ(),
                PumpConfig.MAX_BRANCH_PIPES.getAsInt() + NOTIFY_EXTRA_NODES);
        List<BlockPos> notified = new ArrayList<>(1);
        for (int[] node : nodes) {
            BlockPos below = new BlockPos(node[0], node[1] - 1, node[2]);
            if (!level.hasChunkAt(below) || !(level.getBlockState(below).getBlock() instanceof PumpBlock)
                    || notified.contains(below)) {
                continue;
            }
            notified.add(below);
            BlockEntity blockEntity = level.getBlockEntity(below);
            if (blockEntity instanceof PumpBlockEntity pump) {
                pump.markTowerDirty();
            }
        }
    }

    /** Same as {@link #notifyPumps(Level, BlockPos)} for each pipe next to a removed or changed one. */
    public static void notifyPumpsAround(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            notifyPumps(level, pos.relative(direction));
        }
    }

    private static final class LevelProbe implements BranchGraph.Probe {
        private final LevelReader level;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        private LevelProbe(LevelReader level) {
            this.level = level;
        }

        private BlockState stateAt(int x, int y, int z) {
            cursor.set(x, y, z);
            if (level.isOutsideBuildHeight(cursor) || !level.hasChunkAt(cursor)) {
                return null;
            }
            return level.getBlockState(cursor);
        }

        @Override
        public boolean isPipe(int x, int y, int z) {
            BlockState state = stateAt(x, y, z);
            return state != null && PipeTower.isPipe(state);
        }

        @Override
        public int armMask(int x, int y, int z) {
            BlockState state = stateAt(x, y, z);
            if (state == null || !(state.getBlock() instanceof PipeBlock)) {
                return 0;
            }
            int mask = 0;
            for (int dir = 0; dir < 4; dir++) {
                if (state.getValue(PipeBlock.armProperty(HORIZONTAL[dir]))) {
                    mask |= 1 << dir;
                }
            }
            return mask;
        }

        @Override
        public boolean arm(int x, int y, int z, int dir) {
            BlockState state = stateAt(x, y, z);
            return state != null && state.getBlock() instanceof PipeBlock
                    && state.getValue(PipeBlock.armProperty(HORIZONTAL[dir]));
        }
    }
}
