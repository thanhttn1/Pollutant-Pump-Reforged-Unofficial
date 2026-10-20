/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Modified: ported from Kotlin/Forge 1.16.5 to Java 21/NeoForge 1.21.1 (2026-10-19).
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private PumpConfig() {
    }

    /** @return the cost of one operation, never above what the pump can physically store */
    public static int effectiveEnergyPerOperation() {
        return Math.min(ENERGY_PER_OPERATION.getAsInt(), ENERGY_CAPACITY.getAsInt());
    }
}
