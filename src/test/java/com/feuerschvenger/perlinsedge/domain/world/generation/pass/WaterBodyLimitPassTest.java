package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The map is 10 by 10 tiles of grass and the bodies are laid out by hand, so the sizes are known. */
class WaterBodyLimitPassTest {

    private static final int SIDE = 10;
    private static final int AREA = SIDE * SIDE;
    /** On a map of 100 tiles this keeps two bodies. */
    private static final int KEEP_TWO = 2_000;
    private static final int KEEP_ONE = 500;
    private static final int NO_MINIMUM = 1;
    private static final int BIG = 6;
    private static final int MEDIUM = 3;

    private static GenerationContext grassWithBodies() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);
        // A body of 6 tiles on the first row, one of 3 on the third and a single tile on the fifth
        for (var x = 0; x < BIG; x++) {
            context.setGround(context.index(x, 0), TileType.DEEP_WATER);
        }
        for (var x = 0; x < MEDIUM; x++) {
            context.setGround(context.index(x, 2), TileType.SHALLOW_WATER);
        }
        context.setGround(context.index(0, 4), TileType.SHALLOW_WATER);
        return context;
    }

    private static void apply(GenerationContext context, int keepPer100k, int minKeptTiles, TileType fill) {
        new WaterBodyLimitPass(new WaterBodyParams(keepPer100k, minKeptTiles, fill)).apply(context);
    }

    @Test
    void onlyTheLargestBodiesStayAndTheOthersAreFilled() {
        final var context = grassWithBodies();

        apply(context, KEEP_TWO, NO_MINIMUM, TileType.SAND);

        assertEquals(BIG, TestContexts.countGround(context, TileType.DEEP_WATER));
        assertEquals(MEDIUM, TestContexts.countGround(context, TileType.SHALLOW_WATER));
        assertEquals(1, TestContexts.countGround(context, TileType.SAND));
        assertEquals(TileType.SAND, context.groundAt(context.index(0, 4)));
    }

    @Test
    void aBodyBelowTheMinimumIsFilledEvenWhenThereIsRoomToKeepIt() {
        final var context = grassWithBodies();

        apply(context, KEEP_TWO, MEDIUM + 1, TileType.GRASS);

        assertEquals(BIG, TestContexts.countGround(context, TileType.DEEP_WATER));
        assertEquals(0, TestContexts.countGround(context, TileType.SHALLOW_WATER));
    }

    @Test
    void aMapKeepsAtLeastOneBodyWhateverItsSizeWhenTheDensityIsAboveZero() {
        final var context = grassWithBodies();

        apply(context, 1, NO_MINIMUM, TileType.GRASS);

        assertEquals(BIG, TestContexts.countGround(context, TileType.DEEP_WATER));
        assertEquals(0, TestContexts.countGround(context, TileType.SHALLOW_WATER));
    }

    @Test
    void theKeepCountIsTheMostBodiesAndAMapKeepsNoneWhenEvenTheLargestIsBelowTheMinimum() {
        final var context = grassWithBodies();
        final var pass = new WaterBodyLimitPass(new WaterBodyParams(1, BIG + 1, TileType.GRASS));

        pass.apply(context);

        assertEquals(1, pass.keepCount(AREA));
        assertEquals(AREA, TestContexts.countGround(context, TileType.GRASS));
    }

    @Test
    void aDensityOfZeroKeepsNoBody() {
        final var context = grassWithBodies();

        apply(context, 0, NO_MINIMUM, TileType.GRASS);

        assertEquals(AREA, TestContexts.countGround(context, TileType.GRASS));
    }

    @Test
    void riversJoinTheBodyTheyFlowInto() {
        final var context = grassWithBodies();
        // A river from the big body down to the third row joins it to the medium one
        context.setGround(context.index(0, 1), TileType.RIVER);

        apply(context, KEEP_ONE, NO_MINIMUM, TileType.GRASS);

        assertEquals(BIG + MEDIUM + 1, TestContexts.countGround(context, TileType.DEEP_WATER)
                + TestContexts.countGround(context, TileType.SHALLOW_WATER)
                + TestContexts.countGround(context, TileType.RIVER));
        assertEquals(TileType.GRASS, context.groundAt(context.index(0, 4)));
    }

    @Test
    void iceIsNotWaterAndStays() {
        final var context = grassWithBodies();
        context.setGround(context.index(SIDE - 1, SIDE - 1), TileType.ICE);

        apply(context, 0, NO_MINIMUM, TileType.GRASS);

        assertEquals(TileType.ICE, context.groundAt(context.index(SIDE - 1, SIDE - 1)));
    }

    @Test
    void aMapWithoutWaterIsLeftAlone() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.SAND);

        apply(context, KEEP_TWO, NO_MINIMUM, TileType.GRASS);

        assertEquals(AREA, TestContexts.countGround(context, TileType.SAND));
    }

    @Test
    void theNumberOfKeptBodiesScalesWithTheArea() {
        final var pass = new WaterBodyLimitPass(new WaterBodyParams(KEEP_TWO, NO_MINIMUM, TileType.GRASS));

        assertEquals(2, pass.keepCount(AREA));
        assertEquals(8, pass.keepCount(4 * AREA));
        assertEquals(1, pass.keepCount(1));
    }

    @Test
    void theParametersRefuseNumbersThatCannotWork() {
        assertThrows(IllegalArgumentException.class, () -> new WaterBodyParams(-1, 1, TileType.GRASS));
        assertThrows(IllegalArgumentException.class,
                () -> new WaterBodyParams(WaterBodyParams.MAX_KEEP_PER_100K + 1, 1, TileType.GRASS));
        assertThrows(IllegalArgumentException.class, () -> new WaterBodyParams(1, 0, TileType.GRASS));
        assertThrows(IllegalArgumentException.class, () -> new WaterBodyParams(1, 1, TileType.SHALLOW_WATER));
        assertThrows(IllegalArgumentException.class, () -> new WaterBodyParams(1, 1, TileType.ICE));
        assertThrows(NullPointerException.class, () -> new WaterBodyParams(1, 1, null));
        assertThrows(NullPointerException.class, () -> new WaterBodyLimitPass(null));
    }

    @Test
    void thePassIsNamed() {
        final var pass = new WaterBodyLimitPass(new WaterBodyParams(1, 1, TileType.GRASS));

        assertEquals("water-body-limit", pass.name());
    }
}
