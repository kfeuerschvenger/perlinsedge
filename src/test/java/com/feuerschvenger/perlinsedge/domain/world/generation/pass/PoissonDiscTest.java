package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.SiteList;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoissonDiscTest {

    private static final int SIDE = 60;
    private static final int SPACING = 8;
    private static final long SEED = 11L;
    private static final long OTHER_SEED = 12L;
    private static final int MIN_SITES = 20;
    private static final int STRIP_HEIGHT = 10;
    private static final int STRIP_GAP_ROW = 20;
    private static final int BIG_SIDE = 300;
    private static final int BIG_SPACING = 45;
    private static final int HALF = 2;

    private static SiteList sample(int width, int height, Mask host, int spacing, long seed) {
        return PoissonDisc.sample(width, height, host, spacing, new SplittableRandom(seed));
    }

    private static long squaredDistance(int width, int first, int second) {
        final long dx = first % width - second % width;
        final long dy = first / width - second / width;
        return dx * dx + dy * dy;
    }

    @Test
    void noTwoSitesAreCloserThanTheSpacing() {
        final var sites = sample(SIDE, SIDE, index -> true, SPACING, SEED);

        assertTrue(sites.size() > MIN_SITES, "sites " + sites.size());
        for (var first = 0; first < sites.size(); first++) {
            for (var second = first + 1; second < sites.size(); second++) {
                assertTrue(squaredDistance(SIDE, sites.get(first), sites.get(second)) >= SPACING * SPACING,
                        first + " and " + second);
            }
        }
    }

    @Test
    void noHostTileIsLeftWithoutASiteWithinTheSpacing() {
        final var sites = sample(SIDE, SIDE, index -> true, SPACING, SEED);

        for (var tile = 0; tile < SIDE * SIDE; tile++) {
            var near = false;
            for (var position = 0; position < sites.size() && !near; position++) {
                near = squaredDistance(SIDE, tile, sites.get(position)) < SPACING * SPACING;
            }
            assertTrue(near, "tile " + tile + " has no site within the spacing");
        }
    }

    @Test
    void sitesStandOnHostTilesOnly() {
        final Mask leftHalf = index -> index % SIDE < SIDE / HALF;

        final var sites = sample(SIDE, SIDE, leftHalf, SPACING, SEED);

        assertTrue(sites.size() > 0);
        for (var position = 0; position < sites.size(); position++) {
            assertTrue(leftHalf.test(sites.get(position)));
        }
    }

    @Test
    void separateHostAreasEachGetSites() {
        final Mask strips = index -> index / SIDE < STRIP_HEIGHT || index / SIDE >= STRIP_GAP_ROW + STRIP_HEIGHT;

        final var sites = sample(SIDE, SIDE, strips, SPACING, SEED);

        var top = 0;
        var bottom = 0;
        for (var position = 0; position < sites.size(); position++) {
            if (sites.get(position) / SIDE < STRIP_HEIGHT) {
                top++;
            } else {
                bottom++;
            }
        }
        assertTrue(top > 0 && bottom > 0, "top " + top + ", bottom " + bottom);
    }

    @Test
    void theSameSeedGivesTheSameSitesAndAnotherSeedGivesOthers() {
        final var first = sample(SIDE, SIDE, index -> true, SPACING, SEED);
        final var again = sample(SIDE, SIDE, index -> true, SPACING, SEED);
        final var other = sample(SIDE, SIDE, index -> true, SPACING, OTHER_SEED);

        assertArrayEquals(first.toArray(), again.toArray());
        assertFalse(Arrays.equals(first.toArray(), other.toArray()));
    }

    @Test
    void aSpacingOfOneMakesEveryHostTileASite() {
        final var sites = sample(HALF * HALF, HALF * HALF, index -> true, 1, SEED);

        assertEquals(HALF * HALF * HALF * HALF, sites.size());
    }

    @Test
    void noHostTileGivesNoSite() {
        assertEquals(0, sample(SIDE, SIDE, index -> false, SPACING, SEED).size());
    }

    @Test
    void aLargeMapWithALargeSpacingGivesAFewDozenSites() {
        final var sites = sample(BIG_SIDE, BIG_SIDE, index -> true, BIG_SPACING, SEED);

        assertTrue(sites.size() >= MIN_SITES && sites.size() < BIG_SIDE, "sites " + sites.size());
    }

    @Test
    void invalidArgumentsAreRefused() {
        final var random = new SplittableRandom(SEED);

        assertThrows(NullPointerException.class, () -> PoissonDisc.sample(SIDE, SIDE, null, SPACING, random));
        assertThrows(NullPointerException.class, () -> PoissonDisc.sample(SIDE, SIDE, index -> true, SPACING, null));
        assertThrows(IllegalArgumentException.class,
                () -> PoissonDisc.sample(0, SIDE, index -> true, SPACING, random));
        assertThrows(IllegalArgumentException.class,
                () -> PoissonDisc.sample(SIDE, 0, index -> true, SPACING, random));
        assertThrows(IllegalArgumentException.class, () -> PoissonDisc.sample(SIDE, SIDE, index -> true, 0, random));
    }
}
