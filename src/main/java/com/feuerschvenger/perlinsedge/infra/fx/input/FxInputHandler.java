package com.feuerschvenger.perlinsedge.infra.fx.input;

import com.feuerschvenger.perlinsedge.domain.events.IKeyListener;
import com.feuerschvenger.perlinsedge.domain.utils.WheelAccumulator;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Window;
import javafx.util.Subscription;

import java.util.Arrays;

/**
 * Handles input events for the game, including keyboard and mouse interactions.
 * It manages camera zooming and edge detection for screen edges.
 */
public class FxInputHandler {
    private static final int EDGE_SIZE = 30;

    // Indices into the edge flags.
    private static final int EDGE_UP = 0;
    private static final int EDGE_RIGHT = 1;
    private static final int EDGE_DOWN = 2;
    private static final int EDGE_LEFT = 3;
    private static final int EDGE_COUNT = 4;

    private final boolean[] edgeActive; // [0]Up, [1]Right, [2]Down, [3]Left

    private final FxCamera camera;
    private final Canvas mainGameCanvas;

    private IKeyListener keyListener;
    private ScrollInterceptor scrollInterceptor;
    private final WheelAccumulator zoomWheel = new WheelAccumulator();
    private final KeyRepeatFilter keyRepeats = new KeyRepeatFilter();
    /** Set by the press filter for the press that the bubbling handler is about to see. */
    private boolean pressIsFirst;
    /** The subscription to the focus of the window the scene is shown in, or null when there is none. */
    private Subscription focusSubscription;

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
        this.edgeActive = new boolean[EDGE_COUNT];

        // The tracking runs in filters, which see every event before any node can consume it: a control that consumes
        // a release would otherwise leave its key held for good. The delivery to the game stays on the bubbling
        // handler, so a key a control consumed does not also act on the world.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> pressIsFirst = keyRepeats.firstPress(event.getCode()));
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> keyRepeats.release(event.getCode()));
        scene.setOnKeyPressed((KeyEvent event) -> {
            final boolean first = pressIsFirst;
            pressIsFirst = false;
            if (keyListener != null && first) {
                FxKeyMapper.toGameKey(event.getCode(), event.isControlDown(), event.isShiftDown(),
                        event.isAltDown(), event.isMetaDown()).ifPresent(keyListener::onKeyAction);
            }
        });
        scene.windowProperty().addListener((ObservableValue<? extends Window> obs, Window oldWindow, Window newWindow) ->
                moveFocusWatch(newWindow));
        moveFocusWatch(scene.getWindow());

        mainGameCanvas.setOnScroll(this::handleScroll);

        mainGameCanvas.setOnMouseMoved(this::handleMouseMoved);
        mainGameCanvas.setOnMouseExited(this::handleMouseExited);
    }

    /**
     * Test seam: runs what a focus change of the window runs, which forgets the held keys, so the tests can drive a
     * focus change without a window.
     */
    void onFocusChanged() {
        keyRepeats.clear();
    }

    /**
     * Forgets the held keys whenever the window gains or loses focus, since the release events of a key held across
     * the change go to another window. The subscription leaves the window the scene moves away from.
     */
    private void moveFocusWatch(Window newWindow) {
        if (focusSubscription != null) {
            focusSubscription.unsubscribe();
            focusSubscription = null;
        }
        if (newWindow != null) {
            focusSubscription = newWindow.focusedProperty()
                    .subscribe((Boolean wasFocused, Boolean isFocused) -> onFocusChanged());
        }
    }

    public void setGameKeyListener(IKeyListener listener) {
        this.keyListener = listener;
    }

    /**
     * Sets a handler that gets the first chance at every scroll event.
     * It receives the wheel delta and whether Ctrl is held, and returns true when it used the event, which keeps the
     * camera from zooming for it.
     *
     * @param interceptor the handler, or null to let every event zoom
     */
    public void setScrollInterceptor(ScrollInterceptor interceptor) {
        this.scrollInterceptor = interceptor;
    }

    /**
     * Handles mouse scroll events to zoom the camera in or out unless the interceptor used them.
     *
     * @param event The scroll event containing the scroll delta and mouse position.
     */
    private void handleScroll(ScrollEvent event) {
        if (scrollInterceptor != null && Double.compare(Math.abs(event.getDeltaY()), 0.0) != 0
                && scrollInterceptor.intercept(event.getDeltaY(), event.isControlDown())) {
            return;
        }
        int steps = zoomWheel.add(event.getDeltaY());
        if (steps != 0) {
            camera.zoomStep(steps, event.getX(), event.getY(), mainGameCanvas);
        }
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

        edgeActive[EDGE_UP] = y < EDGE_SIZE;
        edgeActive[EDGE_RIGHT] = x > width - EDGE_SIZE;
        edgeActive[EDGE_DOWN] = y > height - EDGE_SIZE;
        edgeActive[EDGE_LEFT] = x < EDGE_SIZE;
    }

    /**
     * Handles mouse exit events to reset edge detection.
     *
     * @param e The mouse event when the mouse exits the canvas.
     */
    private void handleMouseExited(MouseEvent e) {
        Arrays.fill(edgeActive, false);
    }

    /**
     * Returns a snapshot of which screen edges the mouse is on.
     *
     * @return a copy of the flags, in the order up, right, down, left
     */
    public boolean[] getEdgeActive() {
        return edgeActive.clone();
    }

}
