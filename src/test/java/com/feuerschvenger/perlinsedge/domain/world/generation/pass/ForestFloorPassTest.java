package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForestFloorPassTest {

    private static final int SIDE = 9;
    private static final int CENTRE = SIDE / 2;
    private static final int SQUARE = 3;
    private static final int CORNER_TILES = 4;
    private static final Set<TileType> FLOORS = EnumSet.of(TileType.GRASS, TileType.SAVANNA);

    private static GenerationContext grass() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);
        return context;
    }

    private static void apply(GenerationContext context, int reach) {
        new ForestFloorPass(new ForestFloorParams(reach, FLOORS)).apply(context);
    }

    private static void plant(GenerationContext context, int x, int y) {
        context.setResource(context.index(x, y), ResourceType.TREE);
    }

    @Test
    void aTreeFloorsItsTileAndTheTilesAroundIt() {
        final var context = grass();
        plant(context, CENTRE, CENTRE);

        apply(context, 1);

        assertEquals(SQUARE * SQUARE, TestContexts.countGround(context, TileType.FOREST));
        assertEquals(TileType.FOREST, context.groundAt(context.index(CENTRE + 1, CENTRE - 1)));
        assertEquals(TileType.GRASS, context.groundAt(context.index(CENTRE + 2, CENTRE)));
    }

    @Test
    void aReachOfZeroFloorsOnlyTheTileOfTheTree() {
        final var context = grass();
        plant(context, CENTRE, CENTRE);

        apply(context, 0);

        assertEquals(1, TestContexts.countGround(context, TileType.FOREST));
        assertEquals(TileType.FOREST, context.groundAt(context.index(CENTRE, CENTRE)));
    }

    @Test
    void theFloorStopsAtTheEdgeOfTheMap() {
        final var context = grass();
        plant(context, 0, 0);

        apply(context, 1);

        assertEquals(CORNER_TILES, TestContexts.countGround(context, TileType.FOREST));
    }

    @Test
    void onlyTheFloorTerrainsTurnIntoForest() {
        final var context = grass();
        context.setGround(context.index(CENTRE, CENTRE), TileType.SAND);
        context.setGround(context.index(CENTRE + 1, CENTRE), TileType.ROCKY_GROUND);
        context.setGround(context.index(CENTRE - 1, CENTRE), TileType.SAVANNA);
        plant(context, CENTRE, CENTRE);

        apply(context, 1);

        assertEquals(TileType.SAND, context.groundAt(context.index(CENTRE, CENTRE)));
        assertEquals(TileType.ROCKY_GROUND, context.groundAt(context.index(CENTRE + 1, CENTRE)));
        assertEquals(TileType.FOREST, context.groundAt(context.index(CENTRE - 1, CENTRE)));
        assertEquals(TileType.FOREST, context.groundAt(context.index(CENTRE, CENTRE + 1)));
    }

    @Test
    void reservedTilesKeepTheirGround() {
        final var context = grass();
        final var trail = context.index(CENTRE + 1, CENTRE);
        context.reserve(trail);
        plant(context, CENTRE, CENTRE);

        apply(context, 1);

        assertEquals(TileType.GRASS, context.groundAt(trail));
        assertEquals(SQUARE * SQUARE - 1, TestContexts.countGround(context, TileType.FOREST));
    }

    @Test
    void otherResourcesDoNotMakeForest() {
        final var context = grass();
        context.setResource(context.index(CENTRE, CENTRE), ResourceType.STONE);

        apply(context, 1);

        assertEquals(0, TestContexts.countGround(context, TileType.FOREST));
    }

    @Test
    void theParametersCopyTheFloorsAndRefuseABrokenReach() {
        final var floors = EnumSet.of(TileType.GRASS);
        final var params = new ForestFloorParams(1, floors);
        floors.add(TileType.SAND);

        assertEquals(Set.of(TileType.GRASS), params.floors());
        assertNotSame(params.floors(), params.floors());
        assertThrows(IllegalArgumentException.class, () -> new ForestFloorParams(-1, FLOORS));
        assertThrows(NullPointerException.class, () -> new ForestFloorParams(1, null));
        assertThrows(NullPointerException.class, () -> new ForestFloorPass(null));
    }
}
