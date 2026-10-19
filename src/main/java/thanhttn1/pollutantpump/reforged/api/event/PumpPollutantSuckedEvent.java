/*
 * Pollutant Pump Reforged Unofficial - public API
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired on {@code NeoForge.EVENT_BUS} (logical server only) whenever a pump moves a cloud of pollution.
 *
 * <p>Listen to {@link Pre} to veto or change the transfer, and to {@link Post} to react after it happened.
 * Listening to this base class receives both.
 */
public abstract class PumpPollutantSuckedEvent extends Event {

    /** Where the sucked pollution ends up. */
    public enum Destination {
        /** Absorbed by a Pollution of the Realms Filter Frame next to the pump. */
        FILTER_FRAME,
        /** Placed again as a pollutant block in an empty space next to the pump. */
        OPEN_AIR
    }

    private final ServerLevel level;
    private final BlockPos pumpPos;
    private final BlockPos pollutantPos;
    private final BlockPos destinationPos;
    private final BlockState pollutantState;
    private final Destination destination;
    private int amount;

    protected PumpPollutantSuckedEvent(ServerLevel level, BlockPos pumpPos, BlockPos pollutantPos,
                                       BlockPos destinationPos, BlockState pollutantState,
                                       Destination destination, int amount) {
        this.level = level;
        this.pumpPos = pumpPos.immutable();
        this.pollutantPos = pollutantPos.immutable();
        this.destinationPos = destinationPos.immutable();
        this.pollutantState = pollutantState;
        this.destination = destination;
        this.amount = amount;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getPumpPos() {
        return pumpPos;
    }

    /** @return where the pollutant block was found, before it is removed */
    public BlockPos getPollutantPos() {
        return pollutantPos;
    }

    /** @return position of the Filter Frame, or of the empty block receiving the pollutant */
    public BlockPos getDestinationPos() {
        return destinationPos;
    }

    /** @return state of the pollutant block, including its density */
    public BlockState getPollutantState() {
        return pollutantState;
    }

    public Destination getDestination() {
        return destination;
    }

    /**
     * @return pollution units moved. For {@link Destination#FILTER_FRAME} this is what the filter absorbs; for
     *         {@link Destination#OPEN_AIR} the block is copied unchanged and the value is informational.
     */
    public int getAmount() {
        return amount;
    }

    protected final void applyAmount(int newAmount) {
        this.amount = Math.max(0, newAmount);
    }

    /**
     * Fired before the world is changed. Cancel it to leave the pollutant where it is; the pump then tries the
     * next candidate without spending energy.
     */
    public static class Pre extends PumpPollutantSuckedEvent implements ICancellableEvent {

        public Pre(ServerLevel level, BlockPos pumpPos, BlockPos pollutantPos, BlockPos destinationPos,
                   BlockState pollutantState, Destination destination, int amount) {
            super(level, pumpPos, pollutantPos, destinationPos, pollutantState, destination, amount);
        }

        /**
         * Overrides how many pollution units reach a Filter Frame. Values below the carried amount make the
         * pump destroy the difference, 0 destroys the cloud without filling anything. The filter still caps
         * the value to its free space.
         */
        public void setAmount(int newAmount) {
            applyAmount(newAmount);
        }
    }

    /** Fired after the pollutant was removed and delivered. Not cancellable. */
    public static class Post extends PumpPollutantSuckedEvent {

        public Post(ServerLevel level, BlockPos pumpPos, BlockPos pollutantPos, BlockPos destinationPos,
                    BlockState pollutantState, Destination destination, int amount) {
            super(level, pumpPos, pollutantPos, destinationPos, pollutantState, destination, amount);
        }
    }
}
