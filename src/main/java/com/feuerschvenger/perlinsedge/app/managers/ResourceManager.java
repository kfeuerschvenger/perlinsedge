package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.LootManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Manages resource interactions including harvesting, depletion, and loot generation.
 * Handles cooldowns between hits and player proximity checks.
 */
@Slf4j
public class ResourceManager {
    /** What a click on a resource did. */
    public enum HitResult {
        /** A hit landed. */
        HIT,
        /** The resource was hit too recently for another to land, so the click had no effect. */
        COOLDOWN,
        /** The click was not valid: no resource, out of reach or the tool is missing. */
        REFUSED
    }

    // Configuration constants
    private static final int RESOURCE_HIT_DAMAGE = 10; // Damage per hit
    private static final int MAX_INTERACTION_DISTANCE = 1; // Adjacent tiles only (8-direction)
    private static final double MILLIS_PER_SECOND = 1000.0;

    // Dependencies
    private final GameStateManager gameState;
    private final Consumer<String> hintSink;

    /**
     * Creates a manager that shows no hints to the player.
     *
     * @param gameState shared game state
     */
    public ResourceManager(GameStateManager gameState) {
        this(gameState, message -> { });
    }

    /**
     * Creates a manager that reports why a harvest did nothing.
     *
     * @param gameState shared game state
     * @param hintSink  receives the short message to show the player
     */
    public ResourceManager(GameStateManager gameState, Consumer<String> hintSink) {
        this.gameState = gameState;
        this.hintSink = hintSink;
    }

    /**
     * Handles player interaction with a tile containing a resource.
     * Validates proximity before processing resource hit.
     *
     * @param player Player attempting interaction
     * @param tile Target tile containing resource
     * @return {@link HitResult#HIT} when a hit landed, {@link HitResult#COOLDOWN} when the resource was hit too
     *         recently for another to land, {@link HitResult#REFUSED} when the tile has no resource, the player is out
     *         of reach or lacks the tool
     */
    public HitResult handleTileInteraction(Player player, Tile tile) {
        if (!isValidInteraction(player, tile)) return HitResult.REFUSED;
        if (!hasToolFor(player, tile)) return HitResult.REFUSED;
        return handleResourceHit(tile.getWorldResource(), tile.getX(), tile.getY())
                ? HitResult.HIT : HitResult.COOLDOWN;
    }

    /**
     * Checks that the player carries the tool the resource on a tile needs, and tells the player what is missing when not.
     * The distance is not checked, so it can be asked before the player walks up to the resource.
     *
     * @param player the player
     * @param tile   tile with a resource
     * @return true when the resource needs no tool or the player carries it
     */
    public boolean hasToolFor(Player player, Tile tile) {
        if (tile == null || !tile.hasResource()) return false;
        Resource resource = tile.getWorldResource();
        if (hasRequiredTool(player, resource)) return true;
        ItemType tool = resource.getType().getRequiredTool();
        log.debug("Cannot harvest {} at ({}, {}): the player has no {}",
                resource.getType(), tile.getX(), tile.getY(), tool);
        hintSink.accept("You need a " + tool.getDisplayName().toLowerCase(Locale.ROOT) + " to mine this");
        return false;
    }

    /**
     * Checks that the player carries the tool a resource needs, such as the pickaxe for iron ore.
     */
    private boolean hasRequiredTool(Player player, Resource resource) {
        ItemType tool = resource.getType().getRequiredTool();
        return tool == null || player.canTakeItem(new Item(tool, 1), 1);
    }

    /**
     * Processes a hit on a resource, applying damage and handling depletion.
     * Enforces cooldown between hits and manages resource state.
     *
     * @param resource Resource being harvested
     * @param tileX Resource's X coordinate
     * @param tileY Resource's Y coordinate
     * @return true when the hit landed, false when the cooldown blocked it
     */
    private boolean handleResourceHit(Resource resource, int tileX, int tileY) {
        // The cooldown runs on the game clock, which stands still while paused, so a pause neither skips nor ends it
        double currentTime = gameState.getGameTime();
        double cooldown = AppConfig.getInstance().gameMechanics().getResourceHitCooldownMs() / MILLIS_PER_SECOND;

        if (isOnCooldown(resource, currentTime, cooldown)) return false;

        resource.startFlashing(currentTime);
        updateHitTimestamp(resource, currentTime);

        boolean depleted = resource.takeDamage(RESOURCE_HIT_DAMAGE);
        if (depleted) {
            processResourceDepletion(resource, tileX, tileY);
        }
        return true;
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
    private boolean isOnCooldown(Resource resource, double currentTime, double cooldown) {
        double lastHit = gameState.getResourceHitTimes().getOrDefault(resource, Double.NEGATIVE_INFINITY);
        return (currentTime - lastHit) < cooldown;
    }

    /**
     * Updates last hit game time for resource.
     */
    private void updateHitTimestamp(Resource resource, double timestamp) {
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