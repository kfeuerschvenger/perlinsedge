package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The spawn pass runs before the balancing and the forest floor, so even a map with no free square for the player,
 * where the spawn has to clear the trees around it, ends with its trees at their minimum and a forest floor that
 * follows them.
 */
class SpawnBeforeBalanceTest {

    private static final int SIDE = 30;
    private static final int AREA = SIDE * SIDE;
    private static final int LATTICE_STEP = 3;
    private static final int RADIUS = 3;
    private static final int PER_100K = 100_000;
    private static final float TREE_NOISE = 0.5F;

    /** Grass planted with trees along every third row and column, so no 7 by 7 square of it is free. */
    private static GenerationContext lattice() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);
        TestContexts.fillField(context, NoiseField.TREE, TREE_NOISE);
        for (var index = 0; index < AREA; index++) {
            if (context.xOf(index) % LATTICE_STEP == 0 || context.yOf(index) % LATTICE_STEP == 0) {
                context.setResource(index, ResourceType.TREE);
            }
        }
        return context;
    }

    private static boolean hasTreeNear(GenerationContext context, int x, int y) {
        for (var dy = -1; dy <= 1; dy++) {
            for (var dx = -1; dx <= 1; dx++) {
                if (context.isOnMap(x + dx, y + dy)
                        && context.resourceAt(context.index(x + dx, y + dy)) == ResourceType.TREE) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void aMapWithNoFreeSquareStillMeetsTheTreeQuotaAndTheForestFloorRule() {
        final var context = lattice();
        final var planted = TestContexts.countResource(context, ResourceType.TREE);
        final var minPer100k = (int) Math.ceil(planted * (double) PER_100K / AREA);
        final var quota = ResourceQuota.onFertile(ResourceType.TREE, minPer100k, ResourceQuota.UNLIMITED);
        final var balance = new ResourceBalancePass(new BalanceParams(List.of(quota)));
        final var floor = new ForestFloorPass(new ForestFloorParams(1,
                EnumSet.of(TileType.GRASS, TileType.SAVANNA)));

        new SpawnPass(new SpawnParams(RADIUS)).apply(context);
        final var spawn = context.spawnIndex();
        final var freed = planted - TestContexts.countResource(context, ResourceType.TREE);
        balance.apply(context);
        floor.apply(context);

        assertTrue(freed > 0, "the spawn had to clear trees for this test to mean anything");
        assertTrue(TestContexts.countResource(context, ResourceType.TREE) >= quota.minFor(AREA));
        assertTrue(balance.shortfalls(context).isEmpty());
        for (var dy = -RADIUS; dy <= RADIUS; dy++) {
            for (var dx = -RADIUS; dx <= RADIUS; dx++) {
                final var x = context.xOf(spawn) + dx;
                final var y = context.yOf(spawn) + dy;
                assertTrue(!context.isOnMap(x, y) || context.resourceAt(context.index(x, y)) == null,
                        "a tree stands in the spawn square at " + x + "," + y);
            }
        }
        for (var index = 0; index < AREA; index++) {
            final var ground = context.groundAt(index);
            final var tree = context.resourceAt(index) == ResourceType.TREE;
            assertTrue(ground != TileType.FOREST || hasTreeNear(context, context.xOf(index), context.yOf(index)),
                    "forest floor without a tree near it at " + index);
            assertTrue(!tree || ground == TileType.FOREST, "a tree stands on " + ground + " at " + index);
        }
        assertEquals(TileType.GRASS, context.groundAt(spawn));
    }
}
