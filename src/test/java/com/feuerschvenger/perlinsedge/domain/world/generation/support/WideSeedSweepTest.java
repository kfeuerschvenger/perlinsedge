package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The five fixed seeds are few for the maps whose water decides the shape of the land, so the rivers, the islands and
 * the Mediterranean are also checked against a wide sweep of seeds that look random but are fixed, so a failure can be
 * reproduced. It is one slow test for each type: every seed generates a map of 300 by 300 tiles and runs the common
 * and the per type invariants on it.
 */
class WideSeedSweepTest {

    private static final int SIZE = 300;
    private static final int SEEDS = 30;
    /** The seed of the stream the sweep seeds are drawn from, the day the sweep was added. */
    private static final long SWEEP_SOURCE = 20_261_124L;

    /** Returns the seeds of the sweep, the same every time. */
    private static List<Long> sweepSeeds() {
        return FixedSeeds.sweep(SWEEP_SOURCE, SEEDS);
    }

    /**
     * Runs the common invariants and the ones of the type on a map of every seed of the sweep, and checks that the
     * sweep really generated a map for every one of its distinct seeds, so a sweep that shrinks cannot pass quietly.
     */
    @ParameterizedTest
    @EnumSource(value = MapType.class, names = {"RIVERS", "ISLANDS", "MEDITERRANEAN", "DENSE_FOREST", "MOUNTAINOUS"})
    void theInvariantsHoldOnEverySeedOfTheSweep(MapType type) {
        final var generated = new HashSet<Long>();
        final MapSource source = spec -> {
            generated.add(spec.seed());
            return MapSource.current().generate(spec);
        };
        final var runner = new InvariantRunner(source);
        final var invariants = new ArrayList<>(MapInvariants.common(source));
        invariants.addAll(MapInvariants.perType().get(type));
        assertFalse(invariants.isEmpty(), "the sweep checks at least one invariant");

        for (var seed : sweepSeeds()) {
            runner.run(new MapSpec(type, seed, SIZE), invariants);
        }

        final Set<Long> distinct = new HashSet<>(sweepSeeds());
        assertEquals(SEEDS, distinct.size(), "the seeds of the sweep are all different");
        assertEquals(distinct, generated, "a map was generated for every seed of the sweep");
    }
}
