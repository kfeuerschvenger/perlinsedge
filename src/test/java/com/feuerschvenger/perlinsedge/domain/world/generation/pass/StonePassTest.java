package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StonePassTest {

    private static final int SIDE = 8;
    private static final float THRESHOLD = 0.79F;
    private static final float ABOVE = 0.9F;
    private static final float BELOW = 0.5F;
    private static final float HIGH_THRESHOLD = 0.99F;
    private static final float UNREACHABLE = 3F;
    private static final int IRON_PER_MILLE = 300;

    private static GenerationContext map(TileType ground, float stoneNoise) {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, ground);
        TestContexts.fillField(context, NoiseField.STONE, stoneNoise);
        return context;
    }

    private static void apply(GenerationContext context) {
        new StonePass(new StoneParams(THRESHOLD)).apply(context);
    }

    @Test
    void stoneFillsTheLandWhereTheNoisePassesTheThreshold() {
        for (var ground : new TileType[] {TileType.GRASS, TileType.SAND, TileType.SNOW, TileType.MOUNTAIN}) {
            final var context = map(ground, ABOVE);

            apply(context);

            assertEquals(context.size(), TestContexts.countResource(context, ResourceType.STONE), ground.name());
        }
    }

    @Test
    void noStoneBelowTheThresholdOrOnWaterAndIce() {
        final var low = map(TileType.GRASS, BELOW);
        apply(low);
        assertEquals(0, TestContexts.countResource(low, ResourceType.STONE));

        for (var ground : new TileType[] {TileType.DEEP_WATER, TileType.SHALLOW_WATER, TileType.RIVER,
            TileType.ICE}) {
            final var context = map(ground, ABOVE);

            apply(context);

            assertEquals(0, TestContexts.countResource(context, ResourceType.STONE), ground.name());
        }
    }

    @Test
    void aTileThatHoldsAResourceKeepsIt() {
        final var context = map(TileType.HILL, ABOVE);
        context.setResource(0, ResourceType.IRON_ORE);

        apply(context);

        assertEquals(ResourceType.IRON_ORE, context.resourceAt(0));
        assertEquals(context.size() - 1, TestContexts.countResource(context, ResourceType.STONE));
    }

    @Test
    void theAmountOfStoneNeverChangesTheAmountOfIron() {
        var baseline = -1;
        for (var threshold : new float[] {THRESHOLD, HIGH_THRESHOLD, UNREACHABLE}) {
            final var context = map(TileType.HILL, ABOVE);
            TestContexts.fillField(context, NoiseField.CRYSTALS, BELOW);

            new RockResourcePass(new RockResourceParams(IRON_PER_MILLE, 0, THRESHOLD)).apply(context);
            new StonePass(new StoneParams(threshold)).apply(context);

            final var iron = TestContexts.countResource(context, ResourceType.IRON_ORE);
            baseline = baseline < 0 ? iron : baseline;
            assertEquals(baseline, iron, "threshold " + threshold);
        }
    }

    @Test
    void theParametersRefuseANumberThatIsNotOne() {
        assertThrows(IllegalArgumentException.class, () -> new StoneParams(Float.NaN));
        assertThrows(NullPointerException.class, () -> new StonePass(null));
    }
}
