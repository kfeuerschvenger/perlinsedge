package com.feuerschvenger.perlinsedge.ui.controller;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.events.IKeyListener;
import javafx.scene.input.KeyCode;

import java.util.function.Consumer;

/**
 * KeyController handles keyboard input for the game, allowing players to interact with the game world
 * and access various debug features.
 * Implements IKeyListener to respond to keyboard events.
 */
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
     */
    public KeyController(Runnable togglePauseMenuAction,
                         Runnable centerCameraAction,
                         Runnable generateNewMapAction,
                         Runnable toggleMapConfigPanelAction,
                         Runnable toggleInventoryPanelAction,
                         Runnable onAttack,
                         Runnable toggleBuildingModeAction,
                         Consumer<Integer> onNumberKeyPress) {
        this.togglePauseMenuAction = togglePauseMenuAction;
        this.centerCameraAction = centerCameraAction;
        this.generateNewMapAction = generateNewMapAction;
        this.toggleMapConfigPanelAction = toggleMapConfigPanelAction;
        this.toggleInventoryPanelAction = toggleInventoryPanelAction;
        this.onAttack = onAttack;
        this.toggleBuildingModeAction = toggleBuildingModeAction;
        this.onNumberKeyPress = onNumberKeyPress;
    }

    @Override
    public void onKeyAction(KeyCode code) {

        // --------------------------------------
        // Handle number keys for hotbar selection (1-9)
        // --------------------------------------

        if (code.isDigitKey()) {
            try {
                int number = Integer.parseInt(code.getChar());
                if (number >= 1 && number <= 9) {
                    if (onNumberKeyPress != null) {
                        onNumberKeyPress.accept(number);
                    }
                    return;
                }
            } catch (NumberFormatException ignored) { }
        }

        switch (code) {

            // --------------------------------------
            // ------------- Game keys --------------
            // --------------------------------------

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

            // --------------------------------------
            // ----------- Debugging keys -----------
            // --------------------------------------

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
        }
    }

    private void toggleDebugMode() {
        boolean newDebugState = !debug.isEnabled();
        debug.setEnabled(newDebugState);
        System.out.println("Debug Mode: " + (newDebugState ? "ON" : "OFF"));
    }

    private void toggleGrayScaleMode() {
        boolean newGrayScaleState = !debug.isGrayscaleEnabled();
        debug.setGrayscaleEnabled(newGrayScaleState);
        System.out.println("Gray Scale Mode: " + (newGrayScaleState ? "ENABLED" : "DISABLED"));
    }

    private void toggleDebugText() {
        if (!debug.isGrayscaleEnabled()) {
            return;
        }
        boolean newDebugTextState = !debug.isTextEnabled();
        debug.setTextEnabled(newDebugTextState);
        System.out.println("Debug Text: " + (newDebugTextState ? "VISIBLE" : "HIDDEN"));
    }

    private void toggleMapConfigPanelAction() {
        if (debug.isEnabled() && toggleMapConfigPanelAction != null) {
            toggleMapConfigPanelAction.run();
        }
    }

    private void generateNewRandomMap() {
        if (debug.isEnabled() && generateNewMapAction != null) {
            generateNewMapAction.run();
        }
    }

}