package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A sprite of an odd width moves by exactly the pan on every whole pixel pan, so it stays where the ground and its
 * shadow are.
 */
class FxTileDrawingHelperSnapTest {

    /** Zoom 0.5 is an art scale of one, so an art pixel is a screen pixel. */
    private static final double ZOOM = 0.5;

    private static Image block(int width, int height) {
        final WritableImage image = new WritableImage(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.getPixelWriter().setColor(x, y, Color.RED);
            }
        }
        return image;
    }

    /** Draws with the given painter on a fresh canvas and returns the first painted column. */
    private static int firstColumn(Consumer<GraphicsContext> painter) {
        return FxToolkitSupport.onFx(() -> {
            final Canvas canvas = new Canvas(60, 40);
            final GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setImageSmoothing(false);
            painter.accept(gc);
            final SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.TRANSPARENT);
            final PixelReader reader = canvas.snapshot(parameters, null).getPixelReader();
            for (int x = 0; x < 60; x++) {
                for (int y = 0; y < 40; y++) {
                    if (reader.getArgb(x, y) >>> 24 != 0) {
                        return x;
                    }
                }
            }
            return -1;
        });
    }

    @Test
    void aDecalOfOddWidthMovesByExactlyTheWholePixelPan() {
        final Image decal = block(3, 2);
        final int first = firstColumn(gc -> FxTileDrawingHelper.drawDecal(gc, decal, 20, 5, ZOOM, 0, 0));

        for (int pan = 1; pan <= 4; pan++) {
            final double screenX = 20 + pan;
            assertEquals(first + pan, firstColumn(gc -> FxTileDrawingHelper.drawDecal(gc, decal, screenX, 5, ZOOM, 0, 0)),
                    "a pan of " + pan + " moved the decal by another distance");
        }
    }

    @Test
    void aTileSpriteOfOddWidthMovesByExactlyTheWholePixelPan() {
        final Image tile = block(3, 2);
        final int first = firstColumn(gc -> FxTileDrawingHelper.drawTileSprite(gc, tile, 20, 5, ZOOM));

        for (int pan = 1; pan <= 4; pan++) {
            final double screenX = 20 + pan;
            assertEquals(first + pan, firstColumn(gc -> FxTileDrawingHelper.drawTileSprite(gc, tile, screenX, 5, ZOOM)),
                    "a pan of " + pan + " moved the tile by another distance");
        }
    }
}
