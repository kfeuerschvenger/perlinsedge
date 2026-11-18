package com.feuerschvenger.perlinsedge.ui.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Frames the minimap canvas with the look of a hotbar slot: the same background colour, outline and rounded corners,
 * taken from the same style classes. A canvas draws no background of its own, so this pane supplies it, and the canvas
 * is clipped to the rounded shape inside the outline.
 */
public final class MinimapFrame extends StackPane {
    /** Width of the outline the slot style draws, which the frame needs room for around the canvas. */
    public static final double BORDER_WIDTH = 1;
    /** Sides of the frame that carry the outline: the outline is taken from both sides of the canvas. */
    private static final int BORDER_SIDES = 2;
    /** Corner arc diameter of the shape the canvas is clipped to: twice the corner radius of the slot style, less the outline. */
    private static final double CLIP_ARC = 10;

    /**
     * Wraps a canvas.
     *
     * @param canvas the minimap canvas
     */
    public MinimapFrame(Canvas canvas) {
        getStyleClass().addAll(UiTheme.SLOT, UiTheme.SLOT_EMPTY);
        final var clip = new Rectangle(canvas.getWidth(), canvas.getHeight());
        clip.setArcWidth(CLIP_ARC);
        clip.setArcHeight(CLIP_ARC);
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
        return canvas.getWidth() + BORDER_WIDTH * BORDER_SIDES;
    }

    /**
     * Gives the height the frame takes once laid out, for placing it before the first layout pass.
     *
     * @param canvas the framed canvas
     * @return the canvas height plus the outline on both sides
     */
    public static double outerHeight(Canvas canvas) {
        return canvas.getHeight() + BORDER_WIDTH * BORDER_SIDES;
    }
}
