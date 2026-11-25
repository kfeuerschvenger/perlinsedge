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

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var treeNoise = context.field(NoiseField.TREE);
        final var threshold = 1.0 - params.density();
        final var nearWaterThreshold = threshold - params.waterBonus();
        final var reach = params.waterReach() > 0 ? distanceToRivers(context) : null;
        for (var index = 0; index < context.size(); index++) {
            if (canGrow(context, index)
                    && treeNoise[index] > (isNearWater(reach, index) ? nearWaterThreshold : threshold)) {
                context.setResource(index, ResourceType.TREE);
            }
        }
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
