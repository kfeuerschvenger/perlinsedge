package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.items.ToolStats;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every enemy holds a tile of its own in the game state, and a click on a tile hits the one enemy that is there. */
class EnemyTilesTest {

    private static final int SIZE = 30;
    private static final double STEP = 1.0 / 60.0;

    private GameStateManager state;
    private CombatManager combat;
    private InteractionManager interactions;
    private Player player;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        final TileMap map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        player = new Player(0, 5);
        state.setCurrentMap(map);
        state.setPlayer(player);
        combat = new CombatManager(state);
        final ContainerManager containers = new ContainerManager();
        final List<String> hints = new ArrayList<>();
        interactions = new InteractionManager(state, new BuildingManager(state, new RecipeManager(), containers),
                new ResourceManager(state, hints::add), combat, containers,
                InteractionManager.OpenViews.containerOnly(() -> { }), hints::add);
    }

    @Test
    void settingTheEnemiesHoldsTheirTilesAndForgetsTheOldOnes() {
        final Slime old = new Slime(9, 9);
        state.setEnemies(List.of(old));
        final Slime fresh = new Slime(3, 3);

        state.setEnemies(List.of(fresh));

        assertSame(fresh, state.getEnemyOccupancy().ownerOf(3, 3));
        assertNull(state.getEnemyOccupancy().ownerOf(9, 9));
    }

    @Test
    void theEnemiesUpdatedByTheCombatManagerNeverShareATile() {
        final List<Enemy> crowd = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            crowd.add(new Slime(14 + i % 4, 14 + i / 4));
        }
        crowd.add(new Skeleton(20, 20));
        crowd.add(new Skeleton(20, 22));
        state.setEnemies(crowd);
        player.setPosition(10, 10);

        for (int tick = 0; tick < 60 * 20; tick++) {
            player.heal(1_000);
            combat.updateEnemies(STEP);
            final Set<String> tiles = new HashSet<>();
            for (Enemy enemy : crowd) {
                assertTrue(tiles.add(enemy.getCurrentTileX() + "," + enemy.getCurrentTileY()),
                        "two enemies stand on one tile at tick " + tick);
            }
        }
    }

    @Test
    void anEnemyKilledByASwingReleasesItsTilesAtOnce() {
        final Slime slime = new Slime(1, 5);
        state.setEnemies(List.of(slime));
        slime.takeDamage(slime.getHealth() - 1);

        assertEquals(CombatManager.Strike.HIT, combat.strikeEnemy(slime));

        assertTrue(slime.isDead());
        assertNull(state.getEnemyOccupancy().ownerOf(1, 5));
        assertEquals(0, state.getEnemyOccupancy().heldTileCount());
    }

    @Test
    void aTileOfAnEnemyThatDiedOtherwiseIsReleasedOnTheNextUpdate() {
        final Slime slime = new Slime(15, 15);
        state.setEnemies(List.of(slime));
        slime.takeDamage(1_000);

        combat.updateEnemies(STEP);

        assertEquals(0, state.getEnemyOccupancy().heldTileCount());
    }

    @Test
    void aClickOnATileHitsTheOneEnemyOnIt() {
        final Slime near = new Slime(1, 5);
        final Slime beyond = new Slime(2, 5);
        state.setEnemies(List.of(beyond, near));
        final double beyondHealth = beyond.getHealth();

        assertTrue(interactions.interact(1, 5).isAccepted());

        assertEquals(near.getMaxHealth() - ToolStats.HAND_COMBAT_DAMAGE, near.getHealth(), 1e-9);
        assertEquals(beyondHealth, beyond.getHealth(), 1e-9, "the neighbour is not hit");
    }

    @Test
    void aSpriteDrawnOverTheTileOfAnEnemyDoesNotTakeTheClickFromIt() {
        final Slime hopping = new Slime(2, 5);
        // Mid-hop towards the player, drawn over tile 1 while the enemy standing on it is drawn at its centre
        hopping.setPosX(1.4);
        hopping.setPosY(5.0);
        final Slime standing = new Slime(1, 5);
        state.setEnemies(List.of(hopping, standing));
        final double hoppingHealth = hopping.getHealth();

        assertTrue(interactions.interact(1, 5).isAccepted());

        assertEquals(standing.getMaxHealth() - ToolStats.HAND_COMBAT_DAMAGE, standing.getHealth(), 1e-9);
        assertEquals(hoppingHealth, hopping.getHealth(), 1e-9, "the enemy drawn off-centre is not hit");
    }
}
