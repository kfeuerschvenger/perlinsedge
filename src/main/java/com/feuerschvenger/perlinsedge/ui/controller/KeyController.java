package com.feuerschvenger.perlinsedge.ui.controller;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.events.GameKey;
import com.feuerschvenger.perlinsedge.domain.events.IKeyListener;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * KeyController handles keyboard input for the game, allowing players to interact with the game world
 * and access various debug features.
 * Implements IKeyListener to respond to keyboard events.
 */
@Slf4j
public class KeyController implements IKeyListener {

    private final AppConfig.DebugConfig debug = AppConfig.getInstance().debug();

    private final Runnable togglePauseMenuAction;
    private final Runnable centerCameraAction;
    private final Runnable generateNewMapAction;
    private final Runnable toggleInventoryPanelAction;
    private final Consumer<Integer> onNumberKeyPress;
    private BooleanSupplier inputBlocked;
    private Runnable onDropOne;
    private Runnable onDropStack;
    private Runnable onUseItem;
    private Runnable onRotate;

    /**
     * Constructs a KeyController with the specified actions for various key events.
     *
     * @param togglePauseMenuAction Action run by Escape: drops the selected building piece while building, otherwise toggles the pause menu.
     * @param centerCameraAction Action to center the camera, run by the space bar.
     * @param generateNewMapAction Action to generate a new random map.
     * @param toggleInventoryPanelAction Action to toggle the inventory panel.
     * @param onNumberKeyPress Action to handle number key presses for hotbar selection.
     */
    public KeyController(Runnable togglePauseMenuAction,
                         Runnable centerCameraAction,
                         Runnable generateNewMapAction,
                         Runnable toggleInventoryPanelAction,
                         Consumer<Integer> onNumberKeyPress) {
        this.togglePauseMenuAction = togglePauseMenuAction;
        this.centerCameraAction = centerCameraAction;
        this.generateNewMapAction = generateNewMapAction;
        this.toggleInventoryPanelAction = toggleInventoryPanelAction;
        this.onNumberKeyPress = onNumberKeyPress;
    }

    /**
     * Sets the condition under which gameplay keys are ignored. Pause, centring the camera and the debug keys stay
     * available.
     *
     * @param inputBlocked true while gameplay input must be ignored, null never blocks
     */
    public void setInputBlocked(BooleanSupplier inputBlocked) {
        this.inputBlocked = inputBlocked;
    }

    /**
     * Sets the actions of the drop keys. They are gameplay keys, so they are ignored while input is blocked.
     *
     * @param onDropOne   action for Q, which throws one item
     * @param onDropStack action for Ctrl+Q, which throws the whole stack
     */
    public void setDropActions(Runnable onDropOne, Runnable onDropStack) {
        this.onDropOne = onDropOne;
        this.onDropStack = onDropStack;
    }

    /**
     * Sets the action of the use key. It is a gameplay key, so it is ignored while input is blocked.
     *
     * @param onUseItem action for E, which uses the item held in hand
     */
    public void setUseAction(Runnable onUseItem) {
        this.onUseItem = onUseItem;
    }

    /**
     * Sets the action of the rotate key. It is a gameplay key, so it is ignored while input is blocked.
     *
     * @param onRotate action for R, which turns the selected building piece
     */
    public void setRotateAction(Runnable onRotate) {
        this.onRotate = onRotate;
    }

    /** Tells whether a panel that covers the world currently takes the gameplay keys. */
    private boolean isGameplayBlocked() {
        return inputBlocked != null && inputBlocked.getAsBoolean();
    }

    @Override
    public void onKeyAction(GameKey key) {

        if (isGameplayBlocked() && (key.isDigit() || isGameplayKey(key))) {
            return;
        }

        // Number keys select hotbar slots 1 to 9
        if (key.isDigit()) {
            if (onNumberKeyPress != null) {
                onNumberKeyPress.accept(key.getDigit());
            }
            return;
        }

        switch (key) {
            case ESCAPE, SPACE, I, DROP_ONE, DROP_STACK, ROTATE, USE_ITEM -> runIfSet(gameAction(key));
            default -> runDebugKey(key);
        }
    }

    /** Gives the handler of a game key, which is null while none is set. */
    private Runnable gameAction(GameKey key) {
        return switch (key) {
            case ESCAPE -> togglePauseMenuAction;
            case SPACE -> centerCameraAction;
            case I -> toggleInventoryPanelAction;
            case DROP_ONE -> onDropOne;
            case DROP_STACK -> onDropStack;
            case ROTATE -> onRotate;
            case USE_ITEM -> onUseItem;
            default -> null;
        };
    }

    /** Runs what a debugging key does. */
    private void runDebugKey(GameKey key) {
        switch (key) {
            case F1 -> toggleDebugMode();
            case F2 -> toggleGrayScaleMode();
            case F3 -> toggleDebugText();
            case F4 -> generateNewRandomMap();
            case F5 -> toggleFastTime();
            default -> { /* the other keys have nothing to do with debugging */ }
        }
    }

    /** Runs an action that a key may have no handler for. */
    private static void runIfSet(Runnable action) {
        if (action != null) {
            action.run();
        }
    }

    /** Tells whether a key acts on the world, as opposed to the debug and menu keys. */
    private static boolean isGameplayKey(GameKey key) {
        return switch (key) {
            case I, ROTATE, DROP_ONE, DROP_STACK, USE_ITEM -> true;
            default -> false;
        };
    }

    /**
     * Toggles the debug mode on or off.
     */
    private void toggleDebugMode() {
        boolean newDebugState = !debug.isEnabled();
        debug.setEnabled(newDebugState);
        log.info("Debug Mode: {}", newDebugState ? "ON" : "OFF");
    }

    /**
     * Toggles the fast time of the day cycle, which makes a whole day pass in about forty real seconds.
     */
    private void toggleFastTime() {
        final AppConfig.DayCycleConfig dayCycle = AppConfig.getInstance().dayCycle();
        final boolean newState = !dayCycle.isFastTime();
        dayCycle.setFastTime(newState);
        log.info("Fast time: {}", newState ? "ON" : "OFF");
    }

    /**
     * Toggles the gray scale mode for the debug view.
     * If debug mode is not enabled, this action will not take effect.
     */
    private void toggleGrayScaleMode() {
        boolean newGrayScaleState = !debug.isGrayscaleEnabled();
        debug.setGrayscaleEnabled(newGrayScaleState);
        log.info("Gray Scale Mode: {}", newGrayScaleState ? "ENABLED" : "DISABLED");
    }

    /**
     * Toggles the visibility of debug text.
     * If gray scale mode is not enabled, this action will not take effect.
     */
    private void toggleDebugText() {
        if (!debug.isGrayscaleEnabled()) {
            return;
        }
        boolean newDebugTextState = !debug.isTextEnabled();
        debug.setTextEnabled(newDebugTextState);
        log.info("Debug Text: {}", newDebugTextState ? "VISIBLE" : "HIDDEN");
    }

    /**
     * Generates a new random map. This is the F4 key.
     * This action is only available when debug mode is enabled. The map is replaced inside the same session, so the
     * time of day, the day number and the fast time carry over to the new map.
     */
    private void generateNewRandomMap() {
        if (debug.isEnabled() && generateNewMapAction != null) {
            generateNewMapAction.run();
        }
    }

}
