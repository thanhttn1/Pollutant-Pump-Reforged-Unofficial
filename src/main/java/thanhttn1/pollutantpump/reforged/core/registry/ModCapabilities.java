/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import thanhttn1.pollutantpump.reforged.api.capability.PumpCapabilities;

/** Attaches capabilities to the pump block entity. Listens on the MOD event bus. */
public final class ModCapabilities {

    private ModCapabilities() {
    }

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.PUMP.get(),
                (pump, side) -> pump.getEnergyStorage());
        event.registerBlockEntity(PumpCapabilities.PUMP_STATUS, ModBlockEntities.PUMP.get(),
                (pump, ignoredContext) -> pump);
    }
}
