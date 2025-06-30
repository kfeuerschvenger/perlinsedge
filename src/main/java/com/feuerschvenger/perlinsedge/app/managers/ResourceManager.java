package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.LootManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.List;

/**
 * Manages resource interactions including harvesting, depletion, and loot generation.
 * Handles cooldowns between hits and player proximity checks.
 */
public class ResourceManager {
    // Configuration constants
    private static final int RESOURCE_HIT_DAMAGE = 10; // Damage per hit
    private static final int MAX_INTERACTION_DISTANCE = 1; // Adjacent tiles only (8-direction)

    // Dependencies
    private final GameStateManager gameState;

    public ResourceManager(GameStateManager gameState) {
        this.gameState = gameState;
    }

    /**
     * Handles player interaction with a tile containing a resource.
     * Validates proximity before processing resource hit.
     *
     * @param player Player attempting interaction
     * @param tile Target tile containing resource
     */
    public void handleTileInteraction(Player player, Tile tile) {
        if (!isValidInteraction(player, tile)) return;
        handleResourceHit(tile.getWorldResource(), tile.getX(), tile.getY());
    }

    /**
     * Processes a hit on a resource, applying damage and handling depletion.
     * Enforces cooldown between hits and manages resource state.
     *
     * @param resource Resource being harvested
     * @param tileX Resource's X coordinate
     * @param tileY Resource's Y coordinate
     */
    private void handleResourceHit(Resource resource, int tileX, int tileY) {
        long currentTime = System.currentTimeMillis();
        long cooldown = AppConfig.getInstance().gameMechanics().getResourceHitCooldownMs();

        if (isOnCooldown(resource, currentTime, cooldown)) return;

        resource.startFlashing();
        updateHitTimestamp(resource, currentTime);

        boolean depleted = resource.takeDamage(RESOURCE_HIT_DAMAGE);
        if (depleted) {
            processResourceDepletion(resource, tileX, tileY);
        }
    }

    /**
     * Handles depleted resource:
     * 1. Generates loot drops
     * 2. Places drops in world
     * 3. Removes resource from tile
     */
    private void processResourceDepletion(Resource resource, int tileX, int tileY) {
        List<Item> drops = LootManager.getInstance().generateLootForResource(resource.getType());
        for (Item item : drops) {
            gameState.getDroppedItems().add(
                    new DroppedItem(
                            tileX, tileY, item,
                            gameState.getGameTime(),
                            DroppedItem.DropSource.RESOURCE
                    )
            );
        }

        removeResourceFromTile(tileX, tileY);
        clearResourceCooldown(resource);
    }

    /**
     * Validates resource interaction conditions:
     * - Tile exists and contains resource
     * - Player is within interaction distance
     */
    private boolean isValidInteraction(Player player, Tile tile) {
        return tile != null && tile.hasResource() && isWithinInteractionDistance(player, tile);
    }

    /**
     * Checks if player is adjacent to resource tile.
     */
    private boolean isWithinInteractionDistance(Player player, Tile tile) {
        int dx = Math.abs(player.getCurrentTileX() - tile.getX());
        int dy = Math.abs(player.getCurrentTileY() - tile.getY());
        return dx <= MAX_INTERACTION_DISTANCE &&
                dy <= MAX_INTERACTION_DISTANCE;
    }

    /**
     * Checks if resource is on cooldown from previous hit.
     */
    private boolean isOnCooldown(Resource resource, long currentTime, long cooldown) {
        long lastHit = gameState.getResourceHitTimes().getOrDefault(resource, 0L);
        return (currentTime - lastHit) < cooldown;
    }

    /**
     * Updates last hit timestamp for resource.
     */
    private void updateHitTimestamp(Resource resource, long timestamp) {
        gameState.getResourceHitTimes().put(resource, timestamp);
    }

    /**
     * Removes resource from tile after depletion.
     */
    private void removeResourceFromTile(int tileX, int tileY) {
        TileMap map = gameState.getCurrentMap();
        if (map != null) {
            Tile tile = map.getTile(tileX, tileY);
            if (tile != null) {
                tile.setWorldResource(null);
            }
        }
    }

    /**
     * Clears cooldown tracking for depleted resource.
     */
    private void clearResourceCooldown(Resource resource) {
        gameState.getResourceHitTimes().remove(resource);
    }

}