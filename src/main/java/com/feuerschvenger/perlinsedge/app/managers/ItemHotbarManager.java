package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.infra.fx.input.WheelAccumulator;

/**
 * Chooses which hotbar slot the player holds in hand in exploration mode: the number keys 1 to 9 pick a slot and the
 * mouse wheel moves along the slots. Pressing the number of the slot that is already held uses its item. The building
 * hotbar answers the same keys in building mode, see {@link HotbarManager}.
 */
public class ItemHotbarManager {
    private final GameStateManager gameState;
    private final Runnable useActiveItem;
    private final WheelAccumulator wheel = new WheelAccumulator();

    /**
     * Creates the manager.
     *
     * @param gameState     shared game state
     * @param useActiveItem runs when the number of the slot already held is pressed
     */
    public ItemHotbarManager(GameStateManager gameState, Runnable useActiveItem) {
        this.gameState = gameState;
        this.useActiveItem = useActiveItem;
    }

    /**
     * Handles a number key.
     *
     * @param number the key number from 1 to 9
     * @return true when the key belonged to the item hotbar, which is the case in exploration mode while the game runs
     *         and the player lives; false leaves the key to the building hotbar
     */
    public boolean handleNumberKey(int number) {
        if (!isActive() || number < 1 || number > Player.HOTBAR_SIZE) {
            return false;
        }
        final Player player = gameState.getPlayer();
        final int index = number - 1;
        if (player.getSelectedHotbarIndex() == index) {
            useActiveItem.run();
        } else {
            player.selectHotbarSlot(index);
        }
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
            // Turning the wheel up goes to the slot on the left, like the building hotbar pages
            gameState.getPlayer().cycleHotbar(-steps);
        }
        return true;
    }

    private boolean isActive() {
        return !gameState.isGameplayInputBlocked()
                && !gameState.isGamePaused()
                && gameState.getCurrentGameMode() == GameMode.EXPLORATION;
    }
}
