package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Turns the height field into a sea with islands: picks island centres in tiles, and lifts the height around each one
 * by an amount that fades to nothing at the radius. Beyond every radius the height is the sea floor, so the noise only
 * shapes the coasts.
 */
public final class IslandShapePass implements GenerationPass {

    private static final String NAME = "island-shape";
    private static final long SALT = 0x15_1A_4D_5E_ED_00_01L;
    private static final double TILES_PER_UNIT = 100_000.0;
    /** The centres keep this share of the radius away from the edge of the map, so an island is mostly on the map. */
    private static final double EDGE_MARGIN_OF_RADIUS = 0.4;
    /** The margin is kept on both sides of an axis. */
    private static final int MARGIN_SIDES = 2;

    private final IslandParams params;

    /**
     * Creates the pass.
     *
     * @param params the island count, size and lift
     */
    public IslandShapePass(IslandParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var heights = context.field(NoiseField.HEIGHT);
        final var count = islandCount(context.width(), context.height());
        final var centresX = new int[count];
        final var centresY = new int[count];
        placeCentres(context, centresX, centresY);

        final var radius = (double) params.radiusTiles();
        final var radiusSquared = (long) params.radiusTiles() * params.radiusTiles();
        for (var y = 0; y < context.height(); y++) {
            for (var x = 0; x < context.width(); x++) {
                final var nearest = nearestSquaredDistance(centresX, centresY, x, y);
                final var effect = nearest >= radiusSquared
                        ? 0.0
                        : Math.pow(1.0 - Math.sqrt((double) nearest) / radius, params.falloffPower());
                final var index = context.index(x, y);
                heights[index] = (float) (heights[index] * effect * params.heightGain()
                        + (effect * params.lift() - params.seaOffset()));
            }
        }
    }

    /**
     * Returns how many islands a map of a size gets: the density times the area, never below the minimum.
     *
     * @param width  the number of columns
     * @param height the number of rows
     * @return the island count
     */
    int islandCount(int width, int height) {
        final var byArea = (int) Math.round((double) width * height * params.islandsPer100kTiles() / TILES_PER_UNIT);
        return Math.max(params.minIslands(), byArea);
    }

    /** Draws the centres at random tiles, away from the edge by a share of the radius where the map allows it. */
    private void placeCentres(GenerationContext context, int[] centresX, int[] centresY) {
        final var random = context.random(SALT);
        final var margin = (int) Math.round(params.radiusTiles() * EDGE_MARGIN_OF_RADIUS);
        for (var i = 0; i < centresX.length; i++) {
            centresX[i] = centreOnAxis(random, context.width(), margin);
            centresY[i] = centreOnAxis(random, context.height(), margin);
        }
    }

    /** Draws one coordinate between the margins, or the middle of the axis when the margins leave no room. */
    private static int centreOnAxis(RandomGenerator random, int length, int margin) {
        final var usableMargin = Math.min(margin, (length - 1) / 2);
        return usableMargin + random.nextInt(length - MARGIN_SIDES * usableMargin);
    }

    /** Returns the squared distance in tiles from a tile to the closest centre. */
    private static long nearestSquaredDistance(int[] centresX, int[] centresY, int x, int y) {
        var nearest = Long.MAX_VALUE;
        for (var i = 0; i < centresX.length; i++) {
            final long dx = x - centresX[i];
            final long dy = y - centresY[i];
            nearest = Math.min(nearest, dx * dx + dy * dy);
        }
        return nearest;
    }
}
