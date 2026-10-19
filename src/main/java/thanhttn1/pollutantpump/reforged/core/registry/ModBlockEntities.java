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

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;
import thanhttn1.pollutantpump.reforged.core.blockentity.PumpBlockEntity;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> REGISTRY =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PollutantPumpApi.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpBlockEntity>> PUMP =
            REGISTRY.register("pump", () -> BlockEntityType.Builder
                    .of(PumpBlockEntity::new, ModBlocks.PUMP.get())
                    .build(null));

    private ModBlockEntities() {
    }
}
