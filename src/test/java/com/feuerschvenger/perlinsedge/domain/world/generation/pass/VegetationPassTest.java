package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VegetationPassTest {

    private static final int SIDE = 8;
    private static final float DENSITY = 0.3F;
    private static final float DENSE_NOISE = 0.8F;
    private static final float SPARSE_NOISE = 0.6F;
    private static final float FULL = 1F;
    private static final float NONE = 0F;
    private static final float OVER = 1.1F;

    private static GenerationContext map(TileType ground, float treeNoise) {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, ground);
        TestContexts.fillField(context, NoiseField.TREE, treeNoise);
        return context;
    }

    private static void apply(GenerationContext context, float density) {
        new VegetationPass(new VegetationParams(density)).apply(context);
    }

    @Test
    void treesGrowWhereTheNoiseExceedsOneMinusTheDensity() {
        final var dense = map(TileType.GRASS, DENSE_NOISE);
        final var sparse = map(TileType.GRASS, SPARSE_NOISE);

        apply(dense, DENSITY);
        apply(sparse, DENSITY);

        assertEquals(dense.size(), TestContexts.countResource(dense, ResourceType.TREE));
        assertEquals(0, TestContexts.countResource(sparse, ResourceType.TREE));
    }

    @Test
    void reservedTilesGrowNoTree() {
        final var context = map(TileType.GRASS, DENSE_NOISE);
        final var reserved = context.index(0, 0);
        context.reserve(reserved);

        apply(context, DENSITY);

        assertNull(context.resourceAt(reserved));
        assertEquals(context.size() - 1, TestContexts.countResource(context, ResourceType.TREE));
    }

    @Test
    void treesGrowOnFertileGroundOnly() {
        for (var ground : TileType.values()) {
            final var context = map(ground, DENSE_NOISE);

            apply(context, DENSITY);

            final var expected = ground.isFertile() ? context.size() : 0;
            assertEquals(expected, TestContexts.countResource(context, ResourceType.TREE), ground.name());
        }
    }

    @Test
    void aTileThatHoldsAResourceKeepsIt() {
        final var context = map(TileType.GRASS, DENSE_NOISE);
        context.setResource(0, ResourceType.STONE);

        apply(context, DENSITY);

        assertEquals(ResourceType.STONE, context.resourceAt(0));
    }

    @Test
    void theExtremeDensitiesGrowEverywhereOrNowhere() {
        final var all = map(TileType.GRASS, SPARSE_NOISE);
        final var none = map(TileType.GRASS, DENSE_NOISE);

        apply(all, FULL);
        apply(none, NONE);

        assertEquals(all.size(), TestContexts.countResource(all, ResourceType.TREE));
        assertEquals(0, TestContexts.countResource(none, ResourceType.TREE));
    }

    @Test
    void theParametersRefuseADensityThatIsNotAShare() {
        assertThrows(IllegalArgumentException.class, () -> new VegetationParams(-DENSITY));
        assertThrows(IllegalArgumentException.class, () -> new VegetationParams(OVER));
        assertThrows(IllegalArgumentException.class, () -> new VegetationParams(Float.NaN));
        assertThrows(NullPointerException.class, () -> new VegetationPass(null));
    }
}
