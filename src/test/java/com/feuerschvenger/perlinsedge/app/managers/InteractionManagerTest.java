package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Door;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Wall;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
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

        assertTrue(interactions.interact(1, 5).isAccepted());

        assertFalse(interactions.hasPending());
        assertEquals(0.0, stone.secondsSinceHit(state.getGameTime()), "the hit landed");
        assertFalse(player.isMoving());
    }

    @Test
    void aFarResourceMakesThePlayerWalkThereAndHitItOnArrival() {
        Resource stone = stoneAt(8, 5);

        assertTrue(interactions.interact(8, 5).isAccepted());
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
    void aClickDuringTheHitCooldownEarnsNoFeedback() {
        stoneAt(1, 5);

        assertEquals(InteractionManager.Outcome.ACCEPTED, interactions.interact(1, 5));
        assertEquals(InteractionManager.Outcome.IGNORED, interactions.interact(1, 5));
        assertFalse(InteractionManager.Outcome.IGNORED.isAccepted());
    }

    @Test
    void theWalkEndsAtTheFirstMomentInReachAndThePlayerRestsThere() {
        stoneAt(8, 5);
        interactions.interact(8, 5);
        // A longer walk than the one needed, as when an earlier order left steps in the queue
        player.walkAlong(List.of(new PathNode(1, 5), new PathNode(2, 5), new PathNode(3, 5), new PathNode(4, 5),
                new PathNode(5, 5), new PathNode(6, 5), new PathNode(7, 5), new PathNode(8, 6), new PathNode(9, 6)));

        tickUntilIdle();
        for (int i = 0; i < 120; i++) {
            player.update(STEP);
        }

        assertEquals(7, player.getCurrentTileX(), "the player stopped as soon as the stone was in reach");
        assertEquals(7.0, player.getPosX(), 1e-9);
        assertEquals(5, player.getCurrentTileY());
        assertFalse(player.isMoving());
    }

    @Test
    void anInteractionInReachStopsTheWalkInProgress() {
        Resource stone = stoneAt(1, 5);
        player.walkAlong(List.of(new PathNode(0, 6), new PathNode(0, 7), new PathNode(0, 8)));

        assertTrue(interactions.interact(1, 5).isAccepted());
        for (int i = 0; i < 120; i++) {
            player.update(STEP);
        }

        assertEquals(0, player.getCurrentTileX());
        assertEquals(5, player.getCurrentTileY(), "the player did not walk off");
        assertEquals(0.0, stone.secondsSinceHit(state.getGameTime()), "the stone was hit");
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

        assertTrue(interactions.interact(8, 5).isAccepted());
        assertFalse(containers.isContainerOpen(), "the chest stays shut while the player walks");
        tickUntilIdle();

        assertTrue(containers.isContainerOpen());
        assertEquals(1, containerShown.get(), "the container was shown once");
    }

    @Test
    void aFarDoorTogglesOnArrival() {
        Door door = new Door(8, 5);
        map.getTile(8, 5).setBuilding(door);

        assertTrue(interactions.interact(8, 5).isAccepted());
        assertFalse(door.isOpen());
        tickUntilIdle();

        assertTrue(door.isOpen());
    }

    @Test
    void aNewClickCancelsThePendingInteraction() {
        Resource stone = stoneAt(8, 5);
        interactions.interact(8, 5);

        boolean accepted = interactions.interact(2, 2).isAccepted();

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

        assertFalse(interactions.interact(8, 5).isAccepted());

        assertFalse(interactions.hasPending());
        assertFalse(player.isMoving());
        assertEquals(1, hints.size());
    }

    @Test
    void aResourceThatNeedsAToolIsRefusedBeforeWalking() {
        map.getTile(8, 5).setWorldResource(new Resource(ResourceType.IRON_ORE, 1));

        assertFalse(interactions.interact(8, 5).isAccepted());

        assertFalse(player.isMoving(), "no point walking to ore the player cannot mine");
        assertEquals(1, hints.size());
        assertTrue(hints.get(0).toLowerCase().contains("pickaxe"));
    }

    @Test
    void ironOreIsMinedOnArrivalWhenThePickaxeIsCarried() {
        player.addItem(new Item(ItemType.PICKAXE, 1));
        Resource ore = new Resource(ResourceType.IRON_ORE, 1);
        map.getTile(8, 5).setWorldResource(ore);

        assertTrue(interactions.interact(8, 5).isAccepted());
        tickUntilIdle();

        assertTrue(ore.secondsSinceHit(state.getGameTime()) < 0.5);
    }

    @Test
    void aTileWithNothingToInteractWithIsRefused() {
        assertFalse(interactions.interact(3, 3).isAccepted());

        map.getTile(4, 4).setBuilding(new Wall(4, 4));
        assertFalse(interactions.interact(4, 4).isAccepted(), "a wall does nothing when clicked");
        assertFalse(interactions.hasPending());
    }

    /** Lets the player walk for a few frames, so it is between two tiles. */
    private void walkAFewFrames() {
        for (int i = 0; i < 4; i++) {
            state.incrementGameTime(STEP);
            player.update(STEP);
            interactions.update();
        }
        assertTrue(player.getPosX() != Math.rint(player.getPosX()), "the player is between two tiles");
    }

    private void assertPlayerStaysOnATile() {
        for (int i = 0; i < 120; i++) {
            state.incrementGameTime(STEP);
            player.update(STEP);
            interactions.update();
        }
        assertFalse(player.isMoving(), "the walk is over");
        assertEquals(Math.rint(player.getPosX()), player.getPosX(), 1e-9, "the player rests on a tile");
        assertTrue(player.getCurrentTileX() <= 1, "the player did not keep walking to the stone");
    }

    @Test
    void aRefusedClickMidWalkStopsThePlayer() {
        stoneAt(8, 5);
        interactions.interact(8, 5);
        walkAFewFrames();

        assertFalse(interactions.interact(3, 3).isAccepted());

        assertPlayerStaysOnATile();
    }

    @Test
    void aFailedMoveOrderMidWalkStopsThePlayer() {
        stoneAt(8, 5);
        map.setTile(10, 2, new Tile(10, 2, TileType.DEEP_WATER));
        interactions.interact(8, 5);
        walkAFewFrames();

        // The way the right click does it: cancel the interaction, then order the move
        interactions.cancel();
        assertFalse(player.moveTo(map, 10, 2));

        assertPlayerStaysOnATile();
    }

    @Test
    void pausingMidWalkAndResumingLeavesThePlayerStopped() {
        stoneAt(8, 5);
        interactions.interact(8, 5);
        walkAFewFrames();

        // The pause hook cancels the interaction, and the loop runs again after the resume
        interactions.cancel();
        state.setGamePaused(true);
        state.setGamePaused(false);

        assertPlayerStaysOnATile();
    }

    @Test
    void aMoveOrderMidWalkReplacesTheWalk() {
        stoneAt(8, 5);
        interactions.interact(8, 5);
        walkAFewFrames();

        interactions.cancel();
        assertTrue(player.moveTo(map, 0, 9));
        for (int i = 0; i < 240; i++) {
            player.update(STEP);
        }

        assertEquals(0, player.getCurrentTileX());
        assertEquals(9, player.getCurrentTileY());
    }

    @Test
    void cancellingWithNothingPendingLeavesAPlainWalkAlone() {
        assertTrue(player.moveTo(map, 0, 9));

        interactions.cancel();

        assertTrue(player.isMoving());
    }

    @Test
    void arrivingInsideTheHitCooldownKeepsTheInteractionPendingUntilTheHitLands() {
        Resource stone = stoneAt(3, 5);
        // The stone was hit moments ago: its cooldown outlasts the walk
        state.getResourceHitTimes().put(stone, state.getGameTime() + 0.5);
        assertTrue(interactions.interact(3, 5).isAccepted());

        for (int i = 0; i < 40; i++) {
            state.incrementGameTime(STEP);
            player.update(STEP);
            interactions.update();
        }
        assertEquals(2, player.getCurrentTileX(), "the player has arrived");
        assertTrue(interactions.hasPending(), "the interaction waits for the cooldown");
        assertEquals(Double.POSITIVE_INFINITY, stone.secondsSinceHit(state.getGameTime()));

        tickUntilIdle();

        assertFalse(interactions.hasPending());
        assertTrue(stone.secondsSinceHit(state.getGameTime()) < 0.5, "the hit landed when the cooldown ended");
        assertEquals(ClickFeedback.Kind.NONE, state.getClickFeedback().kind(state.getGameTime()));
    }

    @Test
    void arrivingWithTheToolDroppedHintsAndFlashesRed() {
        player.addItem(new Item(ItemType.PICKAXE, 1));
        Resource ore = new Resource(ResourceType.IRON_ORE, 1);
        map.getTile(8, 5).setWorldResource(ore);
        assertTrue(interactions.interact(8, 5).isAccepted());
        assertTrue(hints.isEmpty());

        player.removeItem(new Item(ItemType.PICKAXE, 1), 1);
        tickUntilIdle();

        assertFalse(interactions.hasPending());
        assertEquals(1, hints.size());
        assertTrue(hints.get(0).toLowerCase().contains("pickaxe"));
        assertEquals(ClickFeedback.Kind.REJECTED, state.getClickFeedback().kind(state.getGameTime()));
        assertEquals(Double.POSITIVE_INFINITY, ore.secondsSinceHit(state.getGameTime()));
    }

    @Test
    void aClickBetweenTwoTilesPlansFromTheTileBeingApproached() {
        // A wall the straight line from the tile left behind would graze: the walk goes around it from the next tile
        Resource stone = stoneAt(4, 6);
        map.getTile(2, 5).setBuilding(new Wall(2, 5));
        player.walkAlong(List.of(new PathNode(1, 5), new PathNode(1, 6), new PathNode(1, 7)));
        for (int i = 0; i < 4; i++) {
            player.update(STEP);
        }

        assertTrue(interactions.interact(4, 6).isAccepted());
        for (int i = 0; i < 600 && interactions.hasPending(); i++) {
            state.incrementGameTime(STEP);
            player.update(STEP);
            interactions.update();
            final int tileX = (int) Math.round(player.getPosX());
            final int tileY = (int) Math.round(player.getPosY());
            assertFalse(tileX == 2 && tileY == 5, "the player entered the blocked tile");
        }

        assertTrue(stone.secondsSinceHit(state.getGameTime()) < 0.5, "the stone was hit on arrival");
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
