/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Modified: ported from Kotlin/Forge 1.16.5 to Java 21/NeoForge 1.21.1 (2026-10-19).
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.blockentity;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import thanhttn1.pollutantpump.reforged.api.capability.IPumpStatus;
import thanhttn1.pollutantpump.reforged.core.block.PumpBlock;
import thanhttn1.pollutantpump.reforged.core.config.PumpConfig;
import thanhttn1.pollutantpump.reforged.core.energy.PumpEnergyStorage;
import thanhttn1.pollutantpump.reforged.core.pollution.PollutantSuction;
import thanhttn1.pollutantpump.reforged.core.registry.ModBlockEntities;
import thanhttn1.pollutantpump.reforged.core.registry.ModDataComponents;
import thanhttn1.pollutantpump.reforged.core.tower.PipeTower;

/**
 * Server-side brain of the pump: keeps the energy buffer, tracks the pipe tower and triggers one suction every
 * {@code workIntervalTicks} while it has power.
 */
public class PumpBlockEntity extends BlockEntity implements IPumpStatus {

    private static final String ENERGY_TAG_KEY = "Energy";

    /** Safety net: rescan the tower now and then in case a pipe changed without notifying us (e.g. /setblock). */
    private static final int TOWER_RECHECK_INTERVAL_TICKS = 100;

    private final PumpEnergyStorage energyStorage = new PumpEnergyStorage(this::setChanged);

    // Spreads periodic rescans of different pumps over different ticks instead of all on the same one.
    private final int towerRecheckOffsetTicks;

    @Nullable
    private BlockPos towerTop;
    private boolean towerDirty = true;
    private long nextWorkGameTime;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMP.get(), pos, state);
        this.towerRecheckOffsetTicks = Math.floorMod(pos.hashCode(), TOWER_RECHECK_INTERVAL_TICKS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PumpBlockEntity pump) {
        if (level instanceof ServerLevel serverLevel) {
            pump.tickServer(serverLevel);
        }
    }

    private void tickServer(ServerLevel level) {
        long gameTime = level.getGameTime();
        refreshTowerIfNeeded(level, gameTime);

        BlockPos top = towerTop;
        boolean working = top != null && hasEnergyForOperation();
        updatePoweredState(level, working);
        if (!working || gameTime < nextWorkGameTime) {
            return;
        }

        nextWorkGameTime = gameTime + PumpConfig.WORK_INTERVAL_TICKS.getAsInt();
        // Energy is only spent when something was actually sucked, idle pumps do not drain power.
        if (PollutantSuction.suckOnePollutant(level, worldPosition, top)) {
            energyStorage.consume(PumpConfig.effectiveEnergyPerOperation());
        }
    }

    private void refreshTowerIfNeeded(ServerLevel level, long gameTime) {
        boolean periodicRecheck = (gameTime + towerRecheckOffsetTicks) % TOWER_RECHECK_INTERVAL_TICKS == 0;
        if (!towerDirty && !periodicRecheck) {
            return;
        }
        towerTop = PipeTower.findTop(level, worldPosition);
        towerDirty = false;
    }

    private void updatePoweredState(ServerLevel level, boolean working) {
        BlockState state = getBlockState();
        if (state.getValue(PumpBlock.POWERED) == working) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(PumpBlock.POWERED, working), Block.UPDATE_CLIENTS);
    }

    private boolean hasEnergyForOperation() {
        return energyStorage.getEnergyStored() >= PumpConfig.effectiveEnergyPerOperation();
    }

    /** Called by pipes when the tower on top of this pump was extended or broken. */
    public void markTowerDirty() {
        this.towerDirty = true;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // ---- persistence: block entity data stays in a CompoundTag, item data uses data components ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(ENERGY_TAG_KEY, energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.setStored(tag.getInt(ENERGY_TAG_KEY));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        int storedEnergy = energyStorage.getEnergyStored();
        if (PumpConfig.KEEP_ENERGY_WHEN_BROKEN.getAsBoolean() && storedEnergy > 0) {
            components.set(ModDataComponents.STORED_ENERGY.get(), storedEnergy);
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        Integer storedEnergy = componentInput.get(ModDataComponents.STORED_ENERGY.get());
        if (storedEnergy != null) {
            energyStorage.setStored(storedEnergy);
        }
    }

    // ---- IPumpStatus ----

    @Override
    public boolean isTowerConnected() {
        return towerTop != null;
    }

    @Override
    public Optional<BlockPos> getTowerTop() {
        return Optional.ofNullable(towerTop);
    }

    @Override
    public boolean isWorking() {
        return towerTop != null && hasEnergyForOperation();
    }

    @Override
    public int getStoredEnergy() {
        return energyStorage.getEnergyStored();
    }

    @Override
    public int getEnergyCapacity() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    public int getEnergyPerOperation() {
        return PumpConfig.effectiveEnergyPerOperation();
    }

    @Override
    public int getHorizontalRange() {
        return PumpConfig.PUMP_RADIUS.getAsInt();
    }

    @Override
    public int getVerticalRange() {
        return PumpConfig.PUMP_HEIGHT_RANGE.getAsInt();
    }
}
