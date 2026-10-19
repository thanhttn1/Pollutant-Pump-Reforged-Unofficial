/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.util;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

/**
 * Visits every position of a box exactly once in a pseudo-random order without allocating a position list.
 *
 * <p>The trick: stepping through {@code n} cells with a stride that is coprime to {@code n} hits each cell once
 * before repeating, so a random start plus a random coprime stride gives a cheap shuffle.
 */
public final class ShuffledBoxScan {

    private ShuffledBoxScan() {
    }

    /**
     * @param visitor receives a reused mutable position, copy it with {@code immutable()} before storing it
     * @return true as soon as the visitor returned true, false when the whole box was visited
     */
    public static boolean anyMatch(RandomSource random, BlockPos center, int horizontalRadius, int verticalRange,
                                   Predicate<BlockPos.MutableBlockPos> visitor) {
        int sizeXZ = horizontalRadius * 2 + 1;
        int sizeY = verticalRange * 2 + 1;
        int cellCount = sizeXZ * sizeXZ * sizeY;
        int stride = pickCoprimeStride(random, cellCount);
        long cellIndex = random.nextInt(cellCount);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int visited = 0; visited < cellCount; visited++) {
            int linear = (int) (cellIndex % cellCount);
            int offsetX = linear % sizeXZ;
            int offsetZ = (linear / sizeXZ) % sizeXZ;
            int offsetY = linear / (sizeXZ * sizeXZ);
            cursor.set(
                    center.getX() - horizontalRadius + offsetX,
                    center.getY() - verticalRange + offsetY,
                    center.getZ() - horizontalRadius + offsetZ);
            if (visitor.test(cursor)) {
                return true;
            }
            cellIndex += stride;
        }
        return false;
    }

    private static int pickCoprimeStride(RandomSource random, int cellCount) {
        if (cellCount <= 2) {
            return 1;
        }
        int candidate = 1 + random.nextInt(cellCount - 1);
        // Coprime numbers are dense (the gap to the next one is tiny), so this ends after a few steps. Wrapping to 1,
        // which is coprime to everything, guarantees it terminates.
        while (greatestCommonDivisor(candidate, cellCount) != 1) {
            candidate = candidate == cellCount - 1 ? 1 : candidate + 1;
        }
        return candidate;
    }

    private static int greatestCommonDivisor(int a, int b) {
        while (b != 0) {
            int remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }
}
