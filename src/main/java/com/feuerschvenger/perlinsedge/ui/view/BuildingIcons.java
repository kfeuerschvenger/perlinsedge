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
        final Image image = image(type);
        final ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(!isSprite(type));
        return view;
    }

    /**
     * Gives the art of a type: its sprite, or the cropped render of the procedural shape.
     *
     * @param type building type
     * @return the image, never null
     */
    static synchronized Image image(BuildingType type) {
        final Image sprite = BuildingSprites.getInstance().restingImage(type);
        if (sprite != null) {
            return sprite;
        }
        return CACHE.computeIfAbsent(type, BuildingIcons::renderProcedural);
    }

    private static boolean isSprite(BuildingType type) {
        return BuildingSprites.getInstance().restingImage(type) != null;
    }

    private static Image renderProcedural(BuildingType type) {
        final Canvas canvas = new Canvas(SCRATCH_SIZE, SCRATCH_SIZE);
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        FxBuildingPainter.draw(gc, BuildingFactory.create(type, 0, 0), SCRATCH_SIZE / 2.0, SCRATCH_SIZE * 0.6, 1.0);
        final SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        final WritableImage full = canvas.snapshot(parameters, null);
        return crop(full);
    }

    /** Cuts an image down to the box that holds its non-transparent pixels. */
    private static Image crop(WritableImage image) {
        final PixelReader reader = image.getPixelReader();
        final int width = (int) image.getWidth();
        final int height = (int) image.getHeight();
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (((reader.getArgb(x, y) >>> 24) & 0xFF) > 0) {
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
