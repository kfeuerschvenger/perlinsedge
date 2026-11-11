package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxTileDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.utils.AssetPaths;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundDecals;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Draws the beds under the water and the decals lying on them, before any other tile.
 * A bed is a real land block with its grid edge and side faces, so it shows through the translucent water as ground.
 * Every bed is drawn before the first decal, and both before the translucent water of the frame, so the water is
 * painted once over a finished bed and a decal is never covered by the bed of a neighbour. A bed sits a full land face
 * below the land tops: the land blocks around the water are drawn later and cover its edge, so their side faces show as
 * banks. Nothing is allocated, the choices are pure functions of the tile and the world seed.
 */
final class WaterBedPainter {

    private final GraphicsContext gc;
    private final SpriteManager spriteManager;

    WaterBedPainter(GraphicsContext gc, SpriteManager spriteManager) {
        this.gc = gc;
        this.spriteManager = spriteManager;
    }

    /**
     * Draws the beds and the underwater decals of the water tiles in a range of the map.
     *
     * @param map     the map
     * @param minX    first column, inclusive
     * @param maxX    last column, exclusive
     * @param minY    first row, inclusive
     * @param maxY    last row, exclusive
     * @param offsetX camera X offset
     * @param offsetY camera Y offset
     * @param zoom    camera zoom
     * @param seed    world seed
     * @param canvasWidth  canvas width
     * @param canvasHeight canvas height
     */
    void paint(TileMap map, int minX, int maxX, int minY, int maxY, double offsetX, double offsetY, double zoom,
               long seed, double canvasWidth, double canvasHeight) {
        final AppConfig.GraphicsConfig graphics = AppConfig.getInstance().graphics();
        final double halfWidth = graphics.getTileHalfWidth() * zoom;
        final double halfHeight = graphics.getTileHalfHeight() * zoom;
        final double tileWidth = graphics.getTileWidth() * zoom;
        final double tileHeight = graphics.getTileHeight() * zoom;
        final double bedSink = AssetPaths.BED_SINK_PIXELS * ZoomSteps.artScale(zoom);

        for (int pass = 0; pass < 2; pass++) {
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    final Tile tile = map.getTile(x, y);
                    if (tile == null || !tile.getType().isWater()) {
                        continue;
                    }
                    final double screenX = (x - y) * halfWidth + offsetX;
                    final double screenY = (x + y) * halfHeight + offsetY;
                    if (!ScreenCulling.isTileVisible(screenX, screenY, tileWidth, tileHeight, canvasWidth, canvasHeight)) {
                        continue;
                    }
                    if (pass == 0) {
                        drawBed(tile, screenX, screenY + bedSink, zoom);
                    } else {
                        drawUnderwaterDecal(tile, screenX, screenY + bedSink, zoom, seed);
                    }
                }
            }
        }
    }

    private void drawBed(Tile tile, double screenX, double bedY, double zoom) {
        final Image bed = spriteManager.getTileSprite(tile.getType(), tile.getX(), tile.getY());
        if (bed != null) {
            FxTileDrawingHelper.drawTileSprite(gc, bed, screenX, bedY, zoom);
        }
    }

    private void drawUnderwaterDecal(Tile tile, double screenX, double bedY, double zoom, long seed) {
        final int x = tile.getX();
        final int y = tile.getY();
        final Image decal = spriteManager.getDecalSprite(GroundDecals.underwaterDecalFor(tile.getType(), x, y, seed));
        if (decal != null) {
            FxTileDrawingHelper.drawDecal(gc, decal, screenX, bedY, zoom,
                    GroundDecals.offsetU(x, y, seed), GroundDecals.offsetV(x, y, seed));
        }
    }
}
