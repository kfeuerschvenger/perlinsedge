package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.function.IntPredicate;
import java.util.random.RandomGenerator;

/**
 * Adds a resource to the free tiles of a host terrain where the tree noise is highest, so new trees thicken the groves
 * that already stand. The share of candidate tiles that passes the threshold starts at what is missing and widens until
 * enough tiles pass, then a random choice of the passing tiles gets the resource. When the tree noise is the same on
 * every candidate tile, no tile is above another and the share widens to every candidate, so the random choice alone
 * decides.
 * <p>
 * The fill works in a buffer its caller lends it for each add, one slot per tile of the map, so the pass that runs it
 * allocates one buffer for its own scans and for this fill instead of two. The caller must not rely on what the buffer
 * holds after an add.
 */
final class FertileFill {

    private static final double GROWTH_FACTOR = 2.0;

    private final GenerationContext context;
    private final RandomGenerator random;

    FertileFill(GenerationContext context, RandomGenerator random) {
        this.context = context;
        this.random = random;
    }

    /**
     * Puts the resource on up to {@code missing} free tiles of the host terrain.
     *
     * @param type    the resource
     * @param missing how many tiles to add
     * @param host    the terrain that can hold the resource
     * @param buffer  the scratch space, with one slot per tile of the map, which the fill overwrites
     * @throws IllegalArgumentException when the buffer is shorter than the map has tiles
     */
    void add(ResourceType type, int missing, ResourceQuota.Host host, int[] buffer) {
        if (buffer.length < context.size()) {
            throw new IllegalArgumentException(
                    "the buffer has " + buffer.length + " slots for a map of " + context.size() + " tiles");
        }
        final IntPredicate candidate = index -> isCandidate(index, host);
        var candidates = 0;
        for (var index = 0; index < context.size(); index++) {
            candidates += candidate.test(index) ? 1 : 0;
        }
        if (candidates == 0) {
            return;
        }
        final var tree = context.field(NoiseField.TREE);
        var share = Math.min(1.0, (double) missing / candidates);
        var found = collect(tree, candidate, FieldQuantile.above(tree, candidate, share), buffer);
        while (found < missing && share < 1.0) {
            share = Math.min(1.0, share * GROWTH_FACTOR);
            found = collect(tree, candidate, FieldQuantile.above(tree, candidate, share), buffer);
        }
        final var take = Math.min(missing, found);
        Shuffles.partial(buffer, found, take, random);
        for (var position = 0; position < take; position++) {
            context.setResource(buffer[position], type);
        }
    }

    private boolean isCandidate(int index, ResourceQuota.Host host) {
        return context.resourceAt(index) == null && !context.isReserved(index)
                && host.accepts(context.groundAt(index));
    }

    /** Writes the candidate tiles at or above the threshold into the buffer and returns how many. */
    private static int collect(float[] tree, IntPredicate candidate, float threshold, int[] buffer) {
        var found = 0;
        for (var index = 0; index < tree.length; index++) {
            if (tree[index] >= threshold && candidate.test(index)) {
                buffer[found] = index;
                found++;
            }
        }
        return found;
    }
}
