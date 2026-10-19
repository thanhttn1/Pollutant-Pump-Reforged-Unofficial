/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.integration.compat;

import org.slf4j.Logger;

import net.neoforged.fml.ModList;

/**
 * Registry of optional mods we integrate with. To support a new mod: add a constant here, then put its code in a
 * sub-package of {@code integration.compat} (never reference its classes from {@code core}).
 *
 * <ul>
 *   <li>{@link #JADE}: tooltip plugin, discovered by Jade itself through {@code @WailaPlugin}</li>
 *   <li>{@link #JEI}: info pages, discovered by JEI itself through {@code @JeiPlugin}</li>
 *   <li>{@link #ADVANCED_CHIMNEYS}: data only, its pipe is an optional entry of the {@code pump_pipes} block tag</li>
 * </ul>
 */
public enum CompatModule {
    JADE("jade"),
    JEI("jei"),
    ADVANCED_CHIMNEYS("adchimneys");

    private final String modId;

    CompatModule(String modId) {
        this.modId = modId;
    }

    public String getModId() {
        return modId;
    }

    public boolean isLoaded() {
        return ModList.get().isLoaded(modId);
    }

    public static void logActiveModules(Logger logger) {
        for (CompatModule module : values()) {
            if (module.isLoaded()) {
                logger.info("Compat module active: {}", module.modId);
            }
        }
    }
}
