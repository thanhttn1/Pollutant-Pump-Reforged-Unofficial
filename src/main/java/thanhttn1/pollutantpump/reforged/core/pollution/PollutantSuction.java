/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Modified: ported from Kotlin/Forge 1.16.5 to Java 21/NeoForge 1.21.1 (2026-10-19).
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.pollution;

import com.endertech.minecraft.mods.adpother.blocks.AbstractGas;
import com.endertech.minecraft.mods.adpother.blocks.FilterFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import thanhttn1.pollutantpump.reforged.api.event.PumpPollutantSuckedEvent;
import thanhttn1.pollutantpump.reforged.api.event.PumpPollutantSuckedEvent.Destination;
import thanhttn1.pollutantpump.reforged.core.config.PumpConfig;
import thanhttn1.pollutantpump.reforged.core.util.ShuffledBoxScan;

/**
 * The only class that talks to Pollution of the Realms. If that mod changes its API, this is the single file to fix.
 *
 * <p>One call picks at most one cloud of air pollution around the tower top and moves it into a Filter Frame next to
 * the pump, or back into the open air beside the pump when no filter has room. Most attempts end before any block is
 * read: no possible destination, or no chunk section in the box that can hold pollution.
 */
public final class PollutantSuction {

    /**
     * An attempt gives up after this many clouds that could not be delivered or were vetoed by an addon, so a
     * blocked pump or a vetoing addon cannot make it grind through every cloud in the box.
     */
    private static final int MAX_FAILED_CANDIDATES = 8;

    private static final Direction[] HORIZONTAL_SIDES = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private PollutantSuction() {
    }

    /** Everything known about one candidate cloud, bundled so helper methods stay short. */
    private record Transfer(ServerLevel level, BlockPos pumpPos, BlockPos sourcePos, BlockState state,
                            AbstractGas pollutant, int amount) {
    }

    /**
     * @param towerTop centre of the suction area
     * @return true when a cloud was moved, so the caller can charge the energy cost
     */
    public static boolean suckOnePollutant(ServerLevel level, BlockPos pumpPos, BlockPos towerTop) {
        // Cheapest checks first: without a destination or without pollution nothing can happen.
        if (!hasPossibleDestination(level, pumpPos)) {
            return false;
        }
        int radius = PumpConfig.PUMP_RADIUS.getAsInt();
        int heightRange = PumpConfig.PUMP_HEIGHT_RANGE.getAsInt();
        ScanVolume volume = ScanVolume.resolve(level, towerTop, radius, heightRange);
        if (!volume.anySectionMayContain(PollutantSuction::isPollutant)) {
            return false;
        }

        Attempt attempt = new Attempt(level, pumpPos, volume);
        ShuffledBoxScan.anyMatch(level.getRandom(), towerTop, radius, heightRange, attempt::visit);
        return attempt.succeeded;
    }

    /**
     * Loads (without initialising) the classes the first suction would otherwise load in the middle of a server tick.
     * Called during mod loading, it moves that one-time cost to the loading screen. It also fails fast with a clear
     * error if Pollution of the Realms is incompatible, instead of crashing later inside a block entity tick.
     *
     * @return how many classes were touched, for the debug log
     */
    public static int preload() {
        Class<?>[] hotClasses = {
                AbstractGas.class, FilterFrame.class, FilterFrame.BlockTile.class,
                ScanVolume.class, Attempt.class, Transfer.class,
                PumpPollutantSuckedEvent.Pre.class, PumpPollutantSuckedEvent.Post.class
        };
        return hotClasses.length;
    }

    private static boolean isPollutant(BlockState state) {
        return state.getBlock() instanceof AbstractGas;
    }

    /** True when at least one side of the pump is a Filter Frame, or free for a release. */
    private static boolean hasPossibleDestination(ServerLevel level, BlockPos pumpPos) {
        boolean releaseAllowed = PumpConfig.RELEASE_INTO_OPEN_AIR.getAsBoolean();
        BlockPos.MutableBlockPos sidePos = new BlockPos.MutableBlockPos();
        for (Direction side : HORIZONTAL_SIDES) {
            sidePos.setWithOffset(pumpPos, side);
            if (level.getBlockState(sidePos).getBlock() instanceof FilterFrame) {
                return true;
            }
            if (releaseAllowed && level.isEmptyBlock(sidePos)) {
                return true;
            }
        }
        return false;
    }

    /** State of one scan: walks the shuffled cells until a cloud is delivered or too many candidates failed. */
    private static final class Attempt {
        private final ServerLevel level;
        private final BlockPos pumpPos;
        private final ScanVolume volume;
        private int failedCandidates;
        private boolean succeeded;

        private Attempt(ServerLevel level, BlockPos pumpPos, ScanVolume volume) {
            this.level = level;
            this.pumpPos = pumpPos;
            this.volume = volume;
        }

        /** @return true to stop the scan, either because a cloud was delivered or because we give up */
        private boolean visit(BlockPos.MutableBlockPos cursor) {
            BlockState state = volume.stateAt(cursor);
            if (state == null || !(state.getBlock() instanceof AbstractGas gas)) {
                return false;
            }
            int amount = gas.getCarriedPollutionAmount(state);
            if (amount <= 0) {
                return false;
            }

            Transfer transfer = new Transfer(level, pumpPos, cursor.immutable(), state, gas, amount);
            succeeded = deliverToFilter(transfer) || releaseIntoOpenAir(transfer);
            if (!succeeded) {
                failedCandidates++;
            }
            return succeeded || failedCandidates >= MAX_FAILED_CANDIDATES;
        }
    }

    // ---- destination 1: Filter Frame ----

    private static boolean deliverToFilter(Transfer transfer) {
        RandomSource random = transfer.level().getRandom();
        int firstSide = random.nextInt(HORIZONTAL_SIDES.length);
        for (int step = 0; step < HORIZONTAL_SIDES.length; step++) {
            Direction side = HORIZONTAL_SIDES[(firstSide + step) % HORIZONTAL_SIDES.length];
            if (tryFillFilterAt(transfer, transfer.pumpPos().relative(side))) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryFillFilterAt(Transfer transfer, BlockPos filterPos) {
        ServerLevel level = transfer.level();
        if (!(level.getBlockState(filterPos).getBlock() instanceof FilterFrame filterFrame)) {
            return false;
        }
        if (!(level.getBlockEntity(filterPos) instanceof FilterFrame.BlockTile filterTile)) {
            return false;
        }
        int freeSpace = filterFrame.getContent(filterTile).getFreeSpaceFor(transfer.pollutant());
        if (freeSpace < transfer.amount()) {
            return false;
        }

        PumpPollutantSuckedEvent.Pre event = postPre(transfer, filterPos, Destination.FILTER_FRAME);
        if (event.isCanceled()) {
            return false;
        }

        // An addon may have changed the amount, but the filter can never take more than it has room for.
        int absorbed = Math.min(event.getAmount(), freeSpace);
        if (absorbed > 0) {
            filterFrame.fill(filterTile, transfer.pollutant(), absorbed);
        }
        completeTransfer(transfer, filterPos, Destination.FILTER_FRAME, absorbed);
        return true;
    }

    // ---- destination 2: open air next to the pump ----

    private static boolean releaseIntoOpenAir(Transfer transfer) {
        if (!PumpConfig.RELEASE_INTO_OPEN_AIR.getAsBoolean()) {
            return false;
        }
        // A cloud already standing at one of the outlets would only be shuffled to another outlet: no cleaning,
        // but energy spent. This also stops a short tower from re-sucking what it released last time.
        if (isBesidePump(transfer.pumpPos(), transfer.sourcePos())) {
            return false;
        }
        ServerLevel level = transfer.level();
        int firstSide = level.getRandom().nextInt(HORIZONTAL_SIDES.length);
        for (int step = 0; step < HORIZONTAL_SIDES.length; step++) {
            Direction side = HORIZONTAL_SIDES[(firstSide + step) % HORIZONTAL_SIDES.length];
            BlockPos outputPos = transfer.pumpPos().relative(side);
            if (!level.isEmptyBlock(outputPos)) {
                continue;
            }
            PumpPollutantSuckedEvent.Pre event = postPre(transfer, outputPos, Destination.OPEN_AIR);
            if (event.isCanceled()) {
                continue;
            }
            // Flag 2 = notify clients only, like the original mod: the cloud logic reschedules itself on placement.
            level.setBlock(outputPos, transfer.state(), Block.UPDATE_CLIENTS);
            completeTransfer(transfer, outputPos, Destination.OPEN_AIR, transfer.amount());
            return true;
        }
        return false;
    }

    private static boolean isBesidePump(BlockPos pumpPos, BlockPos sourcePos) {
        return sourcePos.getY() == pumpPos.getY() && pumpPos.distManhattan(sourcePos) == 1;
    }

    // ---- shared helpers ----

    private static PumpPollutantSuckedEvent.Pre postPre(Transfer transfer, BlockPos destinationPos, Destination destination) {
        return NeoForge.EVENT_BUS.post(new PumpPollutantSuckedEvent.Pre(
                transfer.level(), transfer.pumpPos(), transfer.sourcePos(), destinationPos,
                transfer.state(), destination, transfer.amount()));
    }

    private static void completeTransfer(Transfer transfer, BlockPos destinationPos, Destination destination, int amount) {
        // The original cloud disappears only after the pollution has a new home.
        transfer.level().setBlock(transfer.sourcePos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        NeoForge.EVENT_BUS.post(new PumpPollutantSuckedEvent.Post(
                transfer.level(), transfer.pumpPos(), transfer.sourcePos(), destinationPos,
                transfer.state(), destination, amount));
    }
}
