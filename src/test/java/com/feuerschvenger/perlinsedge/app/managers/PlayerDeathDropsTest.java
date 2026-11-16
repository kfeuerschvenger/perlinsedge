package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Everything the player carries drops on death, the hotbar slots as well as the inventory slots. */
class PlayerDeathDropsTest {

    @Test
    void theHotbarAndTheInventoryDropAndTheStorageIsEmptied() {
        GameStateManager state = new GameStateManager();
        TileMap map = new TileMap(9, 9);
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        Player player = new Player(4, 4);
        player.setItemAt(2, new Item(ItemType.AXE, 1));
        player.setItemAt(Player.HOTBAR_SIZE + 5, new Item(ItemType.STONE, 7));
        state.setCurrentMap(map);
        state.setPlayer(player);
        PlayerManager manager = FxToolkitSupport.onFx(() -> new PlayerManager(state, new HealthBar(),
                new InventoryPanel(state)));
        player.takeDamage(player.getMaxHealth() * 2);

        FxToolkitSupport.onFx(() -> {
            manager.updatePlayer(0.016);
            return null;
        });

        assertEquals(1, droppedOf(state, ItemType.AXE));
        assertEquals(7, droppedOf(state, ItemType.STONE));
        assertNull(player.getItemAt(2));
        assertNull(player.getItemAt(Player.HOTBAR_SIZE + 5));
    }

    private static int droppedOf(GameStateManager state, ItemType type) {
        return state.getDroppedItems().stream()
                .filter(drop -> drop.getItemType() == type)
                .mapToInt(DroppedItem::getQuantity)
                .sum();
    }
}
