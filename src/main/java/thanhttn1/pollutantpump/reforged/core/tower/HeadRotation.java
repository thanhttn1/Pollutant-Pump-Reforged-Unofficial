/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 *
 * Added in 1.1.0 (2026-11-02): round robin over the tower top and the branch heads.
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.tower;

/**
 * Decides which suction mouth (slot 0 = tower top, slot 1.. = branch heads) the next operation uses, and when the
 * pump may back off. The pump only backs off after every slot failed once in a row, so one dead branch cannot slow
 * down the others. With a single slot it behaves exactly like the pump did before branches existed.
 */
public final class HeadRotation {

    private int next;
    private int failedInRow;

    /** @return the slot for the next operation, in {@code [0, slots)} */
    public int pick(int slots) {
        if (slots <= 1) {
            return 0;
        }
        int slot = Math.floorMod(next, slots);
        next = slot + 1;
        return slot;
    }

    public void onSuccess() {
        failedInRow = 0;
    }

    /** @return true when all {@code slots} failed in a row and the idle wait should grow */
    public boolean onFailure(int slots) {
        if (++failedInRow >= Math.max(slots, 1)) {
            failedInRow = 0;
            return true;
        }
        return false;
    }

    public void reset() {
        failedInRow = 0;
    }
}
