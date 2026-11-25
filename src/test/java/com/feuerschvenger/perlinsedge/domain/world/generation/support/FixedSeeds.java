package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/** The seeds every map test and preview shares, so a result seen in one place can be reproduced in another. */
public final class FixedSeeds {

    /** The five seeds used since the first generator tests. */
    public static final List<Long> ALL = List.of(1L, 2L, 3L, 20_261_111L, 987_654_321L);

    private FixedSeeds() {
    }

    /**
     * Returns seeds that look random but are fixed, so a sweep over them can be reproduced.
     *
     * @param source the seed of the stream the seeds are drawn from
     * @param count  how many seeds
     * @return a new list of seeds, the same for the same source and count
     */
    public static List<Long> sweep(long source, int count) {
        final var random = new SplittableRandom(source);
        final var seeds = new ArrayList<Long>();
        for (var i = 0; i < count; i++) {
            seeds.add(random.nextLong());
        }
        return seeds;
    }
}
