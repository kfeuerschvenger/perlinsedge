package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.function.IntPredicate;
import java.util.random.RandomGenerator;

/**
 * Adds a resource to the free fertile tiles where the tree noise is highest, so new trees thicken the groves that
 * already stand. The share of candidate tiles that passes the threshold starts at what is missing and widens until
 * enough tiles pass, then a random choice of the passing tiles gets the resource.
 */
final class FertileFill {

    private static final double GROWTH_FACTOR = 2.0;

    private final GenerationContext context;
    private final int[] buffer;
    private final RandomGenerator random;

    FertileFill(GenerationContext context, int[] buffer, RandomGenerator random) {
        this.context = context;
        this.buffer = buffer;
        this.random = random;
    }

    /**
     * Puts the resource on up to {@code missing} free fertile tiles.
     *
     * @param type    the resource
     * @param missing how many tiles to add
     */
    void add(ResourceType type, int missing) {
        final var tree = context.field(NoiseField.TREE);
        final IntPredicate candidate = this::isCandidate;
        var candidates = 0;
        for (var index = 0; index < context.size(); index++) {
            candidates += candidate.test(index) ? 1 : 0;
        }
        if (candidates == 0) {
            return;
        }
        var share = Math.min(1.0, (double) missing / candidates);
        var found = collect(tree, candidate, FieldQuantile.above(tree, candidate, share));
        while (found < missing && share < 1.0) {
            share = Math.min(1.0, share * GROWTH_FACTOR);
            found = collect(tree, candidate, FieldQuantile.above(tree, candidate, share));
        }
        final var take = Math.min(missing, found);
        Shuffles.partial(buffer, found, take, random);
        for (var position = 0; position < take; position++) {
            context.setResource(buffer[position], type);
        }
    }

    private boolean isCandidate(int index) {
        return context.resourceAt(index) == null && !context.isReserved(index) && context.groundAt(index).isFertile();
    }

    /** Writes the candidate tiles at or above the threshold into the buffer and returns how many. */
    private int collect(float[] tree, IntPredicate candidate, float threshold) {
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
