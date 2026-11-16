package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowList;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundShadow;
import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SilhouetteSprites;
import javafx.scene.image.Image;

/**
 * Adds the shadows of what stands on a tile, resources and buildings. The tiles inside the view and the tiles outside it
 * whose shadows still reach into it both go through here, so an object throws its shadow from the same point wherever
 * the camera is.
 */
public final class FxObjectShadows {

    /** Width in art pixels the shadow is sized from for a resource drawn without art. */
    private static final double FALLBACK_RESOURCE_ART_WIDTH = 12.0;

    /** Share of the half height of a tile that a flat water tile drawn without art is lowered by. */
    private static final double WATER_DEPTH_SHARE = 0.5;

    /** Not instantiable: the class only holds static rules. */
    private FxObjectShadows() {}

    /**
     * Returns how far a tile is lowered on the screen: a water tile with no art is a flat stand-in drawn lower than land,
     * and whatever stands on it is lowered with it.
     *
     * @param water          whether the tile is water
     * @param hasTileArt     whether the tile is drawn from a sprite
     * @param halfTileHeight half the height of a tile in screen pixels
     * @return the drop in screen pixels
     */
    public static double waterDrop(boolean water, boolean hasTileArt, double halfTileHeight) {
        return water && !hasTileArt ? halfTileHeight * WATER_DEPTH_SHARE : 0.0;
    }

    /**
     * Adds the shadows of the resource and the building of a tile.
     *
     * @param shadows        the list to add to
     * @param resource       the type of the resource on the tile, or null for none
     * @param image          the sprite of the resource, or null when it is drawn without art
     * @param contactArt     how far below the tile centre the lowest painted pixel of the sprite lies, in art pixels,
     *                       zero for a sprite that stands on its bottom edge
     * @param building       the type of the building on the tile, or null for none
     * @param screenX        screen X of the tile centre
     * @param screenY        screen Y of the top vertex of the tile, already lowered by {@link #waterDrop}
     * @param halfTileHeight half the height of a tile in screen pixels
     * @param artScale       screen pixels per art pixel
     */
    public static void addForTile(ShadowList shadows, ResourceType resource, Image image, int contactArt,
                                  BuildingType building, double screenX, double screenY, double halfTileHeight,
                                  double artScale) {
        final double groundY = screenY + halfTileHeight;
        if (resource != null) {
            addResource(shadows, resource, image, screenX, groundY + contactArt * artScale, contactArt > 0);
        }
        if (building != null) {
            final double width = GroundShadow.widthFor(building);
            if (width > 0) {
                shadows.add(screenX, groundY, width, 1.0);
            }
        }
    }

    /** Adds the shadow of a resource: the silhouette of its sprite, or an ellipse when it is drawn without art. */
    private static void addResource(ShadowList shadows, ResourceType type, Image image, double screenX, double groundY,
                                    boolean low) {
        if (image != null) {
            // A low sprite touches the ground at its lowest painted row, a tall one at its bottom edge
            final Silhouette silhouette = SilhouetteSprites.of(image);
            final double groundRow = low ? silhouette.paintedBottom() : silhouette.height();
            shadows.addSilhouette(screenX, groundY, silhouette, groundRow, 1.0, 1.0);
        } else {
            shadows.add(screenX, groundY, GroundShadow.widthFor(type, FALLBACK_RESOURCE_ART_WIDTH), 1.0);
        }
    }
}
