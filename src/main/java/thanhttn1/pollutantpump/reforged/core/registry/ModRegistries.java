/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.registry;

import net.neoforged.bus.api.IEventBus;

/** Single place that binds every DeferredRegister to the MOD event bus, so the main class stays tiny. */
public final class ModRegistries {

    private ModRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        ModBlocks.REGISTRY.register(modEventBus);
        ModItems.REGISTRY.register(modEventBus);
        ModBlockEntities.REGISTRY.register(modEventBus);
        ModDataComponents.REGISTRY.register(modEventBus);
        ModCreativeTabs.REGISTRY.register(modEventBus);
        modEventBus.addListener(ModCapabilities::onRegisterCapabilities);
    }
}
