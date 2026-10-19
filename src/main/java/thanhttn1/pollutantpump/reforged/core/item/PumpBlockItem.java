/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import thanhttn1.pollutantpump.reforged.api.PollutantPumpApi;
import thanhttn1.pollutantpump.reforged.core.registry.ModDataComponents;

/** Pump item that shows the energy remembered in its {@code stored_energy} data component. */
public class PumpBlockItem extends BlockItem {

    public PumpBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Integer storedEnergy = stack.get(ModDataComponents.STORED_ENERGY.get());
        if (storedEnergy == null) {
            return;
        }
        tooltip.add(Component.translatable("tooltip." + PollutantPumpApi.MOD_ID + ".stored_energy", storedEnergy)
                .withStyle(ChatFormatting.GRAY));
    }
}
