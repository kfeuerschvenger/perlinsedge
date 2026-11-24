package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import com.feuerschvenger.perlinsedge.domain.world.generation.PerlinMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;

/**
 * The seam between the harness and whatever generates maps. The invariants, the painter and the preview tool only know
 * this interface, so moving them to the new pipeline means writing one more source and changing {@link #current()}.
 */
@FunctionalInterface
public interface MapSource {

    /**
     * Generates the map a spec describes.
     *
     * @param spec the type, seed and size
     * @return a freshly generated map
     */
    TileMap generate(MapSpec spec);

    /** @return the source the game uses today, which is the one the harness checks */
    static MapSource current() {
        return MapSource::generateWithPerlin;
    }

    /**
     * The perlin generator reads some settings from the configuration singleton, so the map type is set for the run and
     * put back afterwards, which leaves other tests in the same JVM as they were.
     */
    private static TileMap generateWithPerlin(MapSpec spec) {
        final var world = AppConfig.getInstance().world();
        final var original = world.getCurrentMapType();
        world.setCurrentMapType(spec.type());
        try {
            final var profile = NoiseSettings.heightProfile(spec.type());
            final var context = new MapGenerationContext(spec.type(), spec.seed(), profile.frequency(),
                    profile.octaves(), world.getTemperatureNoiseFrequency(), world.getTemperatureNoiseOctaves(),
                    world.getMoistureNoiseFrequency(), world.getMoistureNoiseOctaves(),
                    world.getStonePatchFrequency(), world.getStoneThreshold(),
                    world.getCrystalsPatchFrequency(), world.getCrystalsThreshold(),
                    world.getTreeDensity(), world.getRiverDensity());
            return new PerlinMapGenerator(FastNoiseLiteAdapter.FACTORY).generateMap(spec.size(), spec.size(), context);
        } finally {
            world.setCurrentMapType(original);
        }
    }
}
