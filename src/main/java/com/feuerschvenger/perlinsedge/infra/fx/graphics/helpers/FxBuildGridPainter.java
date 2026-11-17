package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ZoomSteps;
import com.feuerschvenger.perlinsedge.infra.fx.utils.TileDepths;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws the faint grid that shows where the pieces will land while the hammer is in hand: the outline of every tile
 * within a few tiles of the hovered one, fading out with the distance from it. It is drawn on the ground, under the
 * shadows and the objects, and is low in opacity so it never competes with the world.
 */
public final class FxBuildGridPainter {
    /** Distance in tiles from the hovered tile at which the grid has faded out completely. */
    public static final double RADIUS_TILES = 3.5;
    /** Opacity of the outline of the hovered tile, the strongest of the grid. */
    public static final double MAX_OPACITY = 0.30;

    private static final Color LINE = Color.rgb(252, 244, 228);
    private static final double LINE_WIDTH = 1.0;

    /** Not instantiable: the class only holds the drawing call. */
    private FxBuildGridPainter() {
    }

    /**
     * Gives the opacity of the outline of a tile at an offset from the hovered one.
     *
     * @param dx tile offset along X
     * @param dy tile offset along Y
     * @return {@link #MAX_OPACITY} on the hovered tile, falling linearly to 0 at {@link #RADIUS_TILES}, and 0 beyond
     */
    public static double opacity(int dx, int dy) {
        final double distance = Math.sqrt((double) dx * dx + (double) dy * dy);
        if (distance >= RADIUS_TILES) {
            return 0.0;
        }
        return MAX_OPACITY * (1.0 - distance / RADIUS_TILES);
    }

    /**
     * Draws the grid around a tile.
     *
     * @param gc      graphics context
     * @param map     the map, whose tiles outside its edge are skipped
     * @param tileX   the hovered tile X
     * @param tileY   the hovered tile Y
     * @param offsetX camera X offset
     * @param offsetY camera Y offset
     * @param zoom    current zoom level
     */
    public static void draw(GraphicsContext gc, TileMap map, int tileX, int tileY, double offsetX, double offsetY,
                            double zoom) {
        final AppConfig.GraphicsConfig graphics = AppConfig.getInstance().graphics();
        final double halfWidth = graphics.getTileHalfWidth() * zoom;
        final double halfHeight = graphics.getTileHalfHeight() * zoom;
        final double sheetDrop = TileDepths.SURFACE_PIXELS * ZoomSteps.integerArtScale(zoom);
        final int reach = (int) Math.ceil(RADIUS_TILES);
        final double[] xs = new double[4];
        final double[] ys = new double[4];

        gc.save();
        gc.setStroke(LINE);
        gc.setLineWidth(LINE_WIDTH);
        for (int dy = -reach; dy <= reach; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                final double opacity = opacity(dx, dy);
                final Tile tile = opacity > 0 ? map.getTile(tileX + dx, tileY + dy) : null;
                if (tile == null) continue;
                final int x = tileX + dx;
                final int y = tileY + dy;
                final double centerX = (x - y) * halfWidth + offsetX;
                final double centerY = (x + y) * halfHeight + offsetY + halfHeight
                        + (tile.getType().isWater() ? sheetDrop : 0.0);
                xs[0] = centerX;
                xs[1] = centerX + halfWidth;
                xs[2] = centerX;
                xs[3] = centerX - halfWidth;
                ys[0] = centerY - halfHeight;
                ys[1] = centerY;
                ys[2] = centerY + halfHeight;
                ys[3] = centerY;
                gc.setGlobalAlpha(opacity);
                gc.strokePolygon(xs, ys, 4);
            }
        }
        gc.restore();
    }
}
