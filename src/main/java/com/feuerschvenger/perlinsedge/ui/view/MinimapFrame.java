package com.feuerschvenger.perlinsedge.ui.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Frames the minimap canvas with the look of a hotbar slot: the same background colour, outline and rounded corners,
 * taken from the same style classes. A canvas draws no background of its own, so this pane supplies it, and the canvas
 * is clipped to the rounded shape inside the outline.
 */
public class MinimapFrame extends StackPane {
    /** Width of the outline the slot style draws, which the frame needs room for around the canvas. */
    public static final double BORDER_WIDTH = 1;
    /** Corner radius of the slot style, less the outline, which is the radius of the shape the canvas is clipped to. */
    private static final double CLIP_RADIUS = 5;

    /**
     * Wraps a canvas.
     *
     * @param canvas the minimap canvas
     */
    public MinimapFrame(Canvas canvas) {
        getStyleClass().addAll(UiTheme.SLOT, UiTheme.SLOT_EMPTY);
        final Rectangle clip = new Rectangle(canvas.getWidth(), canvas.getHeight());
        clip.setArcWidth(CLIP_RADIUS * 2);
        clip.setArcHeight(CLIP_RADIUS * 2);
        canvas.setClip(clip);
        getChildren().add(canvas);
    }

    /**
     * Gives the width the frame takes once laid out, for placing it before the first layout pass.
     *
     * @param canvas the framed canvas
     * @return the canvas width plus the outline on both sides
     */
    public static double outerWidth(Canvas canvas) {
        return canvas.getWidth() + BORDER_WIDTH * 2;
    }

    /**
     * Gives the height the frame takes once laid out, for placing it before the first layout pass.
     *
     * @param canvas the framed canvas
     * @return the canvas height plus the outline on both sides
     */
    public static double outerHeight(Canvas canvas) {
        return canvas.getHeight() + BORDER_WIDTH * 2;
    }
}
