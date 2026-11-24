package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Brings the count of each resource into its range, so every map has the iron, crystals, stone and trees the game needs
 * whatever its terrain. For each quota in turn, a resource above its maximum is thinned by a seeded shuffle that picks
 * the tiles to clear, and a resource below its minimum is added: rocky resources go on the free tiles of rocky ground,
 * zone by zone from the largest, and trees on the free fertile tiles where the tree noise is highest. Reserved tiles are
 * never touched by the adding.
 * <p>
 * The counts it leaves are final except when the spawn pass has to clear the area around the player. It runs on plain
 * arrays and draws from one seeded stream, so it repeats for a seed.
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
        final var fertile = new FertileFill(context, scratch, random);
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
            fertile.add(quota.type(), missing);
        }
    }
}
