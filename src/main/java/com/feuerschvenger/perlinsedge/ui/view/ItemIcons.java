package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxItemDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Builds the item icons the panels show, so every panel scales the art the same way.
 * The icons are 16 px pixel art. An icon is shown at a whole multiple of its own size with smoothing off, so each art
 * pixel stays a crisp square block; a size that is not a whole multiple would blur or uneven the pixels.
 */
final class ItemIcons {

    /** Not instantiable: the class only builds icons. */
    private ItemIcons() {
    }

    /**
     * Gives the icon of an item type as a node.
     *
     * @param type item type
     * @param size edge of the square the icon fills, in pixels; a whole multiple of the art size keeps it crisp
     * @return the icon, drawn without smoothing
     */
    static ImageView view(ItemType type, int size) {
        var imageView = new ImageView(image(type, size));
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(false);
        return imageView;
    }

    /**
     * Gives the art of an item type, or a drawn stand-in when the type has no art.
     *
     * @param type item type
     * @param size edge in pixels of the stand-in, which only matters when the type has no art
     * @return the image
     */
    static Image image(ItemType type, int size) {
        SpriteManager.SpriteMetadata metadata = SpriteManager.getInstance().getItemSprite(type);
        if (metadata != null && metadata.getImage() != null) {
            return metadata.getImage();
        }
        return fallback(type, size);
    }

    private static Image fallback(ItemType type, int size) {
        var canvas = new Canvas(size, size);
        var gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, size, size);
        FxItemDrawingHelper.drawFallbackShape(gc, 0, 0, size, size, type, 1.0);
        return canvas.snapshot(null, null);
    }
}
