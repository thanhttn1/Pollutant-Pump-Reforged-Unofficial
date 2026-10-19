/*
 * Pollutant Pump Reforged Unofficial - public API
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.api.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;

/**
 * Block tags that let addons plug into the pump without touching its code.
 */
public final class PumpTags {

    /**
     * Blocks that count as a segment of a pump's pipe tower. Add your own pipe block through a JSON tag
     * ({@code data/pollutantpumpreforgedunofficial/tags/block/pump_pipes.json}) to let pumps reach higher with it.
     */
    public static final TagKey<Block> PUMP_PIPES = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(PollutantPumpApi.MOD_ID, "pump_pipes"));

    private PumpTags() {
    }
}
