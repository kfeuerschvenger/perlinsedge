package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.utils.Hashes;

/**
 * Turns the tile an enemy spawned on into a random seed, so the timing of its movement is the same on every run and
 * enemies on neighbouring tiles get unrelated sequences.
 */
public final class SpawnSeed {

    private SpawnSeed() {
    }

    /**
     * Mixes the tile with {@link Hashes#mix(int, int)}, so the same tile always gives the same seed and tiles that
     * differ by one coordinate give unrelated seeds.
     *
     * @param tileX column of the spawn tile
     * @param tileY row of the spawn tile
     * @return the seed
     */
    public static long of(int tileX, int tileY) {
        return Hashes.mix(tileX, tileY);
    }
}
