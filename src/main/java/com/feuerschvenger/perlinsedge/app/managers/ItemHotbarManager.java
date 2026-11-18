package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.utils.WheelAccumulator;

/**
 * Chooses which hotbar slot the player holds in hand: the number keys 1 to 9 pick a slot and the
 * mouse wheel moves along the slots, in exploration and in build mode, where picking a slot other than the hammer leaves
 * the build mode. Choosing a slot never uses its item: that has a key of its own, see
 * {@link ItemUseManager}.
 */
public class ItemHotbarManager {
    private final GameStateManager gameState;
    private final WheelAccumulator wheel = new WheelAccumulator();

    /**
     * Creates the manager.
     *
     * @param gameState shared game state
     */
    public ItemHotbarManager(GameStateManager gameState) {
        this.gameState = gameState;
    }

    /**
     * Handles a number key.
     *
     * @param number the key number from 1 to 9
     * @return true when the key belonged to the item hotbar, which is the case in exploration and build mode while the game runs
     *         and the player lives; false otherwise
     */
    public boolean handleNumberKey(int number) {
        if (!isActive() || number < 1 || number > Player.HOTBAR_SIZE) {
            return false;
        }
        gameState.getPlayer().selectHotbarSlot(number - 1);
        return true;
    }

    /**
     * Moves the hand along the hotbar with the mouse wheel. The wheel deltas are accumulated so that one gesture on a
     * touchpad or an inertial wheel moves one slot per {@link WheelAccumulator#STEP_THRESHOLD} units, and the hand wraps
     * around at both ends. Ctrl with the wheel is left to the camera, which zooms.
     *
     * @param deltaY   the vertical wheel delta, positive when the wheel turns up or away
     * @param ctrlDown true when Ctrl is held
     * @return true when the event belongs to the hotbar and must not zoom the camera
     */
    public boolean handleWheel(double deltaY, boolean ctrlDown) {
        if (ctrlDown || !isActive()) {
            wheel.reset();
            return false;
        }
        final int steps = wheel.add(deltaY);
        if (steps != 0) {
            // Turning the wheel up goes to the slot on the left
            gameState.getPlayer().cycleHotbar(-steps);
        }
        return true;
    }

    private boolean isActive() {
        return !gameState.isGameplayInputBlocked()
                && !gameState.isGamePaused()
                && (gameState.getCurrentGameMode() == GameMode.EXPLORATION
                || gameState.getCurrentGameMode() == GameMode.BUILDING_MODE);
    }
}
