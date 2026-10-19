/*
 * Pollutant Pump Reforged Unofficial - public API
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.api.capability;

import java.util.Optional;

import net.minecraft.core.BlockPos;

/**
 * Read-only view of a pump. Obtain it through {@link PumpCapabilities#PUMP_STATUS} instead of checking the
 * concrete block entity class, so addon pumps can expose the same data.
 */
public interface IPumpStatus {

    /** @return true when a pipe tower stands directly on top of the pump */
    boolean isTowerConnected();

    /** @return position of the highest pipe, which is the centre of the suction area */
    Optional<BlockPos> getTowerTop();

    /** @return true when the pump has enough energy for one operation and a tower to suck from */
    boolean isWorking();

    int getStoredEnergy();

    int getEnergyCapacity();

    /** @return energy consumed each time one cloud of pollution is sucked */
    int getEnergyPerOperation();

    /** @return horizontal radius (X/Z) of the suction area around the tower top, in blocks */
    int getHorizontalRange();

    /** @return vertical range (Y) of the suction area above and below the tower top, in blocks */
    int getVerticalRange();
}
