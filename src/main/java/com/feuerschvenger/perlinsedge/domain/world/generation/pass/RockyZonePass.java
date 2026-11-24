package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * Turns zones of the ground into rocky ground, where stone, iron and crystals are placed afterwards. Every host tile is
 * scored by the rock noise plus the height bias times the height, and the top share of the host tiles becomes rocky
 * ground. Because the share is relative to the tiles that qualify, every map type gets rock whatever its height
 * profile. Host tiles within the foothill reach of a mountain become rocky ground too, so the feet of a mountain are
 * rock the player can walk on.
 * <p>
 * The pass reads fields and ground only and draws no random numbers, so it repeats for a seed.
 */
public final class RockyZonePass implements GenerationPass {

    private static final String NAME = "rocky-zones";

    private final RockyZoneParams params;
    private final boolean[] isHost;

    /**
     * Creates the pass.
     *
     * @param params the share of rock, how much height matters, the reach of the foothills and the host terrains
     */
    public RockyZonePass(RockyZoneParams params) {
        this.params = Objects.requireNonNull(params, "params");
        this.isHost = new boolean[TileType.values().length];
        for (final var type : params.hosts()) {
            isHost[type.ordinal()] = true;
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final IntPredicate hostMask = index -> isHost[context.groundAt(index).ordinal()];
        final var score = scoreOf(context);
        final var threshold = FieldQuantile.above(score, hostMask, params.landShare());
        final var distances = params.foothillReach() > 0 ? distanceToMountains(context) : null;
        for (var index = 0; index < context.size(); index++) {
            if (hostMask.test(index) && (score[index] >= threshold || isFoothill(distances, index))) {
                context.setGround(index, TileType.ROCKY_GROUND);
            }
        }
    }

    /** Returns the rock noise, or the rock noise plus the weighted height when the bias is not zero. */
    private float[] scoreOf(GenerationContext context) {
        final var rock = context.field(NoiseField.ROCK);
        if (Float.compare(params.heightBias(), 0F) == 0) {
            return rock;
        }
        final var height = context.field(NoiseField.HEIGHT);
        final var score = new float[rock.length];
        for (var index = 0; index < score.length; index++) {
            score[index] = rock[index] + params.heightBias() * height[index];
        }
        return score;
    }

    /** The distance of every tile to the nearest mountain. */
    private static int[] distanceToMountains(GenerationContext context) {
        return DistanceTransform.chebyshev(context.width(), context.height(),
                index -> context.groundAt(index) == TileType.MOUNTAIN);
    }

    private boolean isFoothill(int[] distances, int index) {
        return distances != null && distances[index] <= params.foothillReach();
    }
}
