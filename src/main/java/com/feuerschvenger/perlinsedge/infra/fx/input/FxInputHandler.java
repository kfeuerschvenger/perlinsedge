package com.feuerschvenger.perlinsedge.infra.fx.input;

import com.feuerschvenger.perlinsedge.domain.events.IKeyListener;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.Arrays;

/**
 * Handles input events for the game, including keyboard and mouse interactions.
 * It manages camera zooming and edge detection for screen edges.
 */
public class FxInputHandler {
    private final static int EDGE_SIZE = 30;

    private final boolean[] edgeActive; // [0]Up, [1]Right, [2]Down, [3]Left

    private final FxCamera camera;
    private final Canvas mainGameCanvas;

    private IKeyListener keyListener;

    /**
     * Constructs a new FxInputHandler for handling input events in the game.
     *
     * @param scene The JavaFX scene to attach input listeners to.
     * @param mainGameCanvas The canvas where the game is rendered.
     * @param camera The camera used for rendering the game.
     */
    public FxInputHandler(Scene scene, Canvas mainGameCanvas, FxCamera camera) {
        this.mainGameCanvas = mainGameCanvas;
        this.camera = camera;
        this.edgeActive = new boolean[4];

        scene.setOnKeyPressed(event -> {
            if (keyListener != null) {
                keyListener.onKeyAction(event.getCode());
            }
        });

        mainGameCanvas.setOnScroll(this::handleScroll);

        mainGameCanvas.setOnMouseMoved(this::handleMouseMoved);
        mainGameCanvas.setOnMouseExited(this::handleMouseExited);
    }

    public void setGameKeyListener(IKeyListener listener) {
        this.keyListener = listener;
    }

    /**
     * Handles mouse scroll events to zoom the camera in or out.
     *
     * @param event The scroll event containing the scroll delta and mouse position.
     */
    private void handleScroll(ScrollEvent event) {
        double zoomFactor = event.getDeltaY() > 0 ? 1.05 : 0.95;
        camera.zoom(zoomFactor, event.getX(), event.getY());
    }

    /**
     * Handles mouse movement events to detect if the mouse is near the edges of the canvas.
     *
     * @param e The mouse event containing the mouse position.
     */
    private void handleMouseMoved(MouseEvent e) {
        double width = mainGameCanvas.getWidth();
        double height = mainGameCanvas.getHeight();

        double x = e.getX();
        double y = e.getY();

        edgeActive[0] = y < EDGE_SIZE; // Up
        edgeActive[1] = x > width - EDGE_SIZE; // Right
        edgeActive[2] = y > height - EDGE_SIZE; // Down
        edgeActive[3] = x < EDGE_SIZE; // Left
    }

    /**
     * Handles mouse exit events to reset edge detection.
     *
     * @param e The mouse event when the mouse exits the canvas.
     */
    private void handleMouseExited(MouseEvent e) {
        Arrays.fill(edgeActive, false);
    }

    public boolean[] getEdgeActive() {
        return edgeActive;
    }

}