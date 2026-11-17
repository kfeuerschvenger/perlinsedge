package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingFactory;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ZoomSteps;
import com.feuerschvenger.perlinsedge.infra.fx.utils.BuildingSprites;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GhostSprites;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Draws buildings and the placement ghost: the sprite of a piece when it has art, the procedural shape of
 * {@link FxBuildingPainter} when it has not.
 */
public final class FxBuildingDrawingHelper {
    /** Opacity of the placement ghost, low enough that the ground shows through. */
    public static final double GHOST_OPACITY = 0.65;
    /** Opacity of the tint laid on the footprint under a ghost that has no art. */
    private static final double FOOTPRINT_TINT_OPACITY = 0.30;

    /** Not instantiable: the class only holds the drawing calls. */
    private FxBuildingDrawingHelper() {
    }

    /**
     * Draws a building from its sprite when it has one, otherwise through {@link FxBuildingPainter}.
     *
     * @param gc      graphics context
     * @param building the building to draw
     * @param sprite  the art of the building in its present state, or null to draw the procedural shape
     * @param centerX screen X of the tile centre
     * @param centerY screen Y of the tile centre
     * @param offsetX camera X offset, for the procedural shape
     * @param offsetY camera Y offset, for the procedural shape
     * @param zoom    current zoom level
     */
    public static void drawBuilding(GraphicsContext gc, Building building, Image sprite, double centerX,
                                    double centerY, double offsetX, double offsetY, double zoom) {
        if (sprite != null) {
            drawSprite(gc, sprite, centerX, centerY, ZoomSteps.integerArtScale(zoom));
        } else {
            drawBuilding(gc, building, offsetX, offsetY, zoom);
        }
    }

    /**
     * Draws a building through {@link FxBuildingPainter}.
     *
     * @param building The building to draw.
     */
    public static void drawBuilding(GraphicsContext gc, Building building,
                                    double offsetX, double offsetY, double zoom
    ) {
        double[] screenCords = IsometricUtils.tileToScreen(
                building.getTileX(), building.getTileY(),
                offsetX, offsetY,
                zoom,
                AppConfig.getInstance().graphics().getTileHalfWidth(),
                AppConfig.getInstance().graphics().getTileHalfHeight()
        );
        FxBuildingPainter.draw(gc, building, screenCords[0], screenCords[1], zoom);
    }

    /**
     * Draws a building sprite with its bottom centre on the bottom vertex of the tile, see {@link BuildingSprites}.
     * It is drawn at a whole multiple of its own size on whole screen pixels, which is where its shadow is anchored.
     */
    private static void drawSprite(GraphicsContext gc, Image sprite, double centerX, double centerY, double artScale) {
        final int widthArt = (int) sprite.getWidth();
        final int heightArt = (int) sprite.getHeight();
        gc.drawImage(sprite,
                PixelSnap.round(BuildingSprites.drawX(centerX, widthArt, artScale)),
                PixelSnap.round(BuildingSprites.drawY(centerY, heightArt, artScale)),
                widthArt * artScale, heightArt * artScale);
    }

    /**
     * Draws the placement ghost of a piece on a tile: the sprite of the piece mixed toward the tint at a low opacity,
     * or, for a piece with no art, its procedural shape at the same opacity over a footprint filled with the tint.
     *
     * @param gc      graphics context
     * @param preview the piece and the tint
     * @param sprites the art of the piece, which is several images for a fence, or null or none when it has none
     * @param centerX screen X of the tile centre
     * @param centerY screen Y of the tile centre
     * @param offsetX camera X offset
     * @param offsetY camera Y offset
     * @param zoom    current zoom level
     */
    public static void drawGhost(GraphicsContext gc, BuildingPreview preview, Image[] sprites, double centerX,
                                 double centerY, double offsetX, double offsetY, double zoom) {
        gc.save();
        gc.setGlobalAlpha(GHOST_OPACITY);
        if (sprites != null && sprites.length > 0) {
            for (Image sprite : sprites) {
                drawSprite(gc, GhostSprites.of(sprite, preview.tint()), centerX, centerY, ZoomSteps.integerArtScale(zoom));
            }
        } else {
            drawFootprint(gc, preview.color(), centerX, centerY, zoom);
            final Building ghost = BuildingFactory.create(preview.type(), preview.tileX(), preview.tileY(),
                    preview.rotation());
            if (ghost instanceof Fence fence) {
                fence.setConnections(preview.connections());
            }
            FxBuildingPainter.draw(gc, ghost, centerX,
                    centerY - AppConfig.getInstance().graphics().getTileHalfHeight() * zoom, zoom);
        }
        gc.restore();
    }

    /**
     * Draws several sprites of one building, such as the layers of a fence, one over the other.
     *
     * @param gc      graphics context
     * @param layers  the images in the order they are drawn
     * @param centerX screen X of the tile centre
     * @param centerY screen Y of the tile centre
     * @param zoom    current zoom level
     */
    public static void drawLayers(GraphicsContext gc, Image[] layers, double centerX, double centerY, double zoom) {
        for (Image layer : layers) {
            drawSprite(gc, layer, centerX, centerY, ZoomSteps.integerArtScale(zoom));
        }
    }

    /** Fills the diamond of a tile with a tint. */
    private static void drawFootprint(GraphicsContext gc, Color color, double centerX, double centerY, double zoom) {
        final double halfWidth = AppConfig.getInstance().graphics().getTileHalfWidth() * zoom;
        final double halfHeight = AppConfig.getInstance().graphics().getTileHalfHeight() * zoom;
        gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), FOOTPRINT_TINT_OPACITY / GHOST_OPACITY));
        gc.fillPolygon(
                new double[]{centerX, centerX + halfWidth, centerX, centerX - halfWidth},
                new double[]{centerY - halfHeight, centerY, centerY + halfHeight, centerY}, 4);
    }
}
