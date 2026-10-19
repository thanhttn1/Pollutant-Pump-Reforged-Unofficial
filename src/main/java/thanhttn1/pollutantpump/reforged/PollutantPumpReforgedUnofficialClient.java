/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;

/** Client-only entry point: never loaded on a dedicated server, so client classes are safe here. */
@Mod(value = PollutantPumpApi.MOD_ID, dist = Dist.CLIENT)
public final class PollutantPumpReforgedUnofficialClient {

    public PollutantPumpReforgedUnofficialClient(ModContainer container) {
        // Adds the "Config" button in the Mods screen, titles come from the lang files.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
