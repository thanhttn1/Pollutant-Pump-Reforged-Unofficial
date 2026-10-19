/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Modified: ported from Kotlin/Forge 1.16.5 to Java 21/NeoForge 1.21.1 (2026-10-19).
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.registry;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;
import thanhttn1.pollutantpump.reforged.core.block.PipeBlock;
import thanhttn1.pollutantpump.reforged.core.block.PumpBlock;

public final class ModBlocks {

    public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(PollutantPumpApi.MOD_ID);

    public static final DeferredBlock<PumpBlock> PUMP =
            REGISTRY.registerBlock("pump", PumpBlock::new, machineProperties());

    // The grate texture has holes, so the pipe must not hide the faces of its neighbours.
    public static final DeferredBlock<PipeBlock> PIPE =
            REGISTRY.registerBlock("pipe", PipeBlock::new, machineProperties().noOcclusion());

    private ModBlocks() {
    }

    /** Same toughness as the original stone-material blocks: strength 2 / blast resistance 15. */
    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
                .strength(2.0F, 15.0F);
    }
}
