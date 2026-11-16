package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.entities.items.ToolStats;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
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

/** A click on an enemy attacks it with the item in hand: at once within reach, otherwise after walking up to it. */
class AttackClickTest {

    private static final int SIZE = 12;
    private static final double STEP = 1.0 / 60.0;

    private GameStateManager state;
    private InteractionManager interactions;
    private Player player;
    private TileMap map;
    private final List<String> hints = new ArrayList<>();

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        player = new Player(0, 5);
        state.setCurrentMap(map);
        state.setPlayer(player);
        final ContainerManager containers = new ContainerManager();
        interactions = new InteractionManager(state,
                new BuildingManager(state, new RecipeManager(), containers),
                new ResourceManager(state, hints::add), new CombatManager(state), containers,
                () -> { }, hints::add);
    }

    private Slime slimeAt(int x, int y) {
        final Slime slime = new Slime(x, y);
        state.getEnemies().add(slime);
        return slime;
    }

    /** Runs the ticks of the game loop that matter here: the player, the pending interaction and the clock. */
    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            state.incrementGameTime(STEP);
            player.update(STEP);
            interactions.update();
        }
    }

    private void tickUntilIdle() {
        for (int i = 0; i < 900 && interactions.hasPending(); i++) {
            tick(1);
        }
    }

    @Test
    void aBareHandedClickOnASlimeInReachHitsItAtOnce() {
        final Slime slime = slimeAt(1, 5);
        final double before = slime.getHealth();

        assertEquals(InteractionManager.Outcome.ACCEPTED, interactions.interact(1, 5));

        assertEquals(before - ToolStats.HAND_COMBAT_DAMAGE, slime.getHealth(), 1e-9);
        assertFalse(interactions.hasPending());
        assertTrue(player.isAttacking());
    }

    @Test
    void aClickOnASlimeWithTheAxeHitsWithTheAxeDamage() {
        player.setItemAt(0, new Item(ItemType.AXE, 1));
        final Slime slime = slimeAt(1, 4);
        final double before = slime.getHealth();

        assertTrue(interactions.interact(1, 4).isAccepted());

        assertEquals(before - ToolStats.combatDamage(ItemType.AXE), slime.getHealth(), 1e-9);
    }

    @Test
    void aClickOnASlimeDuringTheCooldownWaitsAndHitsWhenItEnds() {
        final Slime slime = slimeAt(1, 5);
        interactions.interact(1, 5);
        final double afterFirst = slime.getHealth();

        assertEquals(InteractionManager.Outcome.IGNORED, interactions.interact(1, 5));
        assertEquals(afterFirst, slime.getHealth(), 1e-9, "no second hit during the cooldown");
        assertTrue(interactions.hasPending());

        tickUntilIdle();

        assertEquals(afterFirst - ToolStats.HAND_COMBAT_DAMAGE, slime.getHealth(), 1e-9);
        assertFalse(interactions.hasPending());
    }

    @Test
    void aFarSlimeMakesThePlayerWalkUpToItAndHitItOnArrival() {
        final Slime slime = slimeAt(8, 5);
        final double before = slime.getHealth();

        assertTrue(interactions.interact(8, 5).isAccepted());
        assertTrue(interactions.hasPending());
        assertTrue(player.isMoving());
        assertEquals(before, slime.getHealth(), 1e-9, "nothing is hit before arriving");

        tickUntilIdle();

        assertEquals(7, player.getCurrentTileX(), "the player stopped on the tile next to the slime");
        assertEquals(5, player.getCurrentTileY());
        assertEquals(before - ToolStats.HAND_COMBAT_DAMAGE, slime.getHealth(), 1e-9);
    }

    @Test
    void theWalkFollowsASlimeThatMovesAndHitsItWhereItEnded() {
        final Slime slime = slimeAt(8, 5);
        interactions.interact(8, 5);
        tick(20);
        slime.setPosition(8, 9);
        final double before = slime.getHealth();

        tickUntilIdle();

        assertTrue(Math.abs(player.getCurrentTileX() - 8) <= 1 && Math.abs(player.getCurrentTileY() - 9) <= 1,
                "the player ended next to the slime's new tile");
        assertEquals(before - ToolStats.HAND_COMBAT_DAMAGE, slime.getHealth(), 1e-9);
    }

    @Test
    void theWalkToASlimeNeverEndsOnAnotherSlime() {
        slimeAt(7, 5);
        final Slime other = slimeAt(8, 5);

        assertTrue(interactions.interact(8, 5).isAccepted());
        tickUntilIdle();

        assertFalse(player.getCurrentTileX() == 7 && player.getCurrentTileY() == 5, "the player stood on a slime");
        assertTrue(other.getHealth() < other.getMaxHealth());
    }

    @Test
    void aSlimeThatDiesOrLeavesEndsThePendingAttack() {
        final Slime slime = slimeAt(8, 5);
        interactions.interact(8, 5);

        state.getEnemies().remove(slime);
        tick(1);

        assertFalse(interactions.hasPending());
        // The player finishes the step it is on, then rests: it does not go on to the enemy that is gone
        tick(30);
        assertFalse(player.isMoving());
        assertTrue(player.getCurrentTileX() <= 1);
    }

    @Test
    void aSlimeThatCannotBeReachedIsRefusedWithAHint() {
        slimeAt(8, 5);
        for (int x = 7; x <= 9; x++) {
            for (int y = 4; y <= 6; y++) {
                if (x != 8 || y != 5) {
                    map.setTile(x, y, new Tile(x, y, TileType.DEEP_WATER));
                }
            }
        }

        assertEquals(InteractionManager.Outcome.REFUSED, interactions.interact(8, 5));

        assertFalse(interactions.hasPending());
        assertEquals(List.of("You can't reach that"), hints);
    }

    @Test
    void theEnemySearchBudgetGrowsWithTheMapAndHasAFloor() {
        assertEquals(InteractionManager.ENEMY_SEARCH_MIN_NODES, InteractionManager.enemySearchBudget(map));
        assertEquals(InteractionManager.ENEMY_SEARCH_MAX_NODES, InteractionManager.enemySearchBudget(new TileMap(1000, 1000)));
        assertEquals(40_000, InteractionManager.enemySearchBudget(new TileMap(400, 400)));
    }

    @Test
    void anEnemyBehindALongDetourOnALargeMapIsStillReached() {
        final int width = 400;
        final int height = 400;
        final TileMap large = new TileMap(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                large.setTile(x, y, new Tile(x, y, x == 200 && y != 60 ? TileType.DEEP_WATER : TileType.GRASS));
            }
        }
        state.setCurrentMap(large);
        player.setPosition(10, 5);
        slimeAt(390, 5);

        assertEquals(InteractionManager.Outcome.ACCEPTED, interactions.interact(390, 5));
        assertTrue(hints.isEmpty());
    }

    @Test
    void aNewOrderAndThePauseCancelThePendingAttack() {
        slimeAt(8, 5);
        interactions.interact(8, 5);
        interactions.cancel();
        assertFalse(interactions.hasPending());

        interactions.interact(8, 5);
        state.setGamePaused(true);
        tick(1);
        assertFalse(interactions.hasPending());
    }

    @Test
    void aChangeOfGameModeCancelsThePendingAttack() {
        slimeAt(8, 5);
        interactions.interact(8, 5);

        state.setCurrentGameMode(GameMode.INVENTORY_OPEN);
        tick(1);

        assertFalse(interactions.hasPending());
    }

    @Test
    void aDeadPlayerDoesNotAttack() {
        final Slime slime = slimeAt(1, 5);
        player.takeDamage(1_000);

        assertEquals(InteractionManager.Outcome.REFUSED, interactions.interact(1, 5));
        assertEquals(slime.getMaxHealth(), slime.getHealth(), 1e-9);
    }

}
