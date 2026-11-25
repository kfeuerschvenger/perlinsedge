package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FertileFillTest {

    private static final int SIDE = 10;
    private static final int AREA = SIDE * SIDE;
    private static final int MISSING = 12;
    private static final int FREE_TILES = 5;
    private static final int EXCLUDED_TILES = 2;
    private static final float TIED = 0.5F;
    private static final float HIGH = 0.9F;
    private static final float LOW = 0.1F;
    private static final long SEED = 11L;

    private static GenerationContext grass(float treeNoise) {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);
        TestContexts.fillField(context, NoiseField.TREE, treeNoise);
        return context;
    }

    private static void add(GenerationContext context, int missing) {
        new FertileFill(context, new SplittableRandom(SEED)).add(ResourceType.TREE, missing,
                ResourceQuota.Host.FERTILE, new int[AREA]);
    }

    @Test
    void whenEveryTileHasTheSameNoiseTheShareWidensToEveryTileAndTheMissingTreesAreAdded() {
        final var context = grass(TIED);

        add(context, MISSING);

        assertEquals(MISSING, TestContexts.countResource(context, ResourceType.TREE));
    }

    @Test
    void theTilesWithTheHighestNoiseAreChosenBeforeTheOthers() {
        final var context = grass(LOW);
        final var noise = context.field(NoiseField.TREE);
        for (var index = 0; index < MISSING; index++) {
            noise[index] = HIGH + (float) index / AREA;
        }

        add(context, MISSING);

        for (var index = 0; index < AREA; index++) {
            final var expected = index < MISSING ? ResourceType.TREE : null;
            assertEquals(expected, context.resourceAt(index), "tile " + index);
        }
    }

    @Test
    void fewerFreeTilesThanMissingTreesGetAllTheFreeTilesAndNothingThrows() {
        final var context = grass(TIED);
        for (var index = FREE_TILES; index < AREA; index++) {
            context.setResource(index, ResourceType.STONE);
        }

        add(context, MISSING);

        assertEquals(FREE_TILES, TestContexts.countResource(context, ResourceType.TREE));
    }

    @Test
    void reservedTilesAndOtherGroundAreNeverChosen() {
        final var context = grass(TIED);
        final var reserved = context.index(0, 0);
        context.reserve(reserved);
        final var rock = context.index(1, 0);
        context.setGround(rock, TileType.ROCKY_GROUND);

        add(context, AREA);

        assertNull(context.resourceAt(reserved));
        assertNull(context.resourceAt(rock));
        assertEquals(AREA - EXCLUDED_TILES, TestContexts.countResource(context, ResourceType.TREE));
    }

    @Test
    void aBufferShorterThanTheMapIsRefused() {
        final var context = grass(TIED);
        final var fill = new FertileFill(context, new SplittableRandom(SEED));
        final var shortBuffer = new int[AREA - 1];

        assertThrows(IllegalArgumentException.class,
                () -> fill.add(ResourceType.TREE, MISSING, ResourceQuota.Host.FERTILE, shortBuffer));
    }

    @Test
    void aMapWithoutCandidateTilesIsLeftAlone() {
        final var context = grass(TIED);
        TestContexts.fillGround(context, TileType.DEEP_WATER);

        add(context, MISSING);

        assertEquals(0, TestContexts.countResource(context, ResourceType.TREE));
    }
}
