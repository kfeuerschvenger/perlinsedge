package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesertFloraPassTest {

    private static final int SIDE = 30;
    private static final int POOL_X = 15;
    private static final int POOL_Y = 15;
    private static final int BIG_SIDE = 100;
    private static final float DENSITY = 0.1F;
    private static final float DRY_FACTOR = 0.25F;
    private static final float WET_FACTOR = 2.5F;
    private static final float MAX_CHANCE = 0.35F;
    private static final float CERTAIN = 1F;
    private static final float IMPOSSIBLE = 0F;
    private static final float OVER = 1.5F;
    private static final double WET_OVER_DRY = 5.0;

    private static GenerationContext sand(int side) {
        final var context = TestContexts.of(side, side);
        TestContexts.fillGround(context, TileType.SAND);
        return context;
    }

    private static boolean isTree(GenerationContext context, int x, int y) {
        return context.resourceAt(context.index(x, y)) == ResourceType.TREE;
    }

    @Test
    void treesGrowOnlyWithinTheReachOfWaterWhenTheDryChanceIsZero() {
        final var context = sand(SIDE);
        context.setGround(context.index(POOL_X, POOL_Y), TileType.SHALLOW_WATER);

        new DesertFloraPass(new DesertFloraParams(CERTAIN, IMPOSSIBLE, CERTAIN, CERTAIN)).apply(context);

        for (var y = 0; y < SIDE; y++) {
            for (var x = 0; x < SIDE; x++) {
                final var distance = Math.max(Math.abs(x - POOL_X), Math.abs(y - POOL_Y));
                final var wet = distance <= ResourceType.PALM_REACH && distance > 0;
                assertEquals(wet, isTree(context, x, y), x + "," + y);
            }
        }
    }

    @Test
    void reservedTilesGrowNoTree() {
        final var context = sand(SIDE);
        final var reserved = context.index(0, 0);
        context.reserve(reserved);

        new DesertFloraPass(new DesertFloraParams(CERTAIN, CERTAIN, CERTAIN, CERTAIN)).apply(context);

        assertNull(context.resourceAt(reserved));
        assertTrue(isTree(context, 1, 0));
    }

    @Test
    void treesGatherAroundWaterFarThickerThanInTheDrySand() {
        final var context = sand(BIG_SIDE);
        for (var x = 0; x < BIG_SIDE; x++) {
            context.setGround(context.index(x, BIG_SIDE / 2), TileType.SHALLOW_WATER);
        }

        new DesertFloraPass(new DesertFloraParams(DENSITY, DRY_FACTOR, WET_FACTOR, MAX_CHANCE)).apply(context);

        var wetSand = 0;
        var wetTrees = 0;
        var drySand = 0;
        var dryTrees = 0;
        for (var y = 0; y < BIG_SIDE; y++) {
            final var wet = Math.abs(y - BIG_SIDE / 2) <= ResourceType.PALM_REACH;
            for (var x = 0; x < BIG_SIDE; x++) {
                if (context.groundAt(context.index(x, y)) != TileType.SAND) {
                    continue;
                }
                wetSand += wet ? 1 : 0;
                drySand += wet ? 0 : 1;
                wetTrees += wet && isTree(context, x, y) ? 1 : 0;
                dryTrees += !wet && isTree(context, x, y) ? 1 : 0;
            }
        }
        assertTrue(dryTrees > 0, "cacti grow in the dry sand");
        assertTrue(wetTrees / (double) wetSand >= WET_OVER_DRY * dryTrees / drySand);
    }

    @Test
    void treesGrowOnSandWithNoResourceOnly() {
        final var context = sand(SIDE);
        context.setGround(0, TileType.GRASS);
        context.setGround(1, TileType.SHALLOW_WATER);
        context.setResource(2, ResourceType.STONE);

        new DesertFloraPass(new DesertFloraParams(CERTAIN, CERTAIN, CERTAIN, CERTAIN)).apply(context);

        assertNull(context.resourceAt(0));
        assertNull(context.resourceAt(1));
        assertEquals(ResourceType.STONE, context.resourceAt(2));
        assertEquals(ResourceType.TREE, context.resourceAt(context.size() - 1));
    }

    @Test
    void theMaximumCapsTheChance() {
        final var context = sand(SIDE);

        new DesertFloraPass(new DesertFloraParams(CERTAIN, CERTAIN, CERTAIN, IMPOSSIBLE)).apply(context);

        assertEquals(0, TestContexts.countResource(context, ResourceType.TREE));
    }

    @Test
    void theParametersRefuseSharesOutsideTheRangeAndNegativeFactors() {
        assertThrows(IllegalArgumentException.class,
                () -> new DesertFloraParams(OVER, DRY_FACTOR, WET_FACTOR, MAX_CHANCE));
        assertThrows(IllegalArgumentException.class,
                () -> new DesertFloraParams(DENSITY, -DRY_FACTOR, WET_FACTOR, MAX_CHANCE));
        assertThrows(IllegalArgumentException.class,
                () -> new DesertFloraParams(DENSITY, DRY_FACTOR, Float.POSITIVE_INFINITY, MAX_CHANCE));
        assertThrows(IllegalArgumentException.class,
                () -> new DesertFloraParams(DENSITY, DRY_FACTOR, WET_FACTOR, OVER));
        assertThrows(NullPointerException.class, () -> new DesertFloraPass(null));
    }
}
