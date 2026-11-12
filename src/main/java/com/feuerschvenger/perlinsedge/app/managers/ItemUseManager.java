package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.DamageNumbers;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Uses the item held in hand: a health potion, cooked meat or raw meat heals the player and is used up, one at a time.
 * Using does nothing while the player is dead, the game is paused or any mode but exploration is on, which covers
 * both inventories and building mode. At full health nothing is used up and a hint says so.
 */
@Slf4j
public class ItemUseManager {
    /** Hint shown when the player tries to heal at full health. */
    static final String FULL_HEALTH_HINT = "Already at full health";

    private final GameStateManager gameState;
    private final Runnable onInventoryChanged;
    private final Consumer<String> hintSink;

    /**
     * Creates the manager.
     *
     * @param gameState          shared game state
     * @param onInventoryChanged called after an item was used up, so the views are redrawn
     * @param hintSink           receives the short message shown when an item is refused
     */
    public ItemUseManager(GameStateManager gameState, Runnable onInventoryChanged, Consumer<String> hintSink) {
        this.gameState = gameState;
        this.onInventoryChanged = onInventoryChanged;
        this.hintSink = hintSink;
    }

    /**
     * Uses one item of the stack held in hand.
     *
     * @return true when an item was used up, false when nothing happened: no player or a dead one, the game is paused
     *         or not in exploration mode, the hand is empty or holds something that cannot be used, or the player is
     *         already at full health
     */
    public boolean useActiveItem() {
        if (gameState.isGameplayInputBlocked() || gameState.isGamePaused()
                || gameState.getCurrentGameMode() != GameMode.EXPLORATION) {
            return false;
        }
        final Player player = gameState.getPlayer();
        final ItemType type = player.getActiveItemType();
        if (type == null || !type.isConsumable()) {
            return false;
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            hintSink.accept(FULL_HEALTH_HINT);
            return false;
        }
        final double restored = player.heal(type.getHealAmount());
        gameState.getDamageNumbers().spawn(DamageNumbers.Kind.HEAL, restored, player.getPosX(), player.getPosY(),
                gameState.getGameTime());
        player.consumeActiveItem();
        onInventoryChanged.run();
        log.debug("Used {}: restored {} health", type, restored);
        return true;
    }
}
