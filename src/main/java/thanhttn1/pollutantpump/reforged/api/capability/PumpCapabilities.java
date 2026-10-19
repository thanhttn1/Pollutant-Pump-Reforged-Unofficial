/*
 * Pollutant Pump Reforged Unofficial - public API
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.api.capability;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;

/**
 * Capabilities exposed by pump blocks.
 *
 * <pre>{@code
 * IPumpStatus status = level.getCapability(PumpCapabilities.PUMP_STATUS, pos);
 * if (status != null && status.isWorking()) { ... }
 * }</pre>
 */
public final class PumpCapabilities {

    /** Read-only pump state. The context is {@code Void} because the state does not depend on the side. */
    public static final BlockCapability<IPumpStatus, Void> PUMP_STATUS = BlockCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(PollutantPumpApi.MOD_ID, "pump_status"),
            IPumpStatus.class);

    private PumpCapabilities() {
    }
}
