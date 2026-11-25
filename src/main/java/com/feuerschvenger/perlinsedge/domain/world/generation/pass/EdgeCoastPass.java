package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;

import java.util.Objects;

/**
 * Gives a map a long coast along one edge: the seed picks one of the four edges, and the height is lowered along it by
 * the drop of the parameters at the edge itself, fading to nothing over the depth with a smoothstep curve, so the sea
 * lies along the whole edge and its shore follows the noise. It runs on the height before the ground pass reads it.
 * <p>
 * The pass draws one number from the seeded stream and touches the height field only, so it repeats for a seed.
 */
public final class EdgeCoastPass implements GenerationPass {

    private static final String NAME = "edge-coast";
    private static final long SALT = 0x0EDC_0A57_5EED_0001L;
    /** The smoothstep curve is {@code 3t^2 - 2t^3}. */
    private static final double SMOOTH_QUADRATIC = 3.0;
    private static final double SMOOTH_CUBIC = 2.0;

    /** The four edges of the map and how far a tile lies from each. */
    private enum Edge {
        TOP, BOTTOM, LEFT, RIGHT;

        /** Returns how many tiles a tile lies from the edge, 0 for a tile on it. */
        int distance(int x, int y, int width, int height) {
            return switch (this) {
                case TOP -> y;
                case BOTTOM -> height - 1 - y;
                case LEFT -> x;
                case RIGHT -> width - 1 - x;
            };
        }
    }

    private final EdgeCoastParams params;

    /**
     * Creates the pass.
     *
     * @param params how deep the coast reaches and how far it lowers the height
     */
    public EdgeCoastPass(EdgeCoastParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var edges = Edge.values();
        final var edge = edges[context.random(SALT).nextInt(edges.length)];
        final var heights = context.field(NoiseField.HEIGHT);
        for (var y = 0; y < context.height(); y++) {
            for (var x = 0; x < context.width(); x++) {
                final var distance = edge.distance(x, y, context.width(), context.height());
                if (distance < params.depthTiles()) {
                    final var index = context.index(x, y);
                    heights[index] = (float) (heights[index] - params.drop() * smoothstep(distance));
                }
            }
        }
    }

    /** Returns 1 at the edge and 0 at the depth of the coast, along a smooth curve. */
    private double smoothstep(int distance) {
        final var remaining = 1.0 - distance / (double) params.depthTiles();
        return remaining * remaining * (SMOOTH_QUADRATIC - SMOOTH_CUBIC * remaining);
    }
}
