/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.integration.compat.jade;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;

import static thanhttn1.pollutantpump.reforged.integration.compat.jade.PumpServerDataProvider.KEY_ENERGY_PER_OPERATION;
import static thanhttn1.pollutantpump.reforged.integration.compat.jade.PumpServerDataProvider.KEY_HORIZONTAL_RANGE;
import static thanhttn1.pollutantpump.reforged.integration.compat.jade.PumpServerDataProvider.KEY_TOWER_CONNECTED;
import static thanhttn1.pollutantpump.reforged.integration.compat.jade.PumpServerDataProvider.KEY_TOWER_TOP;
import static thanhttn1.pollutantpump.reforged.integration.compat.jade.PumpServerDataProvider.KEY_VERTICAL_RANGE;
import static thanhttn1.pollutantpump.reforged.integration.compat.jade.PumpServerDataProvider.KEY_WORKING;

/** Client side: turns the synced pump data into tooltip lines. */
public enum PumpComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final String LANG_PREFIX = PollutantPumpApi.MOD_ID + ".jade.";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(KEY_TOWER_CONNECTED)) {
            return;
        }

        tooltip.add(Component.translatable(LANG_PREFIX + stateKey(data)));
        if (data.contains(KEY_TOWER_TOP)) {
            BlockPos top = BlockPos.of(data.getLong(KEY_TOWER_TOP));
            tooltip.add(Component.translatable(LANG_PREFIX + "tower_top", top.toShortString()));
        }
        tooltip.add(Component.translatable(LANG_PREFIX + "range",
                data.getInt(KEY_HORIZONTAL_RANGE), data.getInt(KEY_VERTICAL_RANGE)));
        tooltip.add(Component.translatable(LANG_PREFIX + "cost", data.getInt(KEY_ENERGY_PER_OPERATION)));
    }

    private static String stateKey(CompoundTag data) {
        if (!data.getBoolean(KEY_TOWER_CONNECTED)) {
            return "state.no_tower";
        }
        return data.getBoolean(KEY_WORKING) ? "state.working" : "state.idle";
    }

    @Override
    public ResourceLocation getUid() {
        return PumpServerDataProvider.UID;
    }
}
