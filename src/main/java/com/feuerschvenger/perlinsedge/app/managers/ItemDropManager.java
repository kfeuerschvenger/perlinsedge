package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.DropMerger;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemThrow;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Manages dropped items in the game world: the player throwing items out of a container, merging of nearby piles,
 * pickup and despawning.
 * Every time is read from the game clock of the {@link GameStateManager}, which stands still while the game is paused.
 */
@Slf4j
public class ItemDropManager {
    private final GameStateManager gameState;
    private final Runnable onInventoryChanged;

    /**
     * Creates the manager.
     *
     * @param gameState          shared game state
     * @param onInventoryChanged called after the content of a container changed, so the views are redrawn
     */
    public ItemDropManager(GameStateManager gameState, Runnable onInventoryChanged) {
        this.gameState = gameState;
        this.onInventoryChanged = onInventoryChanged;
    }

    /**
     * Throws items out of a slot of a container into the world.
     * Does nothing when the slot is empty or the player is missing or dead. A thrown stack lands up to two tiles away
     * in the direction the player faces, see {@link ItemThrow}, and merges into the piles of the same type nearby.
     *
     * @param container  container that holds the stack: the player inventory or an open chest
     * @param slot       slot of the stack
     * @param wholeStack true to throw the whole stack, false to throw one item
     * @return true when something was thrown
     */
    public boolean dropFromSlot(Container container, int slot, boolean wholeStack) {
        Player player = gameState.getPlayer();
        if (player == null || player.isDead() || container == null) {
            return false;
        }
        Item stack = container.getItemAt(slot);
        if (stack == null || stack.getQuantity() <= 0) {
            log.debug("DROP: slot {} is empty", slot);
            return false;
        }

        int amount = wholeStack ? stack.getQuantity() : 1;
        if (amount >= stack.getQuantity()) {
            container.setItemAt(slot, null);
        } else {
            stack.removeQuantity(amount);
        }
        throwItem(new Item(stack.getType(), amount), player);
        onInventoryChanged.run();
        return true;
    }

    /**
     * Throws an item from the player into the world.
     *
     * @param item   the thrown units, it is not stored itself
     * @param player the thrower, whose tile and facing direction decide where the item lands
     */
    public void throwItem(Item item, Player player) {
        int fromX = player.getCurrentTileX();
        int fromY = player.getCurrentTileY();
        int[] landing = ItemThrow.landingTile(gameState.getCurrentMap(), fromX, fromY, player.getDirection());
        double now = gameState.getGameTime();

        int remaining = DropMerger.mergeInto(gameState.getDroppedItems(), item.getType(), item.getQuantity(),
                landing[0], landing[1], now);
        if (remaining > 0) {
            gameState.getDroppedItems().add(new DroppedItem(fromX, fromY, landing[0], landing[1],
                    new Item(item.getType(), remaining), now, DroppedItem.DropSource.PLAYER));
        }
        log.debug("DROP: threw {}x {} from ({}, {}) to ({}, {}), {} not merged",
                item.getQuantity(), item.getDisplayName(), fromX, fromY, landing[0], landing[1], remaining);
    }

    /**
     * Updates all dropped item operations: despawning and pickup checks.
     */
    public void update() {
        updateDespawning();
        checkItemPickup();
    }

    private void updateDespawning() {
        double currentTime = gameState.getGameTime();
        Iterator<DroppedItem> iterator = gameState.getDroppedItems().iterator();

        while (iterator.hasNext()) {
            DroppedItem item = iterator.next();

            if (item.isExpired(currentTime)) {
                iterator.remove();
                logDespawn(item, currentTime);
            }
        }
    }

    private void checkItemPickup() {
        Player player = gameState.getPlayer();
        if (player == null || player.isDead()) return;

        List<DroppedItem> droppedItems = gameState.getDroppedItems();
        Iterator<DroppedItem> iterator = droppedItems.iterator();
        int playerX = player.getCurrentTileX();
        int playerY = player.getCurrentTileY();

        while (iterator.hasNext()) {
            DroppedItem item = iterator.next();

            if (item.canBePickedUp(playerX, playerY, gameState.getGameTime())) {
                processItemPickup(player, iterator, item);
            }
        }
    }

    private void processItemPickup(Player player, Iterator<DroppedItem> iterator, DroppedItem item) {
        int quantityPickedUp = player.pickUpItem(item.getItem());

        if (quantityPickedUp <= 0) return;

        if (item.getItem().getQuantity() <= 0) {
            iterator.remove();
        }

        onInventoryChanged.run();
        log.debug("Picked up {}x {}", quantityPickedUp, item.getItem().getDisplayName());
    }

    private void logDespawn(DroppedItem item, double currentTime) {
        if (log.isDebugEnabled()) {
            log.debug("Despawned {} (on the ground for {} minutes)",
                    item.getItem().getType().getDisplayName(),
                    String.format(Locale.ROOT, "%.1f", item.ageSeconds(currentTime) / 60));
        }
    }

}
