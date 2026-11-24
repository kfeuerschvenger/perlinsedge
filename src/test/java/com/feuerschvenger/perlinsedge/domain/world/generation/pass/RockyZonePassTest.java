package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RockyZonePassTest {

    private static final int SIDE = 20;
    private static final int TILES = SIDE * SIDE;
    private static final float QUARTER = 0.25F;
    private static final float HALF = 0.5F;
    private static final double TOLERANCE = 0.04;
    private static final int REACH = 2;
    private static final int HALF_SIDE = SIDE / 2;
    private static final int HALF_TILES = TILES / 2;
    private static final int CENTRE = HALF_SIDE;
    private static final int SQUARE_OF_REACH = (2 * REACH + 1) * (2 * REACH + 1);
    private static final Set<TileType> GRASS_ONLY = EnumSet.of(TileType.GRASS);

    /** A map of grass whose rock noise rises with the index, so the highest scores are the last tiles. */
    private static GenerationContext rising() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);
        final var rock = context.defineField(NoiseField.ROCK);
        for (var index = 0; index < TILES; index++) {
            rock[index] = (float) index / TILES;
        }
        return context;
    }

    private static void apply(GenerationContext context, float share, float bias, int reach, Set<TileType> hosts) {
        new RockyZonePass(new RockyZoneParams(share, bias, reach, hosts)).apply(context);
    }

    @Test
    void theTopShareOfTheHostTilesByRockNoiseBecomesRockyGround() {
        final var context = rising();

        apply(context, QUARTER, 0F, 0, GRASS_ONLY);

        assertEquals(TILES * QUARTER, TestContexts.countGround(context, TileType.ROCKY_GROUND), TILES * TOLERANCE);
        assertEquals(TileType.ROCKY_GROUND, context.groundAt(TILES - 1));
        assertEquals(TileType.GRASS, context.groundAt(0));
    }

    @Test
    void onlyHostTilesChangeAndTheShareIsRelativeToThem() {
        final var context = rising();
        for (var index = 0; index < HALF_TILES; index++) {
            context.setGround(index, TileType.SAND);
        }

        apply(context, HALF, 0F, 0, GRASS_ONLY);

        for (var index = 0; index < HALF_TILES; index++) {
            assertEquals(TileType.SAND, context.groundAt(index));
        }
        assertEquals(HALF_TILES * HALF, TestContexts.countGround(context, TileType.ROCKY_GROUND), TILES * TOLERANCE);
        assertEquals(TileType.GRASS, context.groundAt(HALF_TILES));
    }

    @Test
    void aHeightBiasPullsTheZonesTowardsHighGround() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);
        TestContexts.fillField(context, NoiseField.ROCK, HALF);
        final var height = context.defineField(NoiseField.HEIGHT);
        for (var index = 0; index < TILES; index++) {
            height[index] = index % SIDE < HALF_SIDE ? -1F : 1F;
        }

        apply(context, HALF, 1F, 0, GRASS_ONLY);

        for (var index = 0; index < TILES; index++) {
            final var expected = index % SIDE < HALF_SIDE ? TileType.GRASS : TileType.ROCKY_GROUND;
            assertEquals(expected, context.groundAt(index), "tile " + index);
        }
    }

    @Test
    void theFootOfAMountainBecomesRockyGroundWithinTheReach() {
        final var context = rising();
        final var mountain = context.index(CENTRE, CENTRE);
        context.setGround(mountain, TileType.MOUNTAIN);

        apply(context, 0F, 0F, REACH, GRASS_ONLY);

        assertEquals(TileType.MOUNTAIN, context.groundAt(mountain));
        assertEquals(SQUARE_OF_REACH - 1, TestContexts.countGround(context, TileType.ROCKY_GROUND));
        assertEquals(TileType.ROCKY_GROUND, context.groundAt(context.index(CENTRE + REACH, CENTRE - REACH)));
        assertEquals(TileType.GRASS, context.groundAt(context.index(CENTRE + REACH + 1, CENTRE)));
    }

    @Test
    void aShareOfZeroAndNoReachLeaveTheMapAlone() {
        final var context = rising();

        apply(context, 0F, 0F, 0, GRASS_ONLY);

        assertEquals(TILES, TestContexts.countGround(context, TileType.GRASS));
    }

    @Test
    void aMapWithoutHostTilesIsLeftAlone() {
        final var context = rising();
        TestContexts.fillGround(context, TileType.DEEP_WATER);

        apply(context, HALF, 0F, 0, GRASS_ONLY);

        assertEquals(TILES, TestContexts.countGround(context, TileType.DEEP_WATER));
    }

    @Test
    void theSameInputGivesTheSameZones() {
        final var first = rising();
        final var second = rising();

        apply(first, QUARTER, 0F, 0, GRASS_ONLY);
        apply(second, QUARTER, 0F, 0, GRASS_ONLY);

        assertEquals(Arrays.asList(first.ground()), Arrays.asList(second.ground()));
        assertNotEquals(0, TestContexts.countGround(first, TileType.ROCKY_GROUND));
    }

    @Test
    void theParametersCopyTheHostsAndRefuseValuesOutOfRange() {
        final var hosts = EnumSet.of(TileType.GRASS);
        final var params = new RockyZoneParams(QUARTER, 0F, 0, hosts);
        hosts.add(TileType.SAND);

        assertEquals(Set.of(TileType.GRASS), params.hosts());
        assertNotSame(params.hosts(), params.hosts());
        assertTrue(new RockyZoneParams(QUARTER, 0F, 0, Set.of()).hosts().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new RockyZoneParams(-QUARTER, 0F, 0, GRASS_ONLY));
        assertThrows(IllegalArgumentException.class, () -> new RockyZoneParams(2F, 0F, 0, GRASS_ONLY));
        assertThrows(IllegalArgumentException.class, () -> new RockyZoneParams(Float.NaN, 0F, 0, GRASS_ONLY));
        assertThrows(IllegalArgumentException.class,
                () -> new RockyZoneParams(QUARTER, Float.POSITIVE_INFINITY, 0, GRASS_ONLY));
        assertThrows(IllegalArgumentException.class, () -> new RockyZoneParams(QUARTER, 0F, -1, GRASS_ONLY));
        assertThrows(NullPointerException.class, () -> new RockyZoneParams(QUARTER, 0F, 0, null));
        assertThrows(NullPointerException.class, () -> new RockyZonePass(null));
    }
}
