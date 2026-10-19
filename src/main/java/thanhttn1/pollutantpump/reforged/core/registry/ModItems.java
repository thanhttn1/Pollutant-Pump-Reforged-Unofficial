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

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;
import thanhttn1.pollutantpump.reforged.core.item.PumpBlockItem;

public final class ModItems {

    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(PollutantPumpApi.MOD_ID);

    public static final DeferredItem<PumpBlockItem> PUMP =
            REGISTRY.registerItem("pump", properties -> new PumpBlockItem(ModBlocks.PUMP.get(), properties));

    public static final DeferredItem<BlockItem> PIPE =
            REGISTRY.registerItem("pipe", properties -> new BlockItem(ModBlocks.PIPE.get(), properties));

    private ModItems() {
    }
}
