package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Chooses the tile where the player starts, and runs before the passes that balance the resources and floor the
 * forest, so that they leave the area around the player alone. The free tiles, walkable ground that holds no resource,
 * are labelled into 4-connected regions, and the spawn is taken in the largest one so the player is never shut into a
 * pocket. When a pass left a spawn hint in the context, the region is the largest one inside the piece of land that
 * holds the hint, the walkable ground with resources ignored: on a sea of islands the hint lies on the largest island,
 * and the spawn stays there however the trees and stones have cut the island, instead of moving to a larger free
 * region of a smaller island. Without a hint the region is the largest one of the map. Within the region the spawn is
 * the tile nearest to the anchor, which is the spawn hint, such as the middle of a clearing, or the centre of the map,
 * whose surroundings are free of resources and walkable to the free radius of the parameters.
 * <p>
 * When no tile of the region has such surroundings, the tile of the region nearest to the anchor is taken and the
 * resources around it are cleared, never the terrain, so the area around the spawn is always free of resources. In
 * both cases the square of the free radius around the spawn is reserved, so no later pass puts a resource there or
 * floors it, and the area stays free in the final map whatever the balancing pass has to add elsewhere.
 * <p>
 * The pass labels the map with {@link Regions}, which creates no object per tile.
 */
public final class SpawnPass implements GenerationPass {

    private static final String NAME = "spawn";
    private static final int CENTER_DIVISOR = 2;

    private final SpawnParams params;

    /**
     * Creates the pass.
     *
     * @param params how large the free area around the spawn is
     */
    public SpawnPass(SpawnParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    /** Returns the name the timings report this pass under. */
    @Override
    public String name() {
        return NAME;
    }

    /** Picks the region, finds the spawn in it and reserves the area around it, clearing the resources if it must. */
    @Override
    public void apply(GenerationContext context) {
        final var regions = Regions.label(context.width(), context.height(), index -> isFree(context, index));
        if (regions.largest() == Regions.NO_LABEL) {
            throw new IllegalStateException(
                    "the map has no walkable tile free of resources for the player to start on");
        }
        final var anchor = anchorOf(context);
        final var region = regionOf(context, regions);
        var spawn = nearest(context, regions, region, anchor, true);
        if (spawn == GenerationContext.NONE) {
            spawn = nearest(context, regions, region, anchor, false);
            clearAround(context, spawn);
        }
        reserveAround(context, spawn);
        context.setSpawnIndex(spawn);
    }

    /**
     * Returns the free region the spawn is taken in: the largest one inside the piece of land of the spawn hint when
     * a pass left one on walkable ground, else the largest one of the map.
     */
    private static int regionOf(GenerationContext context, Regions regions) {
        if (context.spawnHint() == GenerationContext.NONE) {
            return regions.largest();
        }
        final var land = Regions.label(context.width(), context.height(),
                index -> context.groundAt(index).isWalkable());
        final var hinted = land.labelAt(context.spawnHint());
        if (hinted == Regions.NO_LABEL) {
            return regions.largest();
        }
        var best = Regions.NO_LABEL;
        for (var index = 0; index < context.size(); index++) {
            final var label = regions.labelAt(index);
            if (label != Regions.NO_LABEL && land.labelAt(index) == hinted
                    && (best == Regions.NO_LABEL || regions.sizeOf(label) > regions.sizeOf(best))) {
                best = label;
            }
        }
        return best == Regions.NO_LABEL ? regions.largest() : best;
    }

    /** Returns the tile the spawn should be near: the hint of a pass, or else the centre of the map. */
    private static int anchorOf(GenerationContext context) {
        return context.spawnHint() == GenerationContext.NONE
                ? context.index(context.width() / CENTER_DIVISOR, context.height() / CENTER_DIVISOR)
                : context.spawnHint();
    }

    /** Tells whether a tile is walkable ground with no resource on it. */
    private static boolean isFree(GenerationContext context, int index) {
        return context.groundAt(index).isWalkable() && context.resourceAt(index) == null;
    }

    /**
     * Finds the tile of a region nearest to the anchor, the first one in row order when several are equally near.
     *
     * @param needsFreeRadius whether the tile must have free surroundings to the free radius
     * @return the index, or {@link GenerationContext#NONE} when no tile of the region qualifies
     */
    private int nearest(GenerationContext context, Regions regions, int label, int anchor, boolean needsFreeRadius) {
        final var anchorX = context.xOf(anchor);
        final var anchorY = context.yOf(anchor);
        var best = GenerationContext.NONE;
        var bestDistance = Long.MAX_VALUE;
        for (var y = 0; y < context.height(); y++) {
            final long dy = (long) y - anchorY;
            for (var x = 0; x < context.width(); x++) {
                final var index = context.index(x, y);
                final long dx = (long) x - anchorX;
                final var distance = dx * dx + dy * dy;
                if (regions.labelAt(index) == label && distance < bestDistance
                        && (!needsFreeRadius || hasFreeSurroundings(context, index))) {
                    best = index;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    /** Tells whether the whole square around a tile lies on the map and is walkable ground without a resource. */
    private boolean hasFreeSurroundings(GenerationContext context, int index) {
        final var radius = params.freeRadius();
        final var x = context.xOf(index);
        final var y = context.yOf(index);
        if (x < radius || y < radius || x + radius >= context.width() || y + radius >= context.height()) {
            return false;
        }
        for (var dy = -radius; dy <= radius; dy++) {
            for (var dx = -radius; dx <= radius; dx++) {
                if (!isFree(context, context.index(x + dx, y + dy))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Removes the resources in the square around a tile, as far as it lies on the map. */
    private void clearAround(GenerationContext context, int index) {
        forEachAround(context, index, tile -> context.setResource(tile, null));
    }

    /** Reserves the square around a tile, as far as it lies on the map. */
    private void reserveAround(GenerationContext context, int index) {
        forEachAround(context, index, context::reserve);
    }

    /** Visits the tiles of the square of the free radius around a tile, as far as it lies on the map. */
    private void forEachAround(GenerationContext context, int index, IntConsumer action) {
        final var radius = params.freeRadius();
        final var x = context.xOf(index);
        final var y = context.yOf(index);
        final var lastY = Math.min(context.height() - 1, y + radius);
        final var lastX = Math.min(context.width() - 1, x + radius);
        for (var ny = Math.max(0, y - radius); ny <= lastY; ny++) {
            for (var nx = Math.max(0, x - radius); nx <= lastX; nx++) {
                action.accept(context.index(nx, ny));
            }
        }
    }
}
