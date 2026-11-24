package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Digs a few small oases into open sand: a pocket of shallow water with a deeper middle tile. The number grows with the
 * map area and every oasis needs a clear stretch of plain sand around it, so none lands on a hill, on the shore of an
 * existing lake or at the edge of the map.
 */
public final class OasisPass implements GenerationPass {

    private static final String NAME = "oases";
    private static final long SALT = 0x0A51_5000_0000_0001L;
    /** Both sides of the margin count, so a span has to hold the margin twice. */
    private static final int MARGIN_SIDES = 2;

    private final OasisParams params;

    /**
     * Creates the pass.
     *
     * @param params how many oases there are and how big
     */
    public OasisPass(OasisParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var clearRadius = params.radius() + 1;
        final var margin = clearRadius + 1;
        final var spanX = context.width() - MARGIN_SIDES * margin;
        final var spanY = context.height() - MARGIN_SIDES * margin;
        if (spanX <= 0 || spanY <= 0) {
            return;
        }
        final var random = context.random(SALT);
        final var count = Math.max(1, context.size() / params.tilesPerOasis());
        for (var i = 0; i < count; i++) {
            digOne(context, random, clearRadius, margin, spanX, spanY);
        }
    }

    /** Tries places at random until an oasis fits or the attempts run out. */
    private void digOne(GenerationContext context, RandomGenerator random, int clearRadius, int margin, int spanX,
                        int spanY) {
        for (var attempt = 0; attempt < params.siteAttempts(); attempt++) {
            final var x = margin + random.nextInt(spanX);
            final var y = margin + random.nextInt(spanY);
            if (isPlainSand(context, x, y, clearRadius)) {
                fill(context, x, y);
                return;
            }
        }
    }

    /** Tells whether every tile within a radius of a point is sand, so an oasis fits without cutting other ground. */
    private static boolean isPlainSand(GenerationContext context, int x, int y, int radius) {
        for (var dy = -radius; dy <= radius; dy++) {
            for (var dx = -radius; dx <= radius; dx++) {
                if (context.groundAt(context.index(x + dx, y + dy)) != TileType.SAND) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Turns a disc of sand into shallow water with a deep centre. */
    private void fill(GenerationContext context, int centerX, int centerY) {
        final var radius = params.radius();
        for (var dy = -radius; dy <= radius; dy++) {
            for (var dx = -radius; dx <= radius; dx++) {
                final var squared = dx * dx + dy * dy;
                if (squared <= radius * radius + 1) {
                    context.setGround(context.index(centerX + dx, centerY + dy),
                            squared == 0 ? TileType.DEEP_WATER : TileType.SHALLOW_WATER);
                }
            }
        }
    }
}
