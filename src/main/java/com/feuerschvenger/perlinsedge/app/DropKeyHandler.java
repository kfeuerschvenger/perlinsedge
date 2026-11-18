package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.ItemDropManager;
import com.feuerschvenger.perlinsedge.domain.entities.containers.SlotRef;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Turns the drop keys into throws: Q throws one item and Ctrl+Q throws the whole stack. With an inventory open the
 * item comes from the slot the mouse is over, or from the selected slot when the mouse is over none. While exploring it
 * comes from the hotbar slot held in hand. In building mode the keys do nothing, and they never act while the game is
 * paused or the player is dead. With an inventory open they are also ignored while a stack is in hand, because the
 * stack in hand is thrown with the right click or by releasing the mouse outside the panel.
 */
@Slf4j
public class DropKeyHandler {
    private final GameStateManager gameState;
    private final Supplier<SlotRef> targetSlot;
    private final BooleanSupplier stackInHand;
    private final ItemDropManager drops;

    /**
     * Creates the handler.
     *
     * @param gameState   shared game state
     * @param targetSlot  gives the slot to throw from, or null when there is none
     * @param stackInHand tells whether a stack is being dragged
     * @param drops       throws the items into the world
     */
    public DropKeyHandler(GameStateManager gameState, Supplier<SlotRef> targetSlot, BooleanSupplier stackInHand,
                          ItemDropManager drops) {
        this.gameState = gameState;
        this.targetSlot = targetSlot;
        this.stackInHand = stackInHand;
        this.drops = drops;
    }

    /** Handles Q: throws one item. */
    public void dropOne() {
        drop(false);
    }

    /** Handles Ctrl+Q: throws the whole stack. */
    public void dropStack() {
        drop(true);
    }

    private void drop(boolean wholeStack) {
        if (gameState.isPlayerDead() || gameState.isGamePaused()) {
            log.debug("DROP KEY: ignored, the player is dead or the game is paused");
            return;
        }
        final var mode = gameState.getCurrentGameMode();
        if (mode == GameMode.EXPLORATION) {
            dropFromHand(wholeStack);
            return;
        }
        if (!isInventoryMode(mode)) {
            log.debug("DROP KEY: ignored, neither exploring nor in an inventory");
            return;
        }
        if (stackInHand.getAsBoolean()) {
            log.debug("DROP KEY: ignored, a stack is in hand");
            return;
        }
        SlotRef slot = targetSlot.get();
        if (slot == null) {
            log.debug("DROP KEY: no slot is hovered or selected");
            return;
        }
        boolean thrown = drops.dropFromSlot(slot.container(), slot.slot(), wholeStack);
        log.debug("DROP KEY: slot {} {}", slot.slot(), thrown ? (wholeStack ? "stack thrown" : "one thrown") : "empty");
    }

    /** Throws from the hotbar slot held in hand, which is the only slot the player can see with no inventory open. */
    private void dropFromHand(boolean wholeStack) {
        final var player = gameState.getPlayer();
        if (player == null) {
            return;
        }
        final int slot = player.getSelectedHotbarIndex();
        boolean thrown = drops.dropFromSlot(player, slot, wholeStack);
        log.debug("DROP KEY: hand slot {} {}", slot, thrown ? (wholeStack ? "stack thrown" : "one thrown") : "empty");
    }

    private static boolean isInventoryMode(GameMode mode) {
        return mode == GameMode.INVENTORY_OPEN || mode == GameMode.CRAFTING_OPEN;
    }

}
