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

public class ResourceManager {
    private final GameStateManager gameState;

    public ResourceManager(GameStateManager gameState) {
        this.gameState = gameState;
    }

    public void handleResourceHit(Resource resource, int tileX, int tileY) {
        long currentTime = System.currentTimeMillis();
        resource.startFlashing();
        long lastHitTime = gameState.getResourceHitTimes().getOrDefault(resource, 0L);
        long cooldown = AppConfig.getInstance().gameMechanics().getResourceHitCooldownMs();

        if (currentTime - lastHitTime < cooldown) {
            return;
        }

        gameState.getResourceHitTimes().put(resource, currentTime);
        boolean depleted = resource.takeDamage(10);
        TileMap map = gameState.getCurrentMap();
        Tile tile = map.getTile(tileX, tileY);

        if (depleted) {
            List<Item> drops = LootManager.getInstance().generateLootForResource(resource.getType());
            for (Item item : drops) {
                DroppedItem dropped = new DroppedItem(tileX, tileY, item, gameState.getGameTime(), DroppedItem.DropSource.RESOURCE);
                gameState.getDroppedItems().add(dropped);
            }

            if (tile != null) {
                tile.setWorldResource(null);
            }

            gameState.getResourceHitTimes().remove(resource);
        }
    }

    public void handleTileInteraction(Player player, Tile tile) {
        if (tile == null || !tile.hasResource()) {
            return;
        }
        int dx = Math.abs(player.getCurrentTileX() - tile.getX());
        int dy = Math.abs(player.getCurrentTileY() - tile.getY());

        if (dx <= 1 && dy <= 1) {
            Resource resource = tile.getWorldResource();
            handleResourceHit(resource, tile.getX(), tile.getY());
        }
    }
}