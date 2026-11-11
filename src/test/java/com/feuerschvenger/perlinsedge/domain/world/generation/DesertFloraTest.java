package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The desert grows scattered cacti and palms, with the palms gathered around the water and a few oases dug into open sand.
 * The map is generated on a fixed seed, so the counts are repeatable.
 */
class DesertFloraTest {

    private static final int SIZE = 150;
    private static final long SEED = 20_261_111L;
    private static final int REACH = 2;

    private final MapType original = AppConfig.getInstance().world().getCurrentMapType();
    private TileMap map;

    @BeforeEach
    void generateDesert() {
        map = generate(SEED);
    }

    @AfterEach
    void restoreMapType() {
        AppConfig.getInstance().world().setCurrentMapType(original);
    }

    private static TileMap generate(long seed) {
        AppConfig.WorldConfig world = AppConfig.getInstance().world();
        world.setCurrentMapType(MapType.DESERT);
        NoiseSettings profile = NoiseSettings.heightProfile(MapType.DESERT);
        MapGenerationContext context = new MapGenerationContext(MapType.DESERT, seed, profile.frequency(), profile.octaves(),
                world.getTemperatureNoiseFrequency(), world.getTemperatureNoiseOctaves(),
                world.getMoistureNoiseFrequency(), world.getMoistureNoiseOctaves(),
                world.getStonePatchFrequency(), world.getStoneThreshold(),
                world.getCrystalsPatchFrequency(), world.getCrystalsThreshold(),
                world.getTreeDensity(), world.getRiverDensity());
        return new PerlinMapGenerator(FastNoiseLiteAdapter.FACTORY).generateMap(SIZE, SIZE, context);
    }

    private boolean waterWithin(int x, int y) {
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dy = -REACH; dy <= REACH; dy++) {
                Tile tile = map.getTile(x + dx, y + dy);
                if (tile != null && tile.getType().isWater()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isTree(Tile tile) {
        return tile.hasResource() && tile.getWorldResource().getType() == ResourceType.TREE;
    }

    @Test
    void theDesertHasTreesAndTheyOnlyStandOnSand() {
        int trees = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                Tile tile = map.getTile(x, y);
                if (isTree(tile)) {
                    trees++;
                    assertEquals(TileType.SAND, tile.getType(), x + "," + y);
                }
            }
        }
        assertTrue(trees >= 500, "trees " + trees);
    }

    @Test
    void cactiAreScatteredThinlyOverTheDrySand() {
        int drySand = 0;
        int dryTrees = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                Tile tile = map.getTile(x, y);
                if (tile.getType() == TileType.SAND && !waterWithin(x, y)) {
                    drySand++;
                    if (isTree(tile)) {
                        dryTrees++;
                    }
                }
            }
        }
        double share = dryTrees / (double) drySand;
        assertTrue(dryTrees >= 50, "dry trees " + dryTrees);
        assertTrue(share > 0.005 && share < 0.06, "dry share " + share);
    }

    @Test
    void treesGatherAroundTheWaterAtLeastFiveTimesThickerThanInTheDrySand() {
        int wetSand = 0;
        int wetTrees = 0;
        int drySand = 0;
        int dryTrees = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                Tile tile = map.getTile(x, y);
                if (tile.getType() != TileType.SAND) {
                    continue;
                }
                if (waterWithin(x, y)) {
                    wetSand++;
                    wetTrees += isTree(tile) ? 1 : 0;
                } else {
                    drySand++;
                    dryTrees += isTree(tile) ? 1 : 0;
                }
            }
        }
        double wet = wetTrees / (double) wetSand;
        double dry = dryTrees / (double) drySand;
        assertTrue(wet > 0.1, "wet share " + wet);
        assertTrue(wet >= 5 * dry, "wet " + wet + " dry " + dry);
    }

    @Test
    void aFewOasesAreDugIntoTheSand() {
        int oases = 0;
        for (int x = 1; x < SIZE - 1; x++) {
            for (int y = 1; y < SIZE - 1; y++) {
                if (map.getTile(x, y).getType() != TileType.DEEP_WATER) {
                    continue;
                }
                boolean ring = true;
                for (int dx = -1; dx <= 1 && ring; dx++) {
                    for (int dy = -1; dy <= 1 && ring; dy++) {
                        if ((dx != 0 || dy != 0) && map.getTile(x + dx, y + dy).getType() != TileType.SHALLOW_WATER) {
                            ring = false;
                        }
                    }
                }
                oases += ring ? 1 : 0;
            }
        }
        assertTrue(oases >= 2 && oases <= 12, "oases " + oases);
    }

    @Test
    void theSameSeedGrowsTheSameTrees() {
        TileMap again = generate(SEED);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                assertEquals(isTree(map.getTile(x, y)), isTree(again.getTile(x, y)), x + "," + y);
                assertEquals(map.getTile(x, y).getType(), again.getTile(x, y).getType(), x + "," + y);
            }
        }
    }
}
