package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Arrays;
import java.util.Objects;

/**
 * Keeps a map to a few bodies of water. The 4-connected bodies of standing water, which are every water terrain except
 * ice and rivers, are labelled, and only the largest ones survive: a body that is smaller than the minimum, or that is
 * not among the largest {@link #keepCount(int)} bodies, is filled with land, so a map keeps at most that many bodies.
 * The desert and the Mediterranean lose their ponds this way, and the rivers map loses its puddles before its rivers
 * are cut.
 * <p>
 * A river is not a body: the rivers are carved along 8-connected paths, so a river is split into many 4-connected
 * pieces, and a pass that counted them would cut a river into bodies and fill most of it. The pass neither counts nor
 * removes a river tile, so it may run after the rivers without touching them.
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

    /** Returns the name the timings report this pass under. */
    @Override
    public String name() {
        return NAME;
    }

    /** Fills with land every body of water that is too small or not among the largest ones the map may keep. */
    @Override
    public void apply(GenerationContext context) {
        final var bodies = Regions.label(context.width(), context.height(),
                index -> isStandingWater(context.groundAt(index)));
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

    /** Tells whether a terrain is water that belongs to a body: any water but a river. */
    private static boolean isStandingWater(TileType type) {
        return type.isWater() && type != TileType.RIVER;
    }

    /**
     * Returns the most bodies a map of a number of tiles may keep: the density times the area, rounded, and at least
     * one unless the density is 0. A body that is among the largest of that many but has fewer tiles than the minimum
     * is filled all the same, so a map may keep fewer bodies than this, or none.
     *
     * @param tiles the number of tiles of the map
     * @return the most bodies the map keeps
     */
    int keepCount(int tiles) {
        if (params.keepPer100k() == 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round((double) tiles * params.keepPer100k() / DENSITY_AREA_TILES));
    }

    /** Marks, by label, the largest bodies that reach the minimum size, at most as many as the map may keep. */
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
