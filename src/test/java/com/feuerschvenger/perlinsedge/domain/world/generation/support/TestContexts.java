package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapRequest;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSourceFactory;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Arrays;

/** Builds the small contexts the unit tests of the generation passes work on. */
public final class TestContexts {

    /** A noise factory whose sources are zero everywhere, for contexts whose fields the test fills by hand. */
    public static final NoiseSourceFactory FLAT = (long seed, NoiseSettings settings) -> (float x, float y) -> 0F;

    private static final long DEFAULT_SEED = 1L;

    private TestContexts() {
    }

    /**
     * Creates a context of a desert map with a fixed seed and flat noise.
     *
     * @param width  the number of columns
     * @param height the number of rows
     * @return the new context
     */
    public static GenerationContext of(int width, int height) {
        return of(MapType.DESERT, DEFAULT_SEED, width, height, FLAT);
    }

    /**
     * Creates a context.
     *
     * @param type   the kind of map
     * @param seed   the seed
     * @param width  the number of columns
     * @param height the number of rows
     * @param noise  where the noise sources come from
     * @return the new context
     */
    public static GenerationContext of(MapType type, long seed, int width, int height, NoiseSourceFactory noise) {
        return new GenerationContext(new MapRequest(type, seed, width, height), noise);
    }

    /**
     * Sets the terrain of every tile.
     *
     * @param context the context
     * @param type    the terrain
     */
    public static void fillGround(GenerationContext context, TileType type) {
        for (var index = 0; index < context.size(); index++) {
            context.setGround(index, type);
        }
    }

    /**
     * Creates a noise field with the same value on every tile.
     *
     * @param context the context, which must not have the field yet
     * @param field   the field
     * @param value   the value
     * @return the array of the field
     */
    public static float[] fillField(GenerationContext context, NoiseField field, float value) {
        final var values = context.defineField(field);
        Arrays.fill(values, value);
        return values;
    }

    /**
     * Counts the tiles that hold a resource.
     *
     * @param context  the context
     * @param resource the resource
     * @return the number of tiles
     */
    public static int countResource(GenerationContext context, ResourceType resource) {
        var count = 0;
        for (var index = 0; index < context.size(); index++) {
            count += context.resourceAt(index) == resource ? 1 : 0;
        }
        return count;
    }

    /**
     * Counts the tiles of a terrain.
     *
     * @param context the context
     * @param type    the terrain
     * @return the number of tiles
     */
    public static int countGround(GenerationContext context, TileType type) {
        var count = 0;
        for (var index = 0; index < context.size(); index++) {
            count += context.groundAt(index) == type ? 1 : 0;
        }
        return count;
    }
}
