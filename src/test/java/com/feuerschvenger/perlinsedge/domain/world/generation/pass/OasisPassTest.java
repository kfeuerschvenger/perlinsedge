package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OasisPassTest {

    private static final int SIDE = 40;
    private static final int TILES_PER_OASIS = 400;
    private static final int RADIUS = 2;
    private static final int ATTEMPTS = 50;
    private static final int TINY_SIDE = 6;
    private static final int EXPECTED_MAX = SIDE * SIDE / TILES_PER_OASIS;

    private static GenerationContext sandMap(int side) {
        final var context = TestContexts.of(side, side);
        TestContexts.fillGround(context, TileType.SAND);
        return context;
    }

    private static OasisParams params() {
        return new OasisParams(TILES_PER_OASIS, RADIUS, ATTEMPTS);
    }

    @Test
    void oasesAreDugIntoSandWithADeepMiddleInsideAShallowPocket() {
        final var context = sandMap(SIDE);

        new OasisPass(params()).apply(context);

        final var deep = TestContexts.countGround(context, TileType.DEEP_WATER);
        assertTrue(deep >= 1 && deep <= EXPECTED_MAX, "oases " + deep);
        for (var index = 0; index < context.size(); index++) {
            if (context.groundAt(index) == TileType.DEEP_WATER) {
                assertEquals(TileType.SHALLOW_WATER,
                        context.groundAt(context.index(context.xOf(index) + 1, context.yOf(index))));
                assertEquals(TileType.SHALLOW_WATER,
                        context.groundAt(context.index(context.xOf(index), context.yOf(index) - 1)));
            }
        }
    }

    @Test
    void noOasisReachesTheEdgeOfTheMap() {
        final var context = sandMap(SIDE);

        new OasisPass(params()).apply(context);

        for (var index = 0; index < context.size(); index++) {
            final var x = context.xOf(index);
            final var y = context.yOf(index);
            if (x < RADIUS || y < RADIUS || x >= SIDE - RADIUS || y >= SIDE - RADIUS) {
                assertEquals(TileType.SAND, context.groundAt(index));
            }
        }
    }

    @Test
    void nothingIsDugWhereThereIsNoPlainSand() {
        final var context = sandMap(SIDE);
        TestContexts.fillGround(context, TileType.GRASS);

        new OasisPass(params()).apply(context);

        assertEquals(context.size(), TestContexts.countGround(context, TileType.GRASS));
    }

    @Test
    void aMapTooSmallForTheMarginIsLeftAlone() {
        final var context = sandMap(TINY_SIDE);

        new OasisPass(params()).apply(context);

        assertEquals(context.size(), TestContexts.countGround(context, TileType.SAND));
    }

    @Test
    void theSameSeedDigsTheSameOases() {
        final var first = sandMap(SIDE);
        final var second = sandMap(SIDE);

        new OasisPass(params()).apply(first);
        new OasisPass(params()).apply(second);

        assertEquals(java.util.Arrays.asList(first.ground()), java.util.Arrays.asList(second.ground()));
    }

    @Test
    void theParametersRefuseNumbersBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new OasisParams(0, RADIUS, ATTEMPTS));
        assertThrows(IllegalArgumentException.class, () -> new OasisParams(TILES_PER_OASIS, 0, ATTEMPTS));
        assertThrows(IllegalArgumentException.class, () -> new OasisParams(TILES_PER_OASIS, RADIUS, 0));
        assertThrows(NullPointerException.class, () -> new OasisPass(null));
    }
}
