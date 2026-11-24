package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RockResourcePassTest {

    private static final int SIDE = 100;
    private static final int SMALL_SIDE = 16;
    private static final int PER_MILLE = 60;
    private static final int ALWAYS = RockResourceParams.PER_MILLE_SCALE;
    private static final float THRESHOLD = 0.8F;
    private static final float THRESHOLD_ABOVE_ONE = 1.1F;
    private static final float THRESHOLD_BELOW_ZERO = -0.1F;
    private static final float ABOVE = 0.9F;
    private static final float BELOW = 0.1F;
    private static final double SHARE_TOLERANCE = 0.4;
    private static final long SEED = 3L;
    private static final long OTHER_SEED = 4L;

    private static GenerationContext rocky(int side, TileType ground, float crystalNoise, long seed) {
        final var context = TestContexts.of(MapType.MOUNTAINOUS, seed, side, side, TestContexts.FLAT);
        TestContexts.fillGround(context, ground);
        TestContexts.fillField(context, NoiseField.CRYSTALS, crystalNoise);
        return context;
    }

    private static void apply(GenerationContext context, int iron, int crystals) {
        new RockResourcePass(new RockResourceParams(iron, crystals, THRESHOLD)).apply(context);
    }

    @Test
    void ironLiesOnHillsAndMountainsOnly() {
        for (var ground : TileType.values()) {
            final var context = rocky(SMALL_SIDE, ground, BELOW, SEED);

            apply(context, ALWAYS, 0);

            final var expected = ground == TileType.HILL || ground == TileType.MOUNTAIN ? context.size() : 0;
            assertEquals(expected, TestContexts.countResource(context, ResourceType.IRON_ORE), ground.name());
        }
    }

    @Test
    void crystalsNeedTheCrystalNoiseToPassItsThreshold() {
        final var low = rocky(SMALL_SIDE, TileType.HILL, BELOW, SEED);
        final var high = rocky(SMALL_SIDE, TileType.HILL, ABOVE, SEED);

        apply(low, 0, ALWAYS);
        apply(high, 0, ALWAYS);

        assertEquals(0, TestContexts.countResource(low, ResourceType.CRYSTALS));
        assertEquals(high.size(), TestContexts.countResource(high, ResourceType.CRYSTALS));
    }

    @Test
    void aTileThatPassesBothChancesIsCrystalsNotIron() {
        final var context = rocky(SMALL_SIDE, TileType.MOUNTAIN, ABOVE, SEED);

        apply(context, ALWAYS, ALWAYS);

        assertEquals(context.size(), TestContexts.countResource(context, ResourceType.CRYSTALS));
        assertEquals(0, TestContexts.countResource(context, ResourceType.IRON_ORE));
    }

    @Test
    void theShareOfIronFollowsTheChanceInThousandths() {
        final var context = rocky(SIDE, TileType.HILL, BELOW, SEED);

        apply(context, PER_MILLE, 0);

        final var share = TestContexts.countResource(context, ResourceType.IRON_ORE) / (double) context.size();
        final var expected = PER_MILLE / (double) RockResourceParams.PER_MILLE_SCALE;
        assertTrue(Math.abs(share - expected) < expected * SHARE_TOLERANCE, "share " + share);
    }

    @Test
    void ironAndCrystalChancesAreIndependentOfEachOther() {
        final var context = rocky(SIDE, TileType.HILL, ABOVE, SEED);

        apply(context, PER_MILLE, PER_MILLE);

        final var crystals = TestContexts.countResource(context, ResourceType.CRYSTALS);
        final var iron = TestContexts.countResource(context, ResourceType.IRON_ORE);
        final var expectedIron = context.size() * (double) PER_MILLE / RockResourceParams.PER_MILLE_SCALE;
        // Iron takes the tiles crystals did not take, so correlated salts would leave far fewer than the chance says
        assertTrue(crystals > 0 && iron > expectedIron * (1 - SHARE_TOLERANCE) - crystals, "iron " + iron);
    }

    @Test
    void theSameSeedGivesTheSameOreAndAnotherSeedGivesOther() {
        final var first = rocky(SIDE, TileType.HILL, ABOVE, SEED);
        final var second = rocky(SIDE, TileType.HILL, ABOVE, SEED);
        final var other = rocky(SIDE, TileType.HILL, ABOVE, OTHER_SEED);

        apply(first, PER_MILLE, PER_MILLE);
        apply(second, PER_MILLE, PER_MILLE);
        apply(other, PER_MILLE, PER_MILLE);

        assertEquals(Arrays.asList(first.resources()), Arrays.asList(second.resources()));
        assertNotEquals(Arrays.asList(first.resources()), Arrays.asList(other.resources()));
    }

    @Test
    void theParametersRefuseChancesOutsideTheScale() {
        assertThrows(IllegalArgumentException.class, () -> new RockResourceParams(-1, 0, THRESHOLD));
        assertThrows(IllegalArgumentException.class, () -> new RockResourceParams(0, ALWAYS + 1, THRESHOLD));
        assertThrows(IllegalArgumentException.class, () -> new RockResourceParams(0, 0, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new RockResourceParams(0, 0, THRESHOLD_ABOVE_ONE));
        assertThrows(IllegalArgumentException.class, () -> new RockResourceParams(0, 0, THRESHOLD_BELOW_ZERO));
        assertThrows(NullPointerException.class, () -> new RockResourcePass(null));
    }

    @Test
    void theParametersAcceptTheBoundariesOfTheScaleAndOfTheThreshold() {
        assertDoesNotThrow(() -> new RockResourceParams(0, 0, 0F));
        assertDoesNotThrow(() -> new RockResourceParams(ALWAYS, ALWAYS, 1F));
    }
}
