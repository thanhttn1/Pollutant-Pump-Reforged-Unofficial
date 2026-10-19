/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.integration.compat.jade;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;
import thanhttn1.pollutantpump.reforged.api.capability.IPumpStatus;
import thanhttn1.pollutantpump.reforged.api.capability.PumpCapabilities;

/** Runs on the server and copies the pump state into Jade's sync tag (Jade's wire format is a CompoundTag). */
public enum PumpServerDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(PollutantPumpApi.MOD_ID, "pump_status");

    static final String KEY_TOWER_CONNECTED = "TowerConnected";
    static final String KEY_WORKING = "Working";
    static final String KEY_TOWER_TOP = "TowerTop";
    static final String KEY_HORIZONTAL_RANGE = "HorizontalRange";
    static final String KEY_VERTICAL_RANGE = "VerticalRange";
    static final String KEY_ENERGY_PER_OPERATION = "EnergyPerOperation";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        // Read through the capability so addon pumps exposing IPumpStatus are supported too.
        IPumpStatus status = accessor.getLevel().getCapability(PumpCapabilities.PUMP_STATUS, accessor.getPosition());
        if (status == null) {
            return;
        }
        data.putBoolean(KEY_TOWER_CONNECTED, status.isTowerConnected());
        data.putBoolean(KEY_WORKING, status.isWorking());
        status.getTowerTop().map(BlockPos::asLong).ifPresent(packed -> data.putLong(KEY_TOWER_TOP, packed));
        data.putInt(KEY_HORIZONTAL_RANGE, status.getHorizontalRange());
        data.putInt(KEY_VERTICAL_RANGE, status.getVerticalRange());
        data.putInt(KEY_ENERGY_PER_OPERATION, status.getEnergyPerOperation());
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
