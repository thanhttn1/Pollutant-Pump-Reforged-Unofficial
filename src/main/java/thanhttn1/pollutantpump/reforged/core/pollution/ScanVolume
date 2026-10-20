/*
 * Pollutant Pump Reforged Unofficial
 * Copyright (C) 2026 thanhtnt2
 * SPDX-License-Identifier: GPL-3.0-only
 */
package thanhttn1.pollutantpump.reforged.core.pollution;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * The chunks covering one scan box, looked up once per attempt instead of once per block.
 *
 * <p>It also answers the cheap question "can this box contain pollution at all?" by asking each chunk section's
 * palette, which lets a pump over clean air skip the block-by-block scan completely.
 */
final class ScanVolume {

    private final LevelChunk[] chunks;
    private final int minChunkX;
    private final int minChunkZ;
    private final int chunkCountX;
    private final int minY;
    private final int maxY;

    private ScanVolume(LevelChunk[] chunks, int minChunkX, int minChunkZ, int chunkCountX, int minY, int maxY) {
        this.chunks = chunks;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.chunkCountX = chunkCountX;
        this.minY = minY;
        this.maxY = maxY;
    }

    /** Chunks that are not loaded are left null: the scan never forces a chunk to load. */
    static ScanVolume resolve(ServerLevel level, BlockPos center, int horizontalRadius, int verticalRange) {
        int minChunkX = SectionPos.blockToSectionCoord(center.getX() - horizontalRadius);
        int maxChunkX = SectionPos.blockToSectionCoord(center.getX() + horizontalRadius);
        int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - horizontalRadius);
        int maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + horizontalRadius);
        int chunkCountX = maxChunkX - minChunkX + 1;
        int chunkCountZ = maxChunkZ - minChunkZ + 1;

        ServerChunkCache chunkSource = level.getChunkSource();
        LevelChunk[] chunks = new LevelChunk[chunkCountX * chunkCountZ];
        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                chunks[(chunkX - minChunkX) + (chunkZ - minChunkZ) * chunkCountX] = chunkSource.getChunkNow(chunkX, chunkZ);
            }
        }

        int minY = Math.max(center.getY() - verticalRange, level.getMinBuildHeight());
        int maxY = Math.min(center.getY() + verticalRange, level.getMaxBuildHeight() - 1);
        return new ScanVolume(chunks, minChunkX, minChunkZ, chunkCountX, minY, maxY);
    }

    /** @return false only when it is certain that no block matching the test exists inside the box's sections */
    boolean anySectionMayContain(Predicate<BlockState> test) {
        if (minY > maxY) {
            return false;
        }
        for (LevelChunk chunk : chunks) {
            if (chunk != null && chunkMayContain(chunk, test)) {
                return true;
            }
        }
        return false;
    }

    private boolean chunkMayContain(LevelChunk chunk, Predicate<BlockState> test) {
        LevelChunkSection[] sections = chunk.getSections();
        int firstSection = chunk.getSectionIndex(minY);
        int lastSection = chunk.getSectionIndex(maxY);
        for (int index = firstSection; index <= lastSection; index++) {
            LevelChunkSection section = sections[index];
            if (!section.hasOnlyAir() && section.maybeHas(test)) {
                return true;
            }
        }
        return false;
    }

    /** @return the block state, or null when the position lies in a chunk that is not loaded */
    @Nullable
    BlockState stateAt(BlockPos pos) {
        int chunkIndexX = SectionPos.blockToSectionCoord(pos.getX()) - minChunkX;
        int chunkIndexZ = SectionPos.blockToSectionCoord(pos.getZ()) - minChunkZ;
        LevelChunk chunk = chunks[chunkIndexX + chunkIndexZ * chunkCountX];
        return chunk == null ? null : chunk.getBlockState(pos);
    }
}
