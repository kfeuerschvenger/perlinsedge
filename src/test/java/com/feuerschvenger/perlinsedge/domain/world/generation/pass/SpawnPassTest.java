package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnPassTest {

    private static final int RADIUS = 3;
    private static final int OPEN_SIDE = 21;
    private static final int CENTRE = OPEN_SIDE / 2;
    private static final int WIDE = 40;
    private static final int WIDE_HEIGHT = 11;
    private static final int CHANNEL = 30;
    private static final int SMALL_SIDE_X = 35;
    private static final int SMALL_SIDE_Y = 5;
    private static final int HINT_SIDE = 41;
    private static final int HINT = 10;
    private static final int PACKED = 9;
    private static final int PACKED_CLEAR = 4;
    private static final int TIGHT = 7;
    private static final int TIGHT_CENTRE = 3;
    private static final int TOO_BIG = SpawnParams.MAX_FREE_RADIUS + 1;

    private static SpawnPass pass(int radius) {
        return new SpawnPass(new SpawnParams(radius));
    }

    private static GenerationContext grass(int width, int height) {
        final var context = TestContexts.of(width, height);
        TestContexts.fillGround(context, TileType.GRASS);
        return context;
    }

    private static int spawnX(GenerationContext context) {
        return context.xOf(context.spawnIndex());
    }

    private static int spawnY(GenerationContext context) {
        return context.yOf(context.spawnIndex());
    }

    @Test
    void onOpenGroundTheSpawnIsTheCentreOfTheMap() {
        final var context = grass(OPEN_SIDE, OPEN_SIDE);

        pass(RADIUS).apply(context);

        assertEquals(CENTRE, spawnX(context));
        assertEquals(CENTRE, spawnY(context));
    }

    @Test
    void aResourceAtTheCentreMovesTheSpawnUntilTheWholeRadiusIsFree() {
        final var context = grass(OPEN_SIDE, OPEN_SIDE);
        context.setResource(context.index(CENTRE, CENTRE), ResourceType.TREE);

        pass(RADIUS).apply(context);

        final var distance = Math.max(Math.abs(spawnX(context) - CENTRE), Math.abs(spawnY(context) - CENTRE));
        assertTrue(distance > RADIUS, "the tree is outside the radius of the spawn: " + distance);
        assertEquals(ResourceType.TREE, context.resourceAt(context.index(CENTRE, CENTRE)));
    }

    @Test
    void theSpawnIsInTheLargestRegionEvenWhenTheHintIsInASmallerOne() {
        final var context = grass(WIDE, WIDE_HEIGHT);
        for (var y = 0; y < WIDE_HEIGHT; y++) {
            context.setGround(context.index(CHANNEL, y), TileType.DEEP_WATER);
        }
        context.setSpawnHint(context.index(SMALL_SIDE_X, SMALL_SIDE_Y));

        pass(0).apply(context);

        assertTrue(spawnX(context) < CHANNEL, "the spawn is on the large side: " + spawnX(context));
    }

    @Test
    void theHintOfAPassIsTheAnchorOfTheSearch() {
        final var context = grass(HINT_SIDE, HINT_SIDE);
        context.setSpawnHint(context.index(HINT, HINT));

        pass(RADIUS).apply(context);

        assertEquals(HINT, spawnX(context));
        assertEquals(HINT, spawnY(context));
    }

    @Test
    void theSpawnKeepsItsRadiusOffTheEdgeOfTheMap() {
        final var context = grass(TIGHT, TIGHT);
        context.setSpawnHint(0);

        pass(RADIUS).apply(context);

        assertEquals(TIGHT_CENTRE, spawnX(context));
        assertEquals(TIGHT_CENTRE, spawnY(context));
    }

    @Test
    void whenNoTileHasFreeSurroundingsTheResourcesAroundTheNearestAreCleared() {
        final var context = grass(PACKED, PACKED);
        for (var index = 0; index < context.size(); index++) {
            context.setResource(index, ResourceType.TREE);
        }
        context.setResource(context.index(PACKED_CLEAR, PACKED_CLEAR), null);

        pass(RADIUS).apply(context);

        assertEquals(PACKED_CLEAR, spawnX(context));
        assertEquals(PACKED_CLEAR, spawnY(context));
        for (var y = PACKED_CLEAR - RADIUS; y <= PACKED_CLEAR + RADIUS; y++) {
            for (var x = PACKED_CLEAR - RADIUS; x <= PACKED_CLEAR + RADIUS; x++) {
                assertNull(context.resourceAt(context.index(x, y)), x + "," + y);
            }
        }
        assertEquals(ResourceType.TREE, context.resourceAt(0));
        assertEquals(context.size(), TestContexts.countGround(context, TileType.GRASS));
    }

    @Test
    void theSquareAroundTheSpawnIsReservedSoLaterPassesLeaveItFree() {
        final var context = grass(OPEN_SIDE, OPEN_SIDE);

        pass(RADIUS).apply(context);

        for (var index = 0; index < context.size(); index++) {
            final var near = Math.abs(context.xOf(index) - spawnX(context)) <= RADIUS
                    && Math.abs(context.yOf(index) - spawnY(context)) <= RADIUS;
            assertEquals(near, context.isReserved(index), "tile " + index);
        }
    }

    @Test
    void theSquareAroundAClearedSpawnIsReservedToo() {
        final var context = grass(PACKED, PACKED);
        for (var index = 0; index < context.size(); index++) {
            context.setResource(index, ResourceType.TREE);
        }
        context.setResource(context.index(PACKED_CLEAR, PACKED_CLEAR), null);

        pass(RADIUS).apply(context);

        assertTrue(context.isReserved(context.index(PACKED_CLEAR - RADIUS, PACKED_CLEAR + RADIUS)));
        assertTrue(context.isReserved(context.index(PACKED_CLEAR, PACKED_CLEAR)));
    }

    @Test
    void aMapWithNoFreeWalkableTileIsRefused() {
        final var context = TestContexts.of(PACKED, PACKED);
        TestContexts.fillGround(context, TileType.DEEP_WATER);

        final var spawnPass = pass(RADIUS);

        assertThrows(IllegalStateException.class, () -> spawnPass.apply(context));
    }

    @Test
    void tilesHeldByResourcesDoNotJoinTheRegionsTheyDivide() {
        final var context = grass(WIDE, WIDE_HEIGHT);
        for (var y = 0; y < WIDE_HEIGHT; y++) {
            context.setResource(context.index(CHANNEL, y), ResourceType.STONE);
        }
        context.setSpawnHint(context.index(SMALL_SIDE_X, SMALL_SIDE_Y));

        pass(0).apply(context);

        assertNotEquals(CHANNEL, spawnX(context));
        assertTrue(spawnX(context) < CHANNEL);
    }

    @Test
    void theParametersRefuseARadiusOutsideTheRange() {
        assertThrows(IllegalArgumentException.class, () -> new SpawnParams(-1));
        assertThrows(IllegalArgumentException.class, () -> new SpawnParams(TOO_BIG));
        assertThrows(NullPointerException.class, () -> new SpawnPass(null));
    }
}
