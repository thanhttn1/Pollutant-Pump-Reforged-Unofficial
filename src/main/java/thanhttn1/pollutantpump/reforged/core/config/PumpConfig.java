/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Modified: ported from Kotlin/Forge 1.16.5 to Java 21/NeoForge 1.21.1 (2026-10-19).
 * Modified: branch pipe and dependency check options (2026-11-02, 1.1.0).
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import thanhttn1.pollutantpump.reforged.core.tower.BranchGraph;

/**
 * Common config ({@code config/pollutantpumpreforgedunofficial-common.toml}). Every gameplay number lives here so
 * modpack makers can tune the pump without touching code. Values are only readable after the config has loaded.
 */
public final class PumpConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue PUMP_RADIUS = BUILDER
            .comment("Horizontal radius (X/Z) around the top pipe from which pollution is sucked. The scan cost grows with the volume, so the maximum is kept low")
            .defineInRange("pumpRadius", 5, 1, 16);

    public static final ModConfigSpec.IntValue PUMP_HEIGHT_RANGE = BUILDER
            .comment("Vertical range (Y) above and below the top pipe from which pollution is sucked")
            .defineInRange("pumpHeightRange", 1, 0, 8);

    public static final ModConfigSpec.IntValue WORK_INTERVAL_TICKS = BUILDER
            .comment("Ticks between two suction attempts (20 ticks = 1 second)")
            .defineInRange("workIntervalTicks", 60, 1, 24_000);

    public static final ModConfigSpec.IntValue MAX_IDLE_INTERVAL_TICKS = BUILDER
            .comment("When an attempt finds nothing to suck, the wait before the next attempt doubles, up to this many ticks, and resets after a success. Set it equal to workIntervalTicks to disable")
            .defineInRange("maxIdleIntervalTicks", 240, 1, 72_000);

    public static final ModConfigSpec.IntValue ENERGY_PER_OPERATION = BUILDER
            .comment("Forge Energy consumed each time the pump successfully sucks one cloud of pollution")
            .defineInRange("energyPerOperation", 40, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ENERGY_CAPACITY = BUILDER
            .comment("Maximum Forge Energy the pump can store")
            .defineInRange("energyCapacity", 1_000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue KEEP_ENERGY_WHEN_BROKEN = BUILDER
            .comment("If true, the dropped pump item stores its energy in a data component and restores it when placed")
            .define("keepEnergyWhenBroken", false);

    public static final ModConfigSpec.BooleanValue RELEASE_INTO_OPEN_AIR = BUILDER
            .comment("If true, pollution that no adjacent Filter Frame can hold is released into an empty block next to the pump")
            .define("releaseIntoOpenAir", true);

    public static final ModConfigSpec.BooleanValue BRANCH_PIPES_ENABLED = BUILDER
            .comment("If true, pipes linked sideways (sneak-place a pipe against the side of another pipe) form branches whose open ends also suck pollution, at a higher energy cost. Existing towers are not affected when false")
            .define("branchPipesEnabled", true);

    public static final ModConfigSpec.IntValue MAX_BRANCH_PIPES = BUILDER
            .comment("Most branch pipes (not counting the tower) a pump looks at. Larger networks are cut off here to keep the periodic rescan cheap")
            .defineInRange("maxBranchPipes", 64, 1, 512);

    public static final ModConfigSpec.IntValue BRANCH_ENERGY_PERCENT = BUILDER
            .comment("Energy cost of a suction at the nearest branch end, as a percentage of energyPerOperation (200 = twice as much)")
            .defineInRange("branchEnergyPercent", 200, 100, 10_000);

    public static final ModConfigSpec.IntValue BRANCH_ENERGY_PERCENT_PER_PIPE = BUILDER
            .comment("Extra percentage of energyPerOperation for every further branch pipe between the tower and the branch end")
            .defineInRange("branchEnergyPercentPerPipe", 10, 0, 1_000);

    public static final ModConfigSpec.BooleanValue STRICT_DEPENDENCY_CHECK = BUILDER
            .comment("If true, the game stops during loading with a clear message when the installed Pollution of the Realms no longer has the methods this mod calls. If false the problem is only logged and the pump may crash later")
            .define("strictDependencyCheck", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private PumpConfig() {
    }

    /** @return the cost of one operation, never above what the pump can physically store */
    public static int effectiveEnergyPerOperation() {
        return Math.min(ENERGY_PER_OPERATION.getAsInt(), ENERGY_CAPACITY.getAsInt());
    }

    /** @return the cost of one suction at a branch end that is {@code pipeCount} branch pipes away from the tower */
    public static int branchEnergyPerOperation(int pipeCount) {
        return BranchGraph.branchCost(effectiveEnergyPerOperation(), ENERGY_CAPACITY.getAsInt(),
                BRANCH_ENERGY_PERCENT.getAsInt(), BRANCH_ENERGY_PERCENT_PER_PIPE.getAsInt(), pipeCount);
    }
}
