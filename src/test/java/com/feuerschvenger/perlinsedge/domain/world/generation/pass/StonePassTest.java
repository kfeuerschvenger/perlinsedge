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
    void theParametersRefuseANumberThatIsNotOne() {
        assertThrows(IllegalArgumentException.class, () -> new StoneParams(Float.NaN));
        assertThrows(NullPointerException.class, () -> new StonePass(null));
    }
}
