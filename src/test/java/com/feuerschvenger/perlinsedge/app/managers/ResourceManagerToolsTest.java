package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.entities.items.ToolStats;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The item held in hand decides what a click on a resource needs, how hard it hits and how often. */
class ResourceManagerToolsTest {
    private static final double STEP = 0.01;

    private GameStateManager state;
    private ResourceManager manager;
    private Player player;
    private Tile tile;
    private final List<String> hints = new ArrayList<>();

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        manager = new ResourceManager(state, hints::add);
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

    /** Hits as fast as the cooldown allows and counts the hits it took to break the resource. */
    private int hitsUntilGone() {
        int hits = 0;
        for (int tick = 0; tick < 5000 && tile.hasResource(); tick++) {
            if (manager.handleTileInteraction(player, tile) == ResourceManager.HitResult.HIT) {
                hits++;
            }
            state.incrementGameTime(STEP);
        }
        return hits;
    }

    /** The game seconds the resource took to break, hitting as fast as the cooldown allows. */
    private double secondsUntilGone() {
        double start = state.getGameTime();
        hitsUntilGone();
        return state.getGameTime() - start;
    }

    @Test
    void anAxeInHandBreaksATreeInThreeHits() {
        player.setItemAt(0, new Item(ItemType.AXE, 1));
        tile.setWorldResource(new Resource(ResourceType.TREE, 1));

        assertEquals(3, hitsUntilGone());
    }

    @Test
    void aPickaxeInHandBreaksStoneIronOreAndCrystalsInThreeHits() {
        player.setItemAt(0, new Item(ItemType.PICKAXE, 1));
        for (ResourceType type : new ResourceType[]{ResourceType.STONE, ResourceType.IRON_ORE, ResourceType.CRYSTALS}) {
            tile.setWorldResource(new Resource(type, 1));

            assertEquals(3, hitsUntilGone(), type.name());
        }
    }

    @Test
    void aBareHandBreaksATreeAndAStoneInAboutThreeTimesTheHitsAndSlowly() {
        tile.setWorldResource(new Resource(ResourceType.TREE, 1));
        int treeByHand = hitsUntilGone();
        tile.setWorldResource(new Resource(ResourceType.STONE, 1));
        int stoneByHand = hitsUntilGone();

        assertEquals(ToolStats.hitsToBreak(null, ResourceType.TREE), treeByHand);
        assertEquals(ToolStats.hitsToBreak(null, ResourceType.STONE), stoneByHand);
        assertTrue(treeByHand >= 8 && stoneByHand >= 8, "tree " + treeByHand + ", stone " + stoneByHand);

        player.setItemAt(0, new Item(ItemType.AXE, 1));
        tile.setWorldResource(new Resource(ResourceType.TREE, 1));
        double withAxe = secondsUntilGone();
        player.setItemAt(0, null);
        tile.setWorldResource(new Resource(ResourceType.TREE, 1));
        double byHand = secondsUntilGone();
        assertTrue(byHand > withAxe * 3, "the hand is slower per hit as well: " + byHand + " s against " + withAxe + " s");
    }

    @Test
    void theHandSwingsEverySixHundredMillisecondsAndAToolEveryThreeHundred() {
        tile.setWorldResource(new Resource(ResourceType.TREE, 1));
        manager.handleTileInteraction(player, tile);

        state.incrementGameTime(0.5);
        assertEquals(ResourceManager.HitResult.COOLDOWN, manager.handleTileInteraction(player, tile));
        state.incrementGameTime(0.11);
        assertEquals(ResourceManager.HitResult.HIT, manager.handleTileInteraction(player, tile));

        player.setItemAt(0, new Item(ItemType.AXE, 1));
        state.incrementGameTime(0.2);
        assertEquals(ResourceManager.HitResult.COOLDOWN, manager.handleTileInteraction(player, tile));
        state.incrementGameTime(0.11);
        assertEquals(ResourceManager.HitResult.HIT, manager.handleTileInteraction(player, tile));
    }

    @Test
    void aWrongToolHitsLikeAHand() {
        player.setItemAt(0, new Item(ItemType.AXE, 1));
        tile.setWorldResource(new Resource(ResourceType.STONE, 1));

        assertEquals(ToolStats.hitsToBreak(null, ResourceType.STONE), hitsUntilGone());
    }

    @Test
    void ironOreAndCrystalsNeedThePickaxeInHandNotJustInTheBag() {
        // The pickaxe is in the bag, outside the hotbar slot held in hand
        player.setItemAt(12, new Item(ItemType.PICKAXE, 1));
        for (ResourceType type : new ResourceType[]{ResourceType.IRON_ORE, ResourceType.CRYSTALS}) {
            tile.setWorldResource(new Resource(type, 1));

            assertFalse(manager.hasToolFor(player, tile), type.name());
            assertEquals(ResourceManager.HitResult.REFUSED, manager.handleTileInteraction(player, tile), type.name());
        }
        assertEquals(2, hints.size());
        assertTrue(hints.get(0).toLowerCase().contains("pickaxe"), hints.get(0));
    }

    @Test
    void selectingTheHotbarSlotOfThePickaxeAllowsTheHit() {
        player.setItemAt(4, new Item(ItemType.PICKAXE, 1));
        tile.setWorldResource(new Resource(ResourceType.IRON_ORE, 1));
        assertFalse(manager.hasToolFor(player, tile));

        player.selectHotbarSlot(4);

        assertTrue(manager.hasToolFor(player, tile));
        assertEquals(ResourceManager.HitResult.HIT, manager.handleTileInteraction(player, tile));
    }

    @Test
    void anAxeInHandDoesNotMineIronOre() {
        player.setItemAt(0, new Item(ItemType.AXE, 1));
        tile.setWorldResource(new Resource(ResourceType.IRON_ORE, 1));

        assertFalse(manager.hasToolFor(player, tile));
        assertEquals(ResourceManager.HitResult.REFUSED, manager.handleTileInteraction(player, tile));
    }

    @Test
    void stoneAndTreesNeedNoToolInHand() {
        tile.setWorldResource(new Resource(ResourceType.STONE, 1));
        assertTrue(manager.hasToolFor(player, tile));
        tile.setWorldResource(new Resource(ResourceType.TREE, 1));
        assertTrue(manager.hasToolFor(player, tile));
    }
}
