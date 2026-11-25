package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;

/**
 * Grows trees on fertile ground that holds no resource yet, in the groves where the tree noise is high. The density is
 * the share of the noise range that grows a tree: a tree grows where the noise exceeds one minus the density. Within
 * the water reach of a river the threshold drops by the water bonus, so the trees stand thicker along the water.
 * Reserved tiles grow none.
 * <p>
 * A density can also be a share of the land instead of a share of the noise range, as {@link VegetationParams} says:
 * the trees then cover that share of the land, on the tiles where the noise is highest, whatever the noise looks like
 * and however much of the land the passes before this one reserved.
 */
public final class VegetationPass implements GenerationPass {

    private static final String NAME = "vegetation";

    private final VegetationParams params;

    /**
     * Creates the pass.
     *
     * @param params how thickly the trees grow
     */
    public VegetationPass(VegetationParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    /** Returns the name the timings report this pass under. */
    @Override
    public String name() {
        return NAME;
    }

    /** Puts a tree on every tile that may grow one where the tree noise is above the threshold of its place. */
    @Override
    public void apply(GenerationContext context) {
        final var treeNoise = context.field(NoiseField.TREE);
        final var threshold = thresholdOf(context, treeNoise);
        final var nearWaterThreshold = threshold - params.waterBonus();
        final var reach = params.waterReach() > 0 ? distanceToRivers(context) : null;
        for (var index = 0; index < context.size(); index++) {
            if (canGrow(context, index)
                    && treeNoise[index] > (isNearWater(reach, index) ? nearWaterThreshold : threshold)) {
                context.setResource(index, ResourceType.TREE);
            }
        }
    }

    /**
     * Returns the tree noise above which a tree grows away from the water: one minus the density for a density that is
     * a share of the noise range, and for a density that is a share of the land the quantile that leaves that many
     * trees, as a share of the tiles that may grow one, above it.
     */
    private double thresholdOf(GenerationContext context, float[] treeNoise) {
        if (!params.relative()) {
            return 1.0 - params.density();
        }
        var land = 0;
        var growing = 0;
        for (var index = 0; index < context.size(); index++) {
            land += context.groundAt(index).isLand() ? 1 : 0;
            growing += canGrow(context, index) ? 1 : 0;
        }
        if (growing == 0) {
            return Double.POSITIVE_INFINITY;
        }
        final var share = Math.min(1.0, (double) params.density() * land / growing);
        // The quantile is the lowest noise that grows a tree, and the comparison is strict, so step just below it
        return Math.nextDown(FieldQuantile.above(treeNoise, index -> canGrow(context, index), share));
    }

    /** Tells whether a tile is fertile, free of resources and not reserved, so a tree may stand on it. */
    private static boolean canGrow(GenerationContext context, int index) {
        return context.resourceAt(index) == null && !context.isReserved(index)
                && context.groundAt(index).isFertile();
    }

    /** The distance of every tile to the nearest river tile. */
    private static int[] distanceToRivers(GenerationContext context) {
        return DistanceTransform.chebyshev(context.width(), context.height(),
                index -> context.groundAt(index) == TileType.RIVER);
    }

    /** Tells whether a tile lies within the water reach of a river, when the distances were measured at all. */
    private boolean isNearWater(int[] reach, int index) {
        return reach != null && reach[index] <= params.waterReach();
    }
}
