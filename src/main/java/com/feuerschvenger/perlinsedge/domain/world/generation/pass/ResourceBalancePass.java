package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

/**
 * Brings the count of each resource into its range, so every map has the iron, crystals, stone and trees the game needs
 * whatever its terrain. For each quota in turn, a resource above its maximum is thinned by a seeded shuffle that picks
 * the tiles to clear, and a resource below its minimum is added: rocky resources go on the free tiles of rocky ground,
 * zone by zone from the largest, and trees on the free tiles of their host terrain where the tree noise is highest.
 * Reserved tiles are never touched by the adding.
 * <p>
 * A minimum is met up to the host tiles available: when the free tiles of the host terrain run out before the
 * resource does, the pass adds what fits, throws nothing and leaves the count short. {@link #shortfalls} reports by how
 * much, so a caller can tell a quota that was met from one the map could not hold.
 * <p>
 * The counts it leaves are final, because the spawn pass runs before it and reserves the area around the player. It
 * runs on plain arrays and draws from one seeded stream, so it repeats for a seed.
 */
public final class ResourceBalancePass implements GenerationPass {

    private static final String NAME = "balance";
    private static final long SALT = 0xBA1A_0CE0_5EED_1234L;

    private final BalanceParams params;

    /**
     * Creates the pass.
     *
     * @param params the quotas to apply
     */
    public ResourceBalancePass(BalanceParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var random = context.random(SALT);
        final var scratch = new int[context.size()];
        final var rocky = new RockyFill(context, random);
        final var fertile = new FertileFill(context, random);
        for (final var quota : params.quotas()) {
            final var found = collect(context, quota.type(), scratch);
            final var max = quota.maxFor(context.size());
            final var min = quota.minFor(context.size());
            if (found > max) {
                thin(context, scratch, found, found - max, random);
            } else if (found < min) {
                add(quota, min - found, rocky, fertile);
            }
        }
    }

    /**
     * Reports which minimums the map does not reach, which is what is left after the pass when the host tiles ran out.
     * It only reads the context, so it can be asked of any map, before or after the pass.
     *
     * @param context the generation to measure
     * @return the missing tiles of each resource that is below its minimum, in a new map with no entry for a resource
     * that meets it, so an empty map means every minimum is met
     */
    public Map<ResourceType, Integer> shortfalls(GenerationContext context) {
        return shortfalls(type -> count(context, type), context.size());
    }

    /**
     * Reports which minimums a map does not reach, from the counts of its resources.
     *
     * @param countOf the number of tiles of a resource on the map
     * @param area    the number of tiles of the map
     * @return the missing tiles of each resource that is below its minimum, in a new map with no entry for a resource
     * that meets it, so an empty map means every minimum is met
     */
    public Map<ResourceType, Integer> shortfalls(ToIntFunction<ResourceType> countOf, int area) {
        final var missing = new EnumMap<ResourceType, Integer>(ResourceType.class);
        for (final var quota : params.quotas()) {
            final var gap = quota.minFor(area) - countOf.applyAsInt(quota.type());
            if (gap > 0) {
                missing.put(quota.type(), gap);
            }
        }
        return missing;
    }

    private static int count(GenerationContext context, ResourceType type) {
        var found = 0;
        for (var index = 0; index < context.size(); index++) {
            found += context.resourceAt(index) == type ? 1 : 0;
        }
        return found;
    }

    /** Writes the indices of the tiles that hold a resource into the buffer and returns how many. */
    private static int collect(GenerationContext context, ResourceType type, int[] buffer) {
        var found = 0;
        for (var index = 0; index < context.size(); index++) {
            if (context.resourceAt(index) == type) {
                buffer[found] = index;
                found++;
            }
        }
        return found;
    }

    /** Clears a random choice of the collected tiles. */
    private static void thin(GenerationContext context, int[] found, int length, int count, RandomGenerator random) {
        Shuffles.partial(found, length, count, random);
        for (var position = 0; position < count; position++) {
            context.setResource(found[position], null);
        }
    }

    private static void add(ResourceQuota quota, int missing, RockyFill rocky, FertileFill fertile) {
        if (quota.host() == ResourceQuota.Host.ROCKY) {
            rocky.add(quota, missing);
        } else {
            fertile.add(quota.type(), missing, quota.host());
        }
    }
}
