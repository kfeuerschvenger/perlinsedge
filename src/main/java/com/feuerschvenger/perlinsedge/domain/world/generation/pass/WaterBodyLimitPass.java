package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;

import java.util.Arrays;
import java.util.Objects;

/**
 * Keeps a map to a few bodies of water. The 4-connected bodies of water, which are every water terrain except ice, are
 * labelled, and only the largest ones survive: a body that is smaller than the minimum, or that is not among the
 * largest {@code keep} bodies, is filled with land. The desert and the Mediterranean lose their ponds this way, and the
 * rivers map loses its puddles before its rivers are cut.
 * <p>
 * The pass reads the ground only, draws no random numbers and creates no object per tile, so it repeats for a seed.
 */
public final class WaterBodyLimitPass implements GenerationPass {

    private static final String NAME = "water-body-limit";
    /** The area, in tiles, that {@code keepPer100k} counts bodies in. */
    private static final double DENSITY_AREA_TILES = 100_000.0;
    private static final int LABEL_BITS = 32;
    private static final long LABEL_MASK = 0xFFFF_FFFFL;

    private final WaterBodyParams params;

    /**
     * Creates the pass.
     *
     * @param params how many bodies stay, how big they have to be and what replaces the others
     */
    public WaterBodyLimitPass(WaterBodyParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var bodies = Regions.label(context.width(), context.height(),
                index -> context.groundAt(index).isWater());
        if (bodies.count() == 0) {
            return;
        }
        final var kept = keptBodies(bodies, keepCount(context.size()));
        for (var index = 0; index < context.size(); index++) {
            final var label = bodies.labelAt(index);
            if (label != Regions.NO_LABEL && !kept[label]) {
                context.setGround(index, params.fill());
            }
        }
    }

    /**
     * Returns how many bodies a map of a number of tiles keeps: the density times the area, rounded, and at least one
     * unless the density is 0.
     *
     * @param tiles the number of tiles of the map
     * @return the number of bodies kept
     */
    int keepCount(int tiles) {
        if (params.keepPer100k() == 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round((double) tiles * params.keepPer100k() / DENSITY_AREA_TILES));
    }

    /** Marks, by label, the largest bodies that reach the minimum size, as many as the map keeps. */
    private boolean[] keptBodies(Regions bodies, int keep) {
        final var kept = new boolean[bodies.count() + 1];
        final var bySize = new long[bodies.count()];
        for (var label = 1; label <= bodies.count(); label++) {
            bySize[label - 1] = ((long) bodies.sizeOf(label) << LABEL_BITS) | label;
        }
        Arrays.sort(bySize);
        var taken = 0;
        for (var position = bySize.length - 1; position >= 0 && taken < keep; position--) {
            final var size = (int) (bySize[position] >>> LABEL_BITS);
            if (size >= params.minKeptTiles()) {
                kept[(int) (bySize[position] & LABEL_MASK)] = true;
                taken++;
            }
        }
        return kept;
    }
}
