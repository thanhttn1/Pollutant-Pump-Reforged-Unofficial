/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;

/**
 * Item data components. Minecraft 1.21.1 has no item NBT anymore, so data carried by an item stack lives here.
 */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> REGISTRY =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, PollutantPumpApi.MOD_ID);

    /** Energy (FE) a pump held when it was broken. Only written when the keepEnergyWhenBroken config is on. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_ENERGY =
            REGISTRY.register("stored_energy", () -> DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private ModDataComponents() {
    }
}
