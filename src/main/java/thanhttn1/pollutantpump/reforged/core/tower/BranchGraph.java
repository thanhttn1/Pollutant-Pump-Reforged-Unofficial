/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Derived from "Pollutant Pump" by bright_spark (https://github.com/thebrightspark/PollutantPump), GPLv3.
 * Added in 1.1.0 (2026-11-02): branch pipe network search.
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.tower;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure graph search over a pump's pipe network. It has no Minecraft dependency on purpose, so the same code can be
 * unit tested outside the game. {@link BranchNetwork} adapts it to a real level.
 *
 * <p>Graph rules: pipes stacked vertically are always connected. Two horizontal neighbours are connected when both are
 * pipes and at least one of them has its arm flag set towards the other. Direction indexes are 0 = north (-z),
 * 1 = east (+x), 2 = south (+z), 3 = west (-x).
 */
public final class BranchGraph {

    public static final int[] DX = {0, 1, 0, -1};
    public static final int[] DZ = {-1, 0, 1, 0};

    private BranchGraph() {
    }

    /** What the search needs to know about the world. Implementations must not load chunks. */
    public interface Probe {
        /** @return true when the block is any pipe of the pump pipe tag */
        boolean isPipe(int x, int y, int z);

        /** @return true when the block is one of our pipes and has its arm flag set towards {@code dir} */
        boolean arm(int x, int y, int z, int dir);

        /**
         * @return bit {@code 1 << dir} set for every arm flag of the block at once; implementations should answer
         *         with a single block lookup, because this runs for every tower pipe on each periodic rescan
         */
        default int armMask(int x, int y, int z) {
            int mask = 0;
            for (int dir = 0; dir < 4; dir++) {
                if (arm(x, y, z, dir)) {
                    mask |= 1 << dir;
                }
            }
            return mask;
        }
    }

    /** A suction mouth of a branch. {@code pipeCount} is the number of branch pipes between the tower and it. */
    public record Head(int x, int y, int z, int pipeCount) {
    }

    private static int opposite(int dir) {
        return (dir + 2) & 3;
    }

    private static boolean horizontalEdge(Probe probe, int x, int y, int z, int dir) {
        int nx = x + DX[dir];
        int nz = z + DZ[dir];
        return probe.isPipe(nx, y, nz) && (probe.arm(x, y, z, dir) || probe.arm(nx, y, nz, opposite(dir)));
    }

    private static boolean isTower(int x, int y, int z, int baseX, int baseY, int baseZ, int topY) {
        return x == baseX && z == baseZ && y > baseY && y <= topY;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | ((y + 2048) & 0xFFFL);
    }

    private static int degree(Probe probe, int x, int y, int z) {
        int degree = 0;
        if (probe.isPipe(x, y + 1, z)) {
            degree++;
        }
        if (probe.isPipe(x, y - 1, z)) {
            degree++;
        }
        for (int dir = 0; dir < 4; dir++) {
            if (horizontalEdge(probe, x, y, z, dir)) {
                degree++;
            }
        }
        return degree;
    }

    /**
     * Finds the open ends (leaves) of every branch that hangs off the tower standing above {@code (baseX, baseY,
     * baseZ)} up to {@code topY}. Tower pipes themselves are never returned.
     *
     * @param maxBranchPipes upper bound of branch pipes visited, so a huge or looping network stays cheap
     */
    public static List<Head> findHeads(Probe probe, int baseX, int baseY, int baseZ, int topY, int maxBranchPipes) {
        List<Head> heads = new ArrayList<>(0);
        if (topY <= baseY || maxBranchPipes <= 0) {
            return heads;
        }
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        for (int y = baseY + 1; y <= topY; y++) {
            // Plain tower pipes (no arms) cost one lookup. A link is mirrored onto both pipes by the block's
            // updateShape, so the tower pipe's own flag is enough to find every branch.
            int mask = probe.armMask(baseX, y, baseZ);
            if (mask == 0) {
                continue;
            }
            for (int dir = 0; dir < 4; dir++) {
                if ((mask & (1 << dir)) != 0 && horizontalEdge(probe, baseX, y, baseZ, dir)) {
                    enqueue(queue, visited, baseX + DX[dir], y, baseZ + DZ[dir], 1);
                }
            }
        }
        if (queue.isEmpty()) {
            return heads;
        }
        int budget = maxBranchPipes;
        while (!queue.isEmpty() && budget-- > 0) {
            int[] node = queue.poll();
            int x = node[0];
            int y = node[1];
            int z = node[2];
            int depth = node[3];
            if (degree(probe, x, y, z) <= 1) {
                heads.add(new Head(x, y, z, depth));
            }
            expand(probe, queue, visited, x, y, z, depth, baseX, baseY, baseZ, topY);
        }
        return heads;
    }

    /**
     * Collects every pipe connected to {@code (startX, startY, startZ)}, tower pipes included, up to
     * {@code maxNodes}. Used to find which pumps a changed branch belongs to.
     */
    public static List<int[]> collect(Probe probe, int startX, int startY, int startZ, int maxNodes) {
        List<int[]> result = new ArrayList<>();
        if (maxNodes <= 0 || !probe.isPipe(startX, startY, startZ)) {
            return result;
        }
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        enqueue(queue, visited, startX, startY, startZ, 0);
        while (!queue.isEmpty() && result.size() < maxNodes) {
            int[] node = queue.poll();
            result.add(new int[] {node[0], node[1], node[2]});
            expand(probe, queue, visited, node[0], node[1], node[2], node[3], 0, 0, 0, Integer.MIN_VALUE);
        }
        return result;
    }

    private static void expand(Probe probe, ArrayDeque<int[]> queue, Set<Long> visited, int x, int y, int z,
                               int depth, int baseX, int baseY, int baseZ, int topY) {
        if (probe.isPipe(x, y + 1, z) && !isTower(x, y + 1, z, baseX, baseY, baseZ, topY)) {
            enqueue(queue, visited, x, y + 1, z, depth + 1);
        }
        if (probe.isPipe(x, y - 1, z) && !isTower(x, y - 1, z, baseX, baseY, baseZ, topY)) {
            enqueue(queue, visited, x, y - 1, z, depth + 1);
        }
        for (int dir = 0; dir < 4; dir++) {
            if (horizontalEdge(probe, x, y, z, dir)
                    && !isTower(x + DX[dir], y, z + DZ[dir], baseX, baseY, baseZ, topY)) {
                enqueue(queue, visited, x + DX[dir], y, z + DZ[dir], depth + 1);
            }
        }
    }

    private static void enqueue(ArrayDeque<int[]> queue, Set<Long> visited, int x, int y, int z, int depth) {
        if (visited.add(key(x, y, z))) {
            queue.add(new int[] {x, y, z, depth});
        }
    }

    /**
     * Energy cost of one suction at a branch head, in the same unit as {@code baseCost}.
     *
     * @param percent        cost of the nearest head as a percentage of {@code baseCost}
     * @param percentPerPipe extra percentage for every further branch pipe
     * @return the cost rounded up, never above {@code capacity} so a branch stays reachable
     */
    public static int branchCost(int baseCost, int capacity, int percent, int percentPerPipe, int pipeCount) {
        if (baseCost <= 0) {
            return 0;
        }
        long total = (long) percent + (long) percentPerPipe * Math.max(pipeCount - 1, 0);
        long cost = ((long) baseCost * total + 99L) / 100L;
        return (int) Math.max(baseCost, Math.min(cost, Math.max(capacity, baseCost)));
    }
}
