package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;

/**
 * Turns the ground under and around the trees into forest floor. Every tile of a floor terrain, such as grass, that
 * holds a tree or lies within the reach of one becomes {@link TileType#FOREST}, so the forest is a product of the trees
 * and the two always agree. Reserved tiles, the clearings and trails, keep their ground so they stay open and read as
 * paths on the minimap. The pass stamps a square around each tree, so its cost follows the number of trees.
 */
public final class ForestFloorPass implements GenerationPass {

    private static final String NAME = "forest-floor";

    private final ForestFloorParams params;
    private final boolean[] isFloor;

    /**
     * Creates the pass.
     *
     * @param params how far the floor reaches and which terrains turn into it
     */
    public ForestFloorPass(ForestFloorParams params) {
        this.params = Objects.requireNonNull(params, "params");
        this.isFloor = new boolean[TileType.values().length];
        for (final var type : params.floors()) {
            isFloor[type.ordinal()] = true;
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var reach = params.reach();
        for (var y = 0; y < context.height(); y++) {
            for (var x = 0; x < context.width(); x++) {
                if (context.resourceAt(context.index(x, y)) == ResourceType.TREE) {
                    stamp(context, x, y, reach);
                }
            }
        }
    }

    /** Floors the square of the given reach around a tile, as far as it lies on the map. */
    private void stamp(GenerationContext context, int x, int y, int reach) {
        final var lastY = Math.min(context.height() - 1, y + reach);
        final var lastX = Math.min(context.width() - 1, x + reach);
        for (var ny = Math.max(0, y - reach); ny <= lastY; ny++) {
            for (var nx = Math.max(0, x - reach); nx <= lastX; nx++) {
                final var index = context.index(nx, ny);
                if (isFloor[context.groundAt(index).ordinal()] && !context.isReserved(index)) {
                    context.setGround(index, TileType.FOREST);
                }
            }
        }
    }
}
