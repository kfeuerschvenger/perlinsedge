package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;

/**
 * Asks the spawn to be on the largest piece of land. The walkable ground, resources ignored, is labelled into
 * 4-connected pieces, and the hint is the tile of the largest piece that lies nearest to its centroid, so on a sea of
 * islands the player starts in the middle of the largest island, however the islands are laid out. The centroid of an
 * island with a bay can lie in the water, which is why the hint is a tile of the piece and not the centroid itself.
 * <p>
 * The pass reads the ground only, draws no random numbers and sets the spawn hint; on a map with no walkable ground it
 * leaves the hint alone.
 */
public final class LandmassHintPass implements GenerationPass {

    private static final String NAME = "landmass-hint";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var land = Regions.label(context.width(), context.height(),
                index -> context.groundAt(index).isWalkable());
        final var largest = land.largest();
        if (largest == Regions.NO_LABEL) {
            return;
        }
        var sumX = 0L;
        var sumY = 0L;
        for (var index = 0; index < context.size(); index++) {
            if (land.labelAt(index) == largest) {
                sumX += context.xOf(index);
                sumY += context.yOf(index);
            }
        }
        final var size = (double) land.sizeOf(largest);
        context.setSpawnHint(nearestTile(context, land, largest, sumX / size, sumY / size));
    }

    /** Returns the first tile of a piece, in row order, nearest to a point. */
    private static int nearestTile(GenerationContext context, Regions land, int label, double x, double y) {
        var best = GenerationContext.NONE;
        var bestDistance = Double.POSITIVE_INFINITY;
        for (var index = 0; index < context.size(); index++) {
            if (land.labelAt(index) == label) {
                final var dx = context.xOf(index) - x;
                final var dy = context.yOf(index) - y;
                final var distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    best = index;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }
}
