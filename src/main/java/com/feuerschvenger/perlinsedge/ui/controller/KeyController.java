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

    AppConfig.DebugConfig debug = AppConfig.getInstance().debug();

    private final Runnable togglePauseMenuAction;
    private final Runnable centerCameraAction;
    private final Runnable generateNewMapAction;
    private final Runnable toggleMapConfigPanelAction;
    private final Runnable toggleInventoryPanelAction;
    private final Runnable onAttack;
    private final Runnable toggleBuildingModeAction;
    private final Consumer<Integer> onNumberKeyPress;
    private final Consumer<Integer> onHotbarPageChange;
    private BooleanSupplier inputBlocked;

    /**
     * Constructs a KeyController with the specified actions for various key events.
     *
     * @param togglePauseMenuAction Action to toggle the pause menu.
     * @param centerCameraAction Action to center the camera.
     * @param generateNewMapAction Action to generate a new random map.
     * @param toggleMapConfigPanelAction Action to toggle the map configuration panel.
     * @param toggleInventoryPanelAction Action to toggle the inventory panel.
     * @param onAttack Action for player attack.
     * @param toggleBuildingModeAction Action to toggle the building mode.
     * @param onNumberKeyPress Action to handle number key presses for hotbar selection.
     * @param onHotbarPageChange Action to switch the building hotbar page, receives -1 for previous and 1 for next.
     */
    public KeyController(Runnable togglePauseMenuAction,
                         Runnable centerCameraAction,
                         Runnable generateNewMapAction,
                         Runnable toggleMapConfigPanelAction,
                         Runnable toggleInventoryPanelAction,
                         Runnable onAttack,
                         Runnable toggleBuildingModeAction,
                         Consumer<Integer> onNumberKeyPress,
                         Consumer<Integer> onHotbarPageChange) {
        this.togglePauseMenuAction = togglePauseMenuAction;
        this.centerCameraAction = centerCameraAction;
        this.generateNewMapAction = generateNewMapAction;
        this.toggleMapConfigPanelAction = toggleMapConfigPanelAction;
        this.toggleInventoryPanelAction = toggleInventoryPanelAction;
        this.onAttack = onAttack;
        this.toggleBuildingModeAction = toggleBuildingModeAction;
        this.onNumberKeyPress = onNumberKeyPress;
        this.onHotbarPageChange = onHotbarPageChange;
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

            // ------------- Game keys --------------

            case ESCAPE:
                if (togglePauseMenuAction != null) togglePauseMenuAction.run();
                break;
            case SPACE:
                if (onAttack != null) onAttack.run();
                break;
            case I:
                if (toggleInventoryPanelAction != null) toggleInventoryPanelAction.run();
                break;
            case B:
                if (toggleBuildingModeAction != null) toggleBuildingModeAction.run();
                break;
            case C:
                if (centerCameraAction != null) centerCameraAction.run();
                break;
            case PAGE_UP:
                if (onHotbarPageChange != null) onHotbarPageChange.accept(-1);
                break;
            case PAGE_DOWN:
                if (onHotbarPageChange != null) onHotbarPageChange.accept(1);
                break;

            // ----------- Debugging keys -----------

            case F1:
                toggleDebugMode();
                break;
            case F2:
                toggleGrayScaleMode();
                break;
            case F3:
                toggleDebugText();
                break;
            case F4:
                toggleMapConfigPanelAction();
                break;
            case F5:
                generateNewRandomMap();
                break;
            case F6:
                toggleFpsOverlay();
                break;
            default:
                break;
        }
    }

    /** Tells whether a key acts on the world, as opposed to the debug and menu keys. */
    private static boolean isGameplayKey(GameKey key) {
        return switch (key) {
            case SPACE, I, B, PAGE_UP, PAGE_DOWN -> true;
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
     * Toggles the FPS overlay. It is off by default and works without the debug mode.
     */
    private void toggleFpsOverlay() {
        boolean newState = !debug.isFpsOverlayEnabled();
        debug.setFpsOverlayEnabled(newState);
        log.info("FPS Overlay: {}", newState ? "ON" : "OFF");
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
     * Toggles the map configuration panel.
     * If debug mode is not enabled, this action will not take effect.
     */
    private void toggleMapConfigPanelAction() {
        if (debug.isEnabled() && toggleMapConfigPanelAction != null) {
            toggleMapConfigPanelAction.run();
        }
    }

    /**
     * Generates a new random map.
     * This action is only available when debug mode is enabled.
     */
    private void generateNewRandomMap() {
        if (debug.isEnabled() && generateNewMapAction != null) {
            generateNewMapAction.run();
        }
    }

}
