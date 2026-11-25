package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The patch value every generated tile carries, which the renderer turns into large patches of ground variants. */
class GroundPatchTest {

    private static final int SIZE = 150;
    private static final long SEED = 7L;
    private static final int BANDS = 3;
    /** Neighbours in the same band; independent per-tile choices among three bands would agree a third of the time. */
    private static final double MIN_SAME_BAND_SHARE = 0.85;
    private static final double MIN_BAND_SHARE = 0.05;

    private final MapGenerator generator = new MapGenerator(FastNoiseLiteAdapter.FACTORY);

    private static int band(Tile tile) {
        return tile.getPatch() * BANDS / (Tile.MAX_PATCH + 1);
    }

    private static double sameBandShare(TileMap map) {
        var pairs = 0;
        var same = 0;
        for (var y = 0; y < map.getHeight() - 1; y++) {
            for (var x = 0; x < map.getWidth() - 1; x++) {
                pairs += 2;
                same += band(map.getTile(x, y)) == band(map.getTile(x + 1, y)) ? 1 : 0;
                same += band(map.getTile(x, y)) == band(map.getTile(x, y + 1)) ? 1 : 0;
            }
        }
        return same / (double) pairs;
    }

    @Test
    void everyMapTypeHasPatchesNotScatter() {
        for (MapType type : MapType.values()) {
            final var map = generator.generate(new MapRequest(type, SEED, SIZE, SIZE));

            final var share = sameBandShare(map);
            assertTrue(share >= MIN_SAME_BAND_SHARE, type + " " + share);
        }
    }

    @Test
    void everyBandCoversAFairShareOfTheMap() {
        final var map = generator.generate(new MapRequest(MapType.DENSE_FOREST, SEED, SIZE, SIZE));
        final var counts = new int[BANDS];
        for (var y = 0; y < SIZE; y++) {
            for (var x = 0; x < SIZE; x++) {
                counts[band(map.getTile(x, y))]++;
            }
        }

        for (var band = 0; band < BANDS; band++) {
            assertTrue(counts[band] >= MIN_BAND_SHARE * SIZE * SIZE, "band " + band + " has " + counts[band]);
        }
    }

    @Test
    void theSameRequestGivesTheSamePatches() {
        final var first = generator.generate(new MapRequest(MapType.SWAMP, SEED, SIZE, SIZE));
        final var second = generator.generate(new MapRequest(MapType.SWAMP, SEED, SIZE, SIZE));

        for (var y = 0; y < SIZE; y++) {
            for (var x = 0; x < SIZE; x++) {
                assertEquals(first.getTile(x, y).getPatch(), second.getTile(x, y).getPatch());
            }
        }
    }
}
