package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxTileDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundDecals;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import com.feuerschvenger.perlinsedge.infra.fx.utils.TileDepths;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Draws the parts of a tile that make it a column: the sunk bed under a water tile with the decal lying on it, and the
 * side faces that a lower neighbour exposes.
 * The tiles are drawn back to front, so a face is only exposed on the two sides that look toward the viewer, the left
 * one over the neighbour one row down and the right one over the neighbour one column on. Only the face pixels that can
 * show are drawn, from a face strip per tile art that is drawn once per 5 pixels of exposed drop, and a tile whose
 * neighbours are not lower draws nothing extra. Nothing is allocated, the choices are pure functions of the tile types
 * and the world seed.
 */
final class ColumnPainter {

    private final GraphicsContext gc;
    private final SpriteManager spriteManager;

    ColumnPainter(GraphicsContext gc, SpriteManager spriteManager) {
        this.gc = gc;
        this.spriteManager = spriteManager;
    }

    /**
     * Draws the bed of a water tile at its depth, the faces its deeper neighbours expose, and its underwater decal.
     * The bed is drawn first and the water sheet of the tile right after, so the sheet is covered by any land drawn
     * later in front of it.
     *
     * @param map     the map
     * @param tile    the water tile
     * @param bed     the bed block sprite of the tile
     * @param art     the tile art name of the bed
     * @param screenX X of the top vertex of the tile at the land tops
     * @param screenY Y of the top vertex of the tile at the land tops
     * @param zoom    camera zoom
     * @param seed    world seed
     */
    void drawBed(TileMap map, Tile tile, Image bed, String art, double screenX, double screenY, double zoom, long seed) {
        final double bedY = screenY + TileDepths.sinkPixels(tile.getType()) * ZoomSteps.artScale(zoom);
        FxTileDrawingHelper.drawTileSprite(gc, bed, screenX, bedY, zoom);
        drawExposedFaces(map, tile, art, screenX, bedY, zoom);
        final int x = tile.getX();
        final int y = tile.getY();
        final Image decal = spriteManager.getDecalSprite(GroundDecals.underwaterDecalFor(tile.getType(), x, y, seed));
        if (decal != null) {
            FxTileDrawingHelper.drawDecal(gc, decal, screenX, bedY, zoom,
                    GroundDecals.offsetU(x, y, seed), GroundDecals.offsetV(x, y, seed));
        }
    }

    /**
     * Draws the face strips under a tile on the sides where a lower neighbour exposes its face.
     *
     * @param map     the map
     * @param tile    the tile
     * @param art     the tile art name, which picks the face colours
     * @param screenX X of the top vertex of the tile
     * @param screenY Y of the top vertex of the tile, at its own depth
     * @param zoom    camera zoom
     */
    void drawExposedFaces(TileMap map, Tile tile, String art, double screenX, double screenY, double zoom) {
        final Image strip = spriteManager.getFaceStrip(art);
        if (strip == null) {
            return;
        }
        final Tile below = map.getTile(tile.getX(), tile.getY() + 1);
        final Tile beside = map.getTile(tile.getX() + 1, tile.getY());
        final int left = TileDepths.stripsExposed(tile.getType(), below == null ? null : below.getType());
        final int right = TileDepths.stripsExposed(tile.getType(), beside == null ? null : beside.getType());
        if (left > 0) {
            drawStrips(strip, false, left, screenX, screenY, zoom);
        }
        if (right > 0) {
            drawStrips(strip, true, right, screenX, screenY, zoom);
        }
    }

    /** Draws one half of a face strip, the strip once per 5 pixels of drop, each one 5 pixels below the last. */
    private void drawStrips(Image strip, boolean rightHalf, int count, double screenX, double screenY, double zoom) {
        final double scale = ZoomSteps.artScale(zoom);
        final double halfWidth = strip.getWidth() / 2;
        final double height = strip.getHeight();
        final double left = Math.rint(screenX - halfWidth * scale) + (rightHalf ? halfWidth * scale : 0);
        final double top = Math.rint(screenY);
        for (int i = 0; i < count; i++) {
            gc.drawImage(strip, rightHalf ? halfWidth : 0, 0, halfWidth, height,
                    left, top + i * TileDepths.FACE_PIXELS * scale, halfWidth * scale, height * scale);
        }
    }
}
