package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;

import java.util.Objects;

/**
 * Chooses the tile where the player starts, the last step of every recipe. The free tiles, walkable ground that holds no
 * resource, are labelled into 4-connected regions, and the spawn is taken in the largest one so the player is never
 * shut into a pocket; on a sea of islands that region is the largest island. Within it the spawn is the tile nearest to
 * the anchor, which is the spawn hint a pass left in the context, such as the middle of a clearing, or the centre of
 * the map, whose surroundings are free of resources and walkable to the free radius of the parameters.
 * <p>
 * When no tile of the region has such surroundings, the tile of the region nearest to the anchor is taken and the
 * resources around it are cleared, never the terrain, so the area around the spawn is always free of resources.
 * <p>
 * The pass labels the map with one flood fill over an array of labels and an array used as the queue, and creates no
 * object per tile.
 */
public final class SpawnPass implements GenerationPass {

    private static final String NAME = "spawn";
    private static final int NO_LABEL = 0;
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

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var labels = new int[context.size()];
        final var largest = labelRegions(context, labels);
        if (largest == NO_LABEL) {
            throw new IllegalStateException("the map has no walkable tile free of resources for the player to start on");
        }
        final var anchor = anchorOf(context);
        var spawn = nearest(context, labels, largest, anchor, true);
        if (spawn == GenerationContext.NONE) {
            spawn = nearest(context, labels, largest, anchor, false);
            clearAround(context, spawn);
        }
        context.setSpawnIndex(spawn);
    }

    /** Returns the tile the spawn should be near: the hint of a pass, or else the centre of the map. */
    private static int anchorOf(GenerationContext context) {
        return context.spawnHint() != GenerationContext.NONE
                ? context.spawnHint()
                : context.index(context.width() / CENTER_DIVISOR, context.height() / CENTER_DIVISOR);
    }

    /** Tells whether a tile is walkable ground with no resource on it. */
    private static boolean isFree(GenerationContext context, int index) {
        return context.groundAt(index).isWalkable() && context.resourceAt(index) == null;
    }

    /**
     * Gives every free tile the number of its region, counting from 1, and returns the number of the largest region.
     * The first of several regions of the same size wins, so the answer is stable.
     *
     * @return the label of the largest region, or {@code NO_LABEL} when no tile is free
     */
    private static int labelRegions(GenerationContext context, int[] labels) {
        final var queue = new int[labels.length];
        var label = NO_LABEL;
        var largest = NO_LABEL;
        var largestSize = 0;
        for (var index = 0; index < labels.length; index++) {
            if (labels[index] == NO_LABEL && isFree(context, index)) {
                label++;
                final var size = flood(context, labels, queue, index, label);
                if (size > largestSize) {
                    largestSize = size;
                    largest = label;
                }
            }
        }
        return largest;
    }

    /** Labels every free tile connected to a start tile along the four directions, and returns how many it labelled. */
    private static int flood(GenerationContext context, int[] labels, int[] queue, int start, int label) {
        final var width = context.width();
        var head = 0;
        var tail = 0;
        labels[start] = label;
        queue[tail++] = start;
        while (head < tail) {
            final var index = queue[head++];
            final var x = context.xOf(index);
            final var y = context.yOf(index);
            tail = visit(context, labels, queue, tail, x > 0, index - 1, label);
            tail = visit(context, labels, queue, tail, x < width - 1, index + 1, label);
            tail = visit(context, labels, queue, tail, y > 0, index - width, label);
            tail = visit(context, labels, queue, tail, y < context.height() - 1, index + width, label);
        }
        return tail;
    }

    /** Queues a neighbour when it exists, is free and has no label yet, and returns the new end of the queue. */
    private static int visit(GenerationContext context, int[] labels, int[] queue, int tail, boolean exists,
                             int neighbour, int label) {
        if (!exists || labels[neighbour] != NO_LABEL || !isFree(context, neighbour)) {
            return tail;
        }
        labels[neighbour] = label;
        queue[tail] = neighbour;
        return tail + 1;
    }

    /**
     * Finds the tile of a region nearest to the anchor, the first one in row order when several are equally near.
     *
     * @param needsFreeRadius whether the tile must have free surroundings to the free radius
     * @return the index, or {@link GenerationContext#NONE} when no tile of the region qualifies
     */
    private int nearest(GenerationContext context, int[] labels, int label, int anchor, boolean needsFreeRadius) {
        final var anchorX = context.xOf(anchor);
        final var anchorY = context.yOf(anchor);
        var best = GenerationContext.NONE;
        var bestDistance = Long.MAX_VALUE;
        for (var index = 0; index < labels.length; index++) {
            if (labels[index] != label) {
                continue;
            }
            final long dx = context.xOf(index) - anchorX;
            final long dy = context.yOf(index) - anchorY;
            final var distance = dx * dx + dy * dy;
            if (distance < bestDistance && (!needsFreeRadius || hasFreeSurroundings(context, index))) {
                best = index;
                bestDistance = distance;
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
        final var radius = params.freeRadius();
        final var x = context.xOf(index);
        final var y = context.yOf(index);
        for (var ny = Math.max(0, y - radius); ny <= Math.min(context.height() - 1, y + radius); ny++) {
            for (var nx = Math.max(0, x - radius); nx <= Math.min(context.width() - 1, x + radius); nx++) {
                context.setResource(context.index(nx, ny), null);
            }
        }
    }
}
