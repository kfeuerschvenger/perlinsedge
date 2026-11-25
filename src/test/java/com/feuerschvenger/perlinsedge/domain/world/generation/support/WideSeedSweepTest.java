package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The five fixed seeds are few for the maps whose water decides the shape of the land, so the rivers and the islands
 * are also checked against a wide sweep of seeds that look random but are fixed, so a failure can be reproduced. It is
 * one slow test for each type: every seed generates a map of 300 by 300 tiles and runs the common and the per type
 * invariants on it.
 */
class WideSeedSweepTest {

    private static final int SIZE = 300;
    private static final int SEEDS = 30;
    /** The seed of the stream the sweep seeds are drawn from, the day the sweep was added. */
    private static final long SWEEP_SOURCE = 20_261_124L;

    /** Returns the seeds of the sweep, the same every time. */
    private static List<Long> sweepSeeds() {
        final var random = new SplittableRandom(SWEEP_SOURCE);
        final var seeds = new ArrayList<Long>();
        for (var i = 0; i < SEEDS; i++) {
            seeds.add(random.nextLong());
        }
        return seeds;
    }

    @ParameterizedTest
    @EnumSource(value = MapType.class, names = {"RIVERS", "ISLANDS"})
    void theInvariantsHoldOnEverySeedOfTheSweep(MapType type) {
        final var source = MapSource.current();
        final var runner = new InvariantRunner(source);
        final var invariants = new ArrayList<>(MapInvariants.common(source));
        invariants.addAll(MapInvariants.perType().get(type));

        var checked = 0;
        for (var seed : sweepSeeds()) {
            checked += runner.run(new MapSpec(type, seed, SIZE), invariants);
        }

        assertEquals(SEEDS * invariants.size(), checked);
    }
}
