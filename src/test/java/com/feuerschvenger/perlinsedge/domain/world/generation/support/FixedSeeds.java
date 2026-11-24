package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import java.util.List;

/** The seeds every map test and preview shares, so a result seen in one place can be reproduced in another. */
public final class FixedSeeds {

    /** The five seeds used since the first generator tests. */
    public static final List<Long> ALL = List.of(1L, 2L, 3L, 20_261_111L, 987_654_321L);

    private FixedSeeds() {
    }
}
