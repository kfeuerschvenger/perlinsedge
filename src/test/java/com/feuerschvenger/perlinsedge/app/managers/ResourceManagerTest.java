package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceManagerTest {

    private GameStateManager state;
    private ResourceManager manager;
    private Player player;
    private Tile tile;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        manager = new ResourceManager(state);
        TileMap map = new TileMap(3, 3);
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.HILL));
            }
        }
        tile = map.getTile(1, 1);
        player = new Player(0, 0);
        state.setCurrentMap(map);
        state.setPlayer(player);
    }

    /** Hits until the resource is gone, bypassing the real-time cooldown between hits. */
    private void hitUntilGone() {
        for (int i = 0; i < 20 && tile.hasResource(); i++) {
            state.getResourceHitTimes().clear();
            manager.handleTileInteraction(player, tile);
        }
    }

    @Test
    void ironOreRequiresAPickaxe() {
        assertEquals(ItemType.PICKAXE, ResourceType.IRON_ORE.getRequiredTool());
        assertNull(ResourceType.STONE.getRequiredTool());
        assertNull(ResourceType.TREE.getRequiredTool());
    }

    @Test
    void ironOreCannotBeMinedByHand() {
        tile.setWorldResource(new Resource(ResourceType.IRON_ORE, 1));

        hitUntilGone();

        assertTrue(tile.hasResource());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void hittingIronOreWithoutAPickaxeTellsThePlayerWhatIsMissing() {
        List<String> hints = new ArrayList<>();
        manager = new ResourceManager(state, hints::add);
        tile.setWorldResource(new Resource(ResourceType.IRON_ORE, 1));

        manager.handleTileInteraction(player, tile);

        assertEquals(1, hints.size());
        assertTrue(hints.get(0).toLowerCase().contains("pickaxe"), hints.get(0));
    }

    @Test
    void noHintWhenTheToolIsCarriedOrNoneIsNeeded() {
        List<String> hints = new ArrayList<>();
        manager = new ResourceManager(state, hints::add);
        player.addItem(new Item(ItemType.PICKAXE, 1));
        tile.setWorldResource(new Resource(ResourceType.IRON_ORE, 1));
        manager.handleTileInteraction(player, tile);
        tile.setWorldResource(new Resource(ResourceType.STONE, 1));
        manager.handleTileInteraction(player, tile);

        assertTrue(hints.isEmpty());
    }

    @Test
    void ironOreDropsIronOreWhenMinedWithAPickaxe() {
        player.addItem(new Item(ItemType.PICKAXE, 1));
        tile.setWorldResource(new Resource(ResourceType.IRON_ORE, 1));

        hitUntilGone();

        assertFalse(tile.hasResource());
        int ore = state.getDroppedItems().stream()
                .filter(drop -> drop.getItemType() == ItemType.IRON_ORE)
                .mapToInt(DroppedItem::getQuantity)
                .sum();
        assertTrue(ore >= 1, "the ore drop gives at least one iron ore");
    }

    @Test
    void stoneStillNeedsNoTool() {
        tile.setWorldResource(new Resource(ResourceType.STONE, 1));

        hitUntilGone();

        assertFalse(tile.hasResource());
    }

    @Test
    void theHitCooldownRunsOnTheGameClock() {
        tile.setWorldResource(new Resource(ResourceType.STONE, 1));
        Resource stone = tile.getWorldResource();

        manager.handleTileInteraction(player, tile);
        assertEquals(0.0, stone.secondsSinceHit(state.getGameTime()), "the first hit lands at once");

        state.incrementGameTime(0.1);
        manager.handleTileInteraction(player, tile);
        assertEquals(0.1, stone.secondsSinceHit(state.getGameTime()), 1e-9, "inside the cooldown the hit waits");

        state.incrementGameTime(0.9);
        manager.handleTileInteraction(player, tile);
        assertEquals(0.0, stone.secondsSinceHit(state.getGameTime()), "after the cooldown of game time the next hit lands");
    }

    @Test
    void farAwayPlayerCannotHarvest() {
        player.setPosition(0, 0);
        TileMap big = new TileMap(6, 6);
        Tile far = new Tile(5, 5, TileType.HILL);
        big.setTile(5, 5, far);
        far.setWorldResource(new Resource(ResourceType.STONE, 1));
        state.setCurrentMap(big);

        for (int i = 0; i < 10; i++) {
            state.getResourceHitTimes().clear();
            manager.handleTileInteraction(player, far);
        }

        assertTrue(far.hasResource());
    }
}
