package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.ItemDropManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.containers.SlotRef;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropKeyHandlerTest {

    private GameStateManager state;
    private Player player;
    private SlotRef target;
    private DropKeyHandler keys;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        TileMap map = new TileMap(9, 9);
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        player = new Player(4, 4);
        player.setItemAt(3, new Item(ItemType.STONE, 5));
        state.setCurrentMap(map);
        state.setPlayer(player);
        state.setCurrentGameMode(GameMode.INVENTORY_OPEN);
        target = new SlotRef(player, 3);
        keys = new DropKeyHandler(state, () -> target, new ItemDropManager(state, () -> { }));
    }

    @Test
    void qThrowsOneItemFromTheTargetSlot() {
        keys.dropOne();

        assertEquals(4, player.getItemAt(3).getQuantity());
        assertEquals(1, state.getDroppedItems().size());
        assertEquals(1, state.getDroppedItems().getFirst().getQuantity());
    }

    @Test
    void ctrlQThrowsTheWholeStack() {
        keys.dropStack();

        assertNull(player.getItemAt(3));
        assertEquals(5, state.getDroppedItems().getFirst().getQuantity());
    }

    @Test
    void theKeysWorkInTheCraftingModeToo() {
        state.setCurrentGameMode(GameMode.CRAFTING_OPEN);

        keys.dropOne();

        assertEquals(4, player.getItemAt(3).getQuantity());
    }

    @Test
    void nothingHappensWithoutAnOpenInventory() {
        for (GameMode mode : new GameMode[]{GameMode.EXPLORATION, GameMode.BUILDING_MODE, GameMode.PAUSED}) {
            state.setCurrentGameMode(mode);

            keys.dropOne();
            keys.dropStack();

            assertEquals(5, player.getItemAt(3).getQuantity(), mode.name());
        }
        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void nothingHappensWhileThePlayerIsDead() {
        player.takeDamage(1000);

        keys.dropStack();

        assertEquals(5, player.getItemAt(3).getQuantity());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void nothingHappensWhileTheGameIsPaused() {
        state.setGamePaused(true);

        keys.dropStack();

        assertEquals(5, player.getItemAt(3).getQuantity());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void nothingHappensWhenNoSlotIsHoveredOrSelected() {
        target = null;

        keys.dropOne();
        keys.dropStack();

        assertEquals(5, player.getItemAt(3).getQuantity());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void anEmptyTargetSlotThrowsNothing() {
        target = new SlotRef(player, 8);

        keys.dropStack();

        assertTrue(state.getDroppedItems().isEmpty());
    }
}
