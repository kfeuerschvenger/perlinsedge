package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingFactory;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxBuildingPainter;
import com.feuerschvenger.perlinsedge.infra.fx.utils.BuildingSprites;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Map;

/**
 * Builds the building icons the palette shows: the sprite of the piece when it has art, otherwise a small render of
 * the procedural shape cropped to its painted pixels, so every piece fills its slot the same way.
 */
final class BuildingIcons {
    /** Edge of the scratch canvas the procedural shape is drawn on, larger than any shape at zoom 1. */
    private static final int SCRATCH_SIZE = 160;
    /** Where the shape is anchored on the scratch canvas, as a fraction of its edge: centred horizontally. */
    private static final double ANCHOR_X_FRACTION = 0.5;
    /** Where the shape is anchored on the scratch canvas, as a fraction of its edge: a little below the middle. */
    private static final double ANCHOR_Y_FRACTION = 0.6;
    /** Bits the alpha channel is shifted by in an ARGB pixel. */
    private static final int ALPHA_SHIFT = 24;
    private static final Map<BuildingType, Image> CACHE = new EnumMap<>(BuildingType.class);

    /** Not instantiable: the class only builds icons. */
    private BuildingIcons() {
    }

    /**
     * Gives the icon of a building type as a node.
     *
     * @param type building type
     * @param size edge of the square the icon fits in, in pixels
     * @return the icon with its aspect kept, drawn without smoothing when it is pixel art
     */
    static ImageView view(BuildingType type, int size) {
        return view(type, 0, size);
    }

    /**
     * Gives the icon of a building type facing a given way as a node.
     *
     * @param type     building type
     * @param rotation the way the piece faces, which only a piece with art shows
     * @param size     edge of the square the icon fits in, in pixels
     * @return the icon with its aspect kept
     */
    static ImageView view(BuildingType type, int rotation, int size) {
        final var view = new ImageView();
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        show(view, type, rotation);
        return view;
    }

    /**
     * Shows a type facing a given way in an icon that already exists, so a card follows the rotation without a new node.
     *
     * @param view     the icon to change
     * @param type     building type
     * @param rotation the way the piece faces
     */
    static void show(ImageView view, BuildingType type, int rotation) {
        view.setImage(image(type, rotation));
        view.setSmooth(!isSprite(type, rotation));
    }

    /**
     * Gives the art of a type facing a given way: its sprite, or the cropped render of the procedural shape, which looks
     * the same whichever way it faces.
     *
     * @param type     building type
     * @param rotation the way the piece faces
     * @return the image, never null
     */
    static synchronized Image image(BuildingType type, int rotation) {
        final var sprite = BuildingSprites.getInstance().restingImage(type, rotation);
        if (sprite != null) {
            return sprite;
        }
        return CACHE.computeIfAbsent(type, BuildingIcons::renderProcedural);
    }

    /**
     * Gives the art of a type facing the first way.
     *
     * @param type building type
     * @return the image, never null
     */
    static Image image(BuildingType type) {
        return image(type, 0);
    }

    private static boolean isSprite(BuildingType type, int rotation) {
        return BuildingSprites.getInstance().restingImage(type, rotation) != null;
    }

    private static Image renderProcedural(BuildingType type) {
        final var canvas = new Canvas(SCRATCH_SIZE, SCRATCH_SIZE);
        final var gc = canvas.getGraphicsContext2D();
        FxBuildingPainter.draw(gc, BuildingFactory.create(type, 0, 0), SCRATCH_SIZE * ANCHOR_X_FRACTION, SCRATCH_SIZE * ANCHOR_Y_FRACTION, 1.0);
        final var parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        final WritableImage full = canvas.snapshot(parameters, null);
        return crop(full);
    }

    /** Cuts an image down to the box that holds its non-transparent pixels. */
    private static Image crop(WritableImage image) {
        final var reader = image.getPixelReader();
        final int width = (int) image.getWidth();
        final int height = (int) image.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                if (((reader.getArgb(x, y) >>> ALPHA_SHIFT) & 0xFF) > 0) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return image;
        }
        return new WritableImage(reader, minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
