/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Modified: ported from Kotlin/Forge 1.16.5 to Java 21/NeoForge 1.21.1 (2026-10-19).
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.tower;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import thanhttn1.pollutantpump.reforged.api.tag.PumpTags;
import thanhttn1.pollutantpump.reforged.core.blockentity.PumpBlockEntity;

/**
 * Helpers for the vertical stack of pipes that stands on top of a pump. Pipe blocks are recognised through the
 * {@link PumpTags#PUMP_PIPES} tag so other mods can add their own pipes.
 */
public final class PipeTower {

    private PipeTower() {
    }

    public static boolean isPipe(BlockState state) {
        return state.is(PumpTags.PUMP_PIPES);
    }

    /**
     * Walks up from {@code basePos} while the blocks are pipes.
     *
     * @return the highest pipe of the tower, or null when the block directly above {@code basePos} is no pipe
     */
    @Nullable
    public static BlockPos findTop(LevelReader level, BlockPos basePos) {
        BlockPos.MutableBlockPos cursor = basePos.mutable();
        int topY = Integer.MIN_VALUE;
        for (int y = basePos.getY() + 1; y < level.getMaxBuildHeight(); y++) {
            cursor.setY(y);
            if (!isPipe(level.getBlockState(cursor))) {
                break;
            }
            topY = y;
        }
        return topY == Integer.MIN_VALUE ? null : new BlockPos(basePos.getX(), topY, basePos.getZ());
    }

    /**
     * Tells the pump below a pipe (if any) that its tower changed, so it rescans on its next tick.
     * Server side only.
     */
    public static void notifyPumpBelow(Level level, BlockPos pipePos) {
        BlockPos.MutableBlockPos cursor = pipePos.mutable().move(Direction.DOWN);
        // Defence in depth: a world can never hold more pipes than its height, so the walk is capped to that.
        int remainingSteps = level.getHeight();
        while (remainingSteps-- > 0 && !level.isOutsideBuildHeight(cursor) && isPipe(level.getBlockState(cursor))) {
            cursor.move(Direction.DOWN);
        }
        if (level.getBlockEntity(cursor) instanceof PumpBlockEntity pump) {
            pump.markTowerDirty();
        }
    }
}
