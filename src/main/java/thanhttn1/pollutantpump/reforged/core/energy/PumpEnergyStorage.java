/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Modified: ported from Kotlin/Forge 1.16.5 to Java 21/NeoForge 1.21.1 (2026-10-19).
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;
import thanhttn1.pollutantpump.reforged.core.config.PumpConfig;

/**
 * Energy buffer of a pump. Capacity is read from the config on every call so a config reload applies immediately,
 * and cables can only insert: the pump spends its own energy internally through {@link #consume(int)}.
 */
public final class PumpEnergyStorage implements IEnergyStorage {

    private final Runnable changeListener;
    private int storedEnergy;

    /** @param changeListener called after the stored amount changed, typically {@code BlockEntity::setChanged} */
    public PumpEnergyStorage(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int accepted = Math.min(Math.max(toReceive, 0), getMaxEnergyStored() - storedEnergy);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            storedEnergy += accepted;
            changeListener.run();
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        // External extraction is disabled on purpose: the pump is a consumer, not a battery.
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return storedEnergy;
    }

    @Override
    public int getMaxEnergyStored() {
        return PumpConfig.ENERGY_CAPACITY.getAsInt();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    /** @return true when at least {@code amount} was available and has been spent */
    public boolean consume(int amount) {
        if (amount > storedEnergy) {
            return false;
        }
        if (amount > 0) {
            storedEnergy -= amount;
            changeListener.run();
        }
        return true;
    }

    /** Overwrites the stored energy (loading, item components). Clamped to the capacity, does not notify. */
    public void setStored(int energy) {
        this.storedEnergy = Math.min(Math.max(energy, 0), getMaxEnergyStored());
    }
}
