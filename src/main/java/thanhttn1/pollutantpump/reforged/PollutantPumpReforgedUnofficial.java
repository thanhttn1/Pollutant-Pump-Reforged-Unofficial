/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Modified: ported from Kotlin/Forge 1.16.5 to Java 21/NeoForge 1.21.1 (2026-10-19).
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;
import thanhttn1.pollutantpump.reforged.core.config.PumpConfig;
import thanhttn1.pollutantpump.reforged.core.pollution.PollutantSuction;
import thanhttn1.pollutantpump.reforged.core.registry.ModRegistries;
import thanhttn1.pollutantpump.reforged.integration.compat.CompatModule;

/** Mod entry point. Wiring only: all behaviour lives in the {@code core} and {@code integration} packages. */
@Mod(PollutantPumpReforgedUnofficial.MODID)
public final class PollutantPumpReforgedUnofficial {

    public static final String MODID = PollutantPumpApi.MOD_ID;
    public static final Logger LOGGER = LogUtils.getLogger();

    public PollutantPumpReforgedUnofficial(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistries.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, PumpConfig.SPEC);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        CompatModule.logActiveModules(LOGGER);
        // Runs on the main thread after all mods are constructed, still inside the loading screen.
        event.enqueueWork(() -> LOGGER.debug("Preloaded {} pump classes", PollutantSuction.preload()));
    }
}
