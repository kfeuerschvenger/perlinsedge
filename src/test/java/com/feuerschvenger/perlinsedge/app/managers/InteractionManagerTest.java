package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Door;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Wall;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionManagerTest {

    private static final int SIZE = 12;
    private static final double STEP = 1.0 / 60.0;

    private GameStateManager state;
    private ContainerManager containers;
    private InteractionManager interactions;
    private Player player;
    private TileMap map;
    private final List<String> hints = new ArrayList<>();
    private final AtomicInteger containerShown = new AtomicInteger();

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        containers = new ContainerManager();
        map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        player = new Player(0, 5);
        state.setCurrentMap(map);
        state.setPlayer(player);
        BuildingManager buildings = new BuildingManager(state, new RecipeManager(), containers);
        ResourceManager resources = new ResourceManager(state, hints::add);
        interactions = new InteractionManager(state, buildings, resources, containers,
                containerShown::incrementAndGet, hints::add);
    }

    /** Runs the game ticks a walk needs, the way the game loop does, until nothing is pending or the ticks run out. */
    private void tickUntilIdle() {
        for (int i = 0; i < 600 && interactions.hasPending(); i++) {
            state.incrementGameTime(STEP);
            player.update(STEP);
            interactions.update();
        }
    }

    private Resource stoneAt(int x, int y) {
        Resource stone = new Resource(ResourceType.STONE, 1);
        map.getTile(x, y).setWorldResource(stone);
        return stone;
    }

    @Test
    void aResourceInReachIsHitAtOnceWithoutWalking() {
        Resource stone = stoneAt(1, 5);

        assertTrue(interactions.interact(1, 5));

        assertFalse(interactions.hasPending());
        assertEquals(0.0, stone.secondsSinceHit(state.getGameTime()), "the hit landed");
        assertFalse(player.isMoving());
    }

    @Test
    void aFarResourceMakesThePlayerWalkThereAndHitItOnArrival() {
        Resource stone = stoneAt(8, 5);

        assertTrue(interactions.interact(8, 5));
        assertTrue(interactions.hasPending());
        assertTrue(player.isMoving());
        assertEquals(Double.POSITIVE_INFINITY, stone.secondsSinceHit(state.getGameTime()), "nothing is hit before arriving");

        tickUntilIdle();

        assertFalse(interactions.hasPending());
        assertEquals(7, player.getCurrentTileX(), "the player stopped on the tile next to the stone");
        assertEquals(5, player.getCurrentTileY());
        assertTrue(stone.secondsSinceHit(state.getGameTime()) < 0.5, "the stone was hit when the player arrived");
    }

    @Test
    void theInteractionHappensOnceOnArrival() {
        Resource stone = stoneAt(8, 5);
        interactions.interact(8, 5);
        tickUntilIdle();

        double stampAfterArrival = stone.secondsSinceHit(state.getGameTime());
        for (int i = 0; i < 120; i++) {
            state.incrementGameTime(STEP);
            player.update(STEP);
            interactions.update();
        }

        assertEquals(stampAfterArrival + 120 * STEP, stone.secondsSinceHit(state.getGameTime()), 1e-9,
                "no further hit followed the arrival");
    }

    @Test
    void aFarChestOpensOnArrival() {
        Chest chest = new Chest(8, 5, 9);
        map.getTile(8, 5).setBuilding(chest);

        assertTrue(interactions.interact(8, 5));
        assertFalse(containers.isContainerOpen(), "the chest stays shut while the player walks");
        tickUntilIdle();

        assertTrue(containers.isContainerOpen());
        assertEquals(1, containerShown.get(), "the container was shown once");
    }

    @Test
    void aFarDoorTogglesOnArrival() {
        Door door = new Door(8, 5);
        map.getTile(8, 5).setBuilding(door);

        assertTrue(interactions.interact(8, 5));
        assertFalse(door.isOpen());
        tickUntilIdle();

        assertTrue(door.isOpen());
    }

    @Test
    void aNewClickCancelsThePendingInteraction() {
        Resource stone = stoneAt(8, 5);
        interactions.interact(8, 5);

        boolean accepted = interactions.interact(2, 2);

        assertFalse(accepted, "a plain tile has nothing to interact with");
        assertFalse(interactions.hasPending());
        for (int i = 0; i < 600; i++) {
            state.incrementGameTime(STEP);
            player.update(STEP);
            interactions.update();
        }
        assertEquals(Double.POSITIVE_INFINITY, stone.secondsSinceHit(state.getGameTime()));
    }

    @Test
    void aMoveOrderCancelsThePendingInteraction() {
        Resource stone = stoneAt(8, 5);
        interactions.interact(8, 5);

        interactions.cancel();
        player.moveTo(map, 0, 0);
        for (int i = 0; i < 600; i++) {
            state.incrementGameTime(STEP);
            player.update(STEP);
            interactions.update();
        }

        assertEquals(Double.POSITIVE_INFINITY, stone.secondsSinceHit(state.getGameTime()));
        assertEquals(0, player.getCurrentTileX());
    }

    @Test
    void theDeathOfThePlayerCancelsThePendingInteraction() {
        Resource stone = stoneAt(8, 5);
        interactions.interact(8, 5);

        player.takeDamage(1000);
        interactions.update();

        assertFalse(interactions.hasPending());
        assertEquals(Double.POSITIVE_INFINITY, stone.secondsSinceHit(state.getGameTime()));
    }

    @Test
    void pausingCancelsThePendingInteraction() {
        stoneAt(8, 5);
        interactions.interact(8, 5);

        state.setGamePaused(true);
        interactions.update();

        assertFalse(interactions.hasPending());
    }

    @Test
    void leavingExplorationCancelsThePendingInteraction() {
        stoneAt(8, 5);
        interactions.interact(8, 5);

        state.setCurrentGameMode(GameMode.INVENTORY_OPEN);
        interactions.update();

        assertFalse(interactions.hasPending());
    }

    @Test
    void aTargetThatDisappearsCancelsThePendingInteraction() {
        stoneAt(8, 5);
        interactions.interact(8, 5);

        map.getTile(8, 5).setWorldResource(null);
        interactions.update();

        assertFalse(interactions.hasPending());
    }

    @Test
    void aTargetWithNoWalkableTileAroundItIsRefusedWithAHint() {
        stoneAt(8, 5);
        for (int x = 7; x <= 9; x++) {
            for (int y = 4; y <= 6; y++) {
                if (x != 8 || y != 5) {
                    map.setTile(x, y, new Tile(x, y, TileType.DEEP_WATER));
                }
            }
        }

        assertFalse(interactions.interact(8, 5));

        assertFalse(interactions.hasPending());
        assertFalse(player.isMoving());
        assertEquals(1, hints.size());
    }

    @Test
    void aResourceThatNeedsAToolIsRefusedBeforeWalking() {
        map.getTile(8, 5).setWorldResource(new Resource(ResourceType.IRON_ORE, 1));

        assertFalse(interactions.interact(8, 5));

        assertFalse(player.isMoving(), "no point walking to ore the player cannot mine");
        assertEquals(1, hints.size());
        assertTrue(hints.get(0).toLowerCase().contains("pickaxe"));
    }

    @Test
    void ironOreIsMinedOnArrivalWhenThePickaxeIsCarried() {
        player.addItem(new Item(ItemType.PICKAXE, 1));
        Resource ore = new Resource(ResourceType.IRON_ORE, 1);
        map.getTile(8, 5).setWorldResource(ore);

        assertTrue(interactions.interact(8, 5));
        tickUntilIdle();

        assertTrue(ore.secondsSinceHit(state.getGameTime()) < 0.5);
    }

    @Test
    void aTileWithNothingToInteractWithIsRefused() {
        assertFalse(interactions.interact(3, 3));

        map.getTile(4, 4).setBuilding(new Wall(4, 4));
        assertFalse(interactions.interact(4, 4), "a wall does nothing when clicked");
        assertFalse(interactions.hasPending());
    }

    @Test
    void aWalkThatStopsShortOfTheTargetIsDroppedInsteadOfWaitingForever() {
        stoneAt(8, 5);
        interactions.interact(8, 5);
        // Dying clears the walk, and the player comes back to life where it stood, still far from the stone
        player.takeDamage(1000);
        player.revive(0, 5);

        interactions.update();

        assertFalse(interactions.hasPending());
    }
}
