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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PollutantPumpApi.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTRY.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + PollutantPumpApi.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.PUMP.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.PUMP.get());
                        output.accept(ModItems.PIPE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
