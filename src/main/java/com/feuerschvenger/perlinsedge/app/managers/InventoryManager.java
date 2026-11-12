package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Crafts from the player inventory.
 * A craft either happens completely, consuming the ingredients and adding the result, or changes nothing and tells
 * the player why with a short hint.
 */
@Slf4j
public class InventoryManager {
    /** Hint shown when the inventory lacks an ingredient. */
    static final String MISSING_INGREDIENTS_HINT = "Missing ingredients";
    /** Hint shown when the result would not fit into the inventory. */
    static final String NO_ROOM_HINT = "Inventory full";

    private final GameStateManager gameState;
    private final Runnable onInventoryChanged;
    private final Consumer<String> hint;

    /**
     * Creates the manager.
     *
     * @param gameState          player state access
     * @param onInventoryChanged called after a craft changed the inventory, so the views are redrawn
     * @param hint               shows a short message to the player when a craft is refused
     */
    public InventoryManager(GameStateManager gameState, Runnable onInventoryChanged, Consumer<String> hint) {
        this.gameState = gameState;
        this.onInventoryChanged = onInventoryChanged;
        this.hint = hint;
    }

    /**
     * Attempts to craft an item based on the specified recipe.
     * Validates the ingredients and the room for the result, consumes the ingredients and adds the result.
     *
     * @param recipe Recipe to craft, ignored when null
     * @return true when the craft happened
     */
    public boolean handleCraftRecipe(Recipe recipe) {
        if (recipe == null) {
            log.warn("Cannot craft: Recipe is null");
            return false;
        }

        Player player = gameState.getPlayer();
        if (player == null) {
            log.warn("Crafting failed: Player is null");
            return false;
        }
        if (player.isDead()) {
            return false;
        }

        if (!player.canCraft(recipe)) {
            log.debug("CRAFT refused: missing ingredients for {}", recipe.getName());
            hint.accept(MISSING_INGREDIENTS_HINT);
            return false;
        }
        if (!player.craftItem(recipe)) {
            log.debug("CRAFT refused: no room for the output of {}", recipe.getName());
            hint.accept(NO_ROOM_HINT);
            return false;
        }

        log.debug("CRAFT done: {}", recipe.getName());
        onInventoryChanged.run();
        return true;
    }

}
