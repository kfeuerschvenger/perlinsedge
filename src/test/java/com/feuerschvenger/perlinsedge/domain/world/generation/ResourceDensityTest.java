package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps open ground the common case: with the shipped densities, a minority of land tiles carries a resource.
 */
class ResourceDensityTest {

    private static final int SIZE = 150;
    private static final long SEED = 20_261_111L;
    private static final double MIN_SHARE = 0.08;
    private static final double MAX_SHARE = 0.25;

    private final MapType original = AppConfig.getInstance().world().getCurrentMapType();

    @AfterEach
    void restoreMapType() {
        AppConfig.getInstance().world().setCurrentMapType(original);
    }

    private static double resourceShare(MapType type) {
        AppConfig.WorldConfig world = AppConfig.getInstance().world();
        world.setCurrentMapType(type);
        NoiseSettings profile = NoiseSettings.heightProfile(type);
        MapGenerationContext context = new MapGenerationContext(type, SEED, profile.frequency(), profile.octaves(),
                world.getTemperatureNoiseFrequency(), world.getTemperatureNoiseOctaves(),
                world.getMoistureNoiseFrequency(), world.getMoistureNoiseOctaves(),
                world.getStonePatchFrequency(), world.getStoneThreshold(),
                world.getCrystalsPatchFrequency(), world.getCrystalsThreshold(),
                world.getTreeDensity(), world.getRiverDensity());
        TileMap map = new PerlinMapGenerator(FastNoiseLiteAdapter.FACTORY).generateMap(SIZE, SIZE, context);

        int land = 0;
        int withResource = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Tile tile = map.getTile(x, y);
                if (tile.getType().isLand()) {
                    land++;
                    if (tile.hasResource()) {
                        withResource++;
                    }
                }
            }
        }
        return (double) withResource / land;
    }

    @Test
    void everySelectableMapKeepsMostLandOpen() {
        StringBuilder outside = new StringBuilder();
        for (MapType type : new MapType[]{MapType.DARK_FOREST, MapType.SWAMP, MapType.ISLANDS, MapType.DESERT,
                MapType.TUNDRA, MapType.RIVERS, MapType.MEDITERRANEAN}) {
            double share = resourceShare(type);
            if (share < MIN_SHARE || share > MAX_SHARE) {
                outside.append(type).append('=').append(share).append(' ');
            }
        }
        assertTrue(outside.isEmpty(), "resource share outside the band: " + outside);
    }
}
