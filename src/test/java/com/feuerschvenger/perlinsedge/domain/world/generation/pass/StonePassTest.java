package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StonePassTest {

    private static final int SIDE = 8;
    private static final int BIG_SIDE = 100;
    private static final int ALWAYS = RockResourceParams.PER_MILLE_SCALE;
    private static final int ALWAYS_LOOSE = Chance.TEN_THOUSAND_SCALE;
    private static final int HALF_PER_MILLE = 500;
    private static final int LOOSE_PER_TEN_THOUSAND = 400;
    private static final float ABOVE = 0.9F;
    private static final float BELOW = 0.5F;
    private static final int IRON_PER_MILLE = 300;
    private static final double TOLERANCE = 0.25;

    private static GenerationContext map(TileType ground, int side) {
        final var context = TestContexts.of(side, side);
        TestContexts.fillGround(context, ground);
        return context;
    }

    @Test
    void stoneFillsTheRockyGroundAtTheRockyChance() {
        final var context = map(TileType.ROCKY_GROUND, SIDE);

        new StonePass(new StoneParams(ALWAYS, 0)).apply(context);

        assertEquals(context.size(), TestContexts.countResource(context, ResourceType.STONE));
    }

    @Test
    void theRockyChanceGivesAboutThatShareOfTheRockyGround() {
        final var context = map(TileType.ROCKY_GROUND, BIG_SIDE);

        new StonePass(new StoneParams(HALF_PER_MILLE, 0)).apply(context);

        final var expected = context.size() / 2.0;
        assertEquals(expected, TestContexts.countResource(context, ResourceType.STONE), expected * TOLERANCE);
    }

    @Test
    void looseStonesLieOnWalkableLandAtTheLooseChance() {
        for (var ground : new TileType[] {TileType.GRASS, TileType.SAND, TileType.SNOW, TileType.SWAMP}) {
            final var context = map(ground, SIDE);

            new StonePass(new StoneParams(0, ALWAYS_LOOSE)).apply(context);

            assertEquals(context.size(), TestContexts.countResource(context, ResourceType.STONE), ground.name());
        }
    }

    @Test
    void theLooseChanceIsRare() {
        final var context = map(TileType.GRASS, BIG_SIDE);

        new StonePass(new StoneParams(0, LOOSE_PER_TEN_THOUSAND)).apply(context);

        final var expected = context.size() * (double) LOOSE_PER_TEN_THOUSAND / Chance.TEN_THOUSAND_SCALE;
        assertEquals(expected, TestContexts.countResource(context, ResourceType.STONE), expected * TOLERANCE * 2);
    }

    @Test
    void noStoneOnWaterIceOrMountains() {
        for (var ground : new TileType[] {TileType.DEEP_WATER, TileType.SHALLOW_WATER, TileType.RIVER, TileType.ICE,
            TileType.MOUNTAIN}) {
            final var context = map(ground, SIDE);

            new StonePass(new StoneParams(ALWAYS, ALWAYS_LOOSE)).apply(context);

            assertEquals(0, TestContexts.countResource(context, ResourceType.STONE), ground.name());
        }
    }

    @Test
    void aTileThatHoldsAResourceKeepsIt() {
        final var context = map(TileType.ROCKY_GROUND, SIDE);
        context.setResource(0, ResourceType.IRON_ORE);

        new StonePass(new StoneParams(ALWAYS, 0)).apply(context);

        assertEquals(ResourceType.IRON_ORE, context.resourceAt(0));
        assertEquals(context.size() - 1, TestContexts.countResource(context, ResourceType.STONE));
    }

    @Test
    void theAmountOfStoneNeverChangesTheAmountOfIron() {
        var baseline = -1;
        for (var perMille : new int[] {0, HALF_PER_MILLE, ALWAYS}) {
            final var context = map(TileType.ROCKY_GROUND, SIDE);
            TestContexts.fillField(context, NoiseField.CRYSTALS, BELOW);

            new RockResourcePass(new RockResourceParams(IRON_PER_MILLE, 0, ABOVE)).apply(context);
            new StonePass(new StoneParams(perMille, 0)).apply(context);

            final var iron = TestContexts.countResource(context, ResourceType.IRON_ORE);
            baseline = baseline < 0 ? iron : baseline;
            assertEquals(baseline, iron, "chance " + perMille);
            assertTrue(iron > 0);
        }
    }

    @Test
    void theParametersRefuseAChanceOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new StoneParams(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new StoneParams(ALWAYS + 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new StoneParams(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new StoneParams(0, ALWAYS_LOOSE + 1));
        assertThrows(NullPointerException.class, () -> new StonePass(null));
    }
}
