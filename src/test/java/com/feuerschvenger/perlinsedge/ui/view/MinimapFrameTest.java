package com.feuerschvenger.perlinsedge.ui.view;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The minimap frame looks like a hotbar slot, because it takes its look from the same style classes. */
class MinimapFrameTest {

    private static Paint fillOf(StackPane node) {
        Background background = node.getBackground();
        assertNotNull(background);
        return background.getFills().get(0).getFill();
    }

    @Test
    void theFrameHasTheBackgroundOutlineAndCornersOfAnEmptyHotbarSlot() {
        FxToolkitSupport.onFx(() -> {
            Canvas canvas = new Canvas(300, 180);
            MinimapFrame frame = new MinimapFrame(canvas);
            StackPane slot = new StackPane();
            slot.getStyleClass().addAll(UiTheme.SLOT, UiTheme.SLOT_EMPTY);
            Pane root = new Pane(frame, slot);
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(UiTheme.class.getResource(UiTheme.STYLESHEET_PATH).toExternalForm());
            root.applyCss();
            root.layout();

            assertEquals(fillOf(slot), fillOf(frame), "the same background colour");
            assertTrue(fillOf(frame) instanceof Color);
            assertEquals(slot.getBackground().getFills().get(0).getRadii(),
                    frame.getBackground().getFills().get(0).getRadii(), "the same rounded corners");
            assertEquals(slot.getBorder().getStrokes().get(0).getTopStroke(),
                    frame.getBorder().getStrokes().get(0).getTopStroke(), "the same outline colour");
            assertEquals(MinimapFrame.outerWidth(canvas), frame.getWidth(), 1e-9);
            assertEquals(MinimapFrame.outerHeight(canvas), frame.getHeight(), 1e-9);
            return null;
        });
    }

    @Test
    void theCanvasIsClippedToARoundedShape() {
        FxToolkitSupport.onFx(() -> {
            Canvas canvas = new Canvas(300, 180);
            MinimapFrame frame = new MinimapFrame(canvas);

            assertSame(canvas, frame.getChildren().get(0));
            javafx.scene.shape.Rectangle clip = (javafx.scene.shape.Rectangle) canvas.getClip();
            assertEquals(300, clip.getWidth(), 1e-9);
            assertEquals(180, clip.getHeight(), 1e-9);
            assertTrue(clip.getArcWidth() > 0 && clip.getArcHeight() > 0);
            return null;
        });
    }
}
