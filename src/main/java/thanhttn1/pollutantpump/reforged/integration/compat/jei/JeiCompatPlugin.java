/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.integration.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;
import thanhttn1.pollutantpump.reforged.core.registry.ModItems;

/**
 * Adds "Info" pages for the pump and the pipe in JEI. JEI discovers this class itself, so the mod still loads
 * without JEI. The text avoids config numbers on purpose: the client config may differ from the server's.
 */
@JeiPlugin
public class JeiCompatPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(PollutantPumpApi.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(ModItems.PUMP.get(),
                Component.translatable("jei." + PollutantPumpApi.MOD_ID + ".pump.info"));
        registration.addIngredientInfo(ModItems.PIPE.get(),
                Component.translatable("jei." + PollutantPumpApi.MOD_ID + ".pipe.info"));
    }
}
