/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.integration.compat.jade;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import thanhttn1.pollutantpump.reforged.core.block.PumpBlock;
import thanhttn1.pollutantpump.reforged.core.blockentity.PumpBlockEntity;

/**
 * Jade tooltip for the pump. Jade finds this class by itself, so nothing in core references it and the mod
 * still loads without Jade. Energy is drawn by Jade's built-in Forge Energy support, so only the pump-specific
 * lines are added here. Jade Addons is NOT required.
 */
@WailaPlugin
public class JadeCompatPlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PumpServerDataProvider.INSTANCE, PumpBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PumpComponentProvider.INSTANCE, PumpBlock.class);
    }
}
