package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.pathfinding.OccupiedTiles;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.pathfinding.Pathfinder;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A move order never walks into an enemy: it is refused onto an enemy's tile and the walk goes around enemies. */
class MoveAroundEnemiesTest {

    private static final int SIZE = 12;
    private static final double STEP = 1.0 / 60.0;

    private GameStateManager state;
    private TileMap map;
    private Player player;
    private PlayerManager players;

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
        players = new PlayerManager(state, null, null);
    }

    @Test
    void aMoveOrderOntoAnEnemyTileIsRefusedAndTheWalkDoesNotStart() {
        state.getEnemies().add(new Slime(6, 5));

        assertFalse(players.movePlayer(map, 6, 5));

        assertFalse(player.isMoving());
    }

    @Test
    void aMoveOrderOntoATileAnEnemyIsSteppingToIsRefused() {
        final Slime slime = new Slime(6, 5);
        slime.setTargetTileX(7);
        slime.setTargetTileY(5);
        state.getEnemies().add(slime);

        assertFalse(players.movePlayer(map, 7, 5));
    }

    @Test
    void aMoveOrderToAFreeTileNextToAnEnemyIsAccepted() {
        state.getEnemies().add(new Slime(6, 5));

        assertTrue(players.movePlayer(map, 5, 5));
        assertTrue(player.isMoving());
    }

    @Test
    void aDeadEnemyDoesNotBlockItsTile() {
        final Slime slime = new Slime(6, 5);
        slime.takeDamage(1_000);
        state.getEnemies().add(slime);

        assertTrue(players.movePlayer(map, 6, 5));
    }

    @Test
    void theWalkNeverStepsOnAnEnemyOnTheWay() {
        state.getEnemies().add(new Slime(3, 5));
        assertTrue(players.movePlayer(map, 8, 5));

        for (int i = 0; i < 600 && player.isMoving(); i++) {
            player.update(STEP);
            assertFalse(player.getCurrentTileX() == 3 && player.getCurrentTileY() == 5,
                    "the player went through the enemy");
        }

        assertEquals(8, player.getCurrentTileX());
        assertEquals(5, player.getCurrentTileY());
    }

    @Test
    void anEnemyThatMovesOntoTheWalkAfterItWasPlannedStopsThePlayerBeforeIt() {
        assertTrue(players.movePlayer(map, 8, 5));
        for (int i = 0; i < 20; i++) {
            player.update(STEP);
        }
        state.getEnemies().add(new Slime(3, 5));

        for (int i = 0; i < 600; i++) {
            players.keepClearOfEnemies();
            player.update(STEP);
            assertFalse(player.getCurrentTileX() == 3 && player.getCurrentTileY() == 5,
                    "the player stepped onto the enemy");
            assertFalse(Math.round(player.getPosX()) == 3 && Math.round(player.getPosY()) == 5,
                    "the player is drawn over the enemy");
        }

        assertFalse(player.isMoving(), "the walk ended in front of the enemy");
        assertTrue(player.getCurrentTileX() <= 2);
    }

    @Test
    void aWalkThatIsNotBlockedIsLeftAlone() {
        state.getEnemies().add(new Slime(3, 9));
        assertTrue(players.movePlayer(map, 8, 5));

        for (int i = 0; i < 600 && player.isMoving(); i++) {
            players.keepClearOfEnemies();
            player.update(STEP);
        }

        assertEquals(8, player.getCurrentTileX());
        assertEquals(5, player.getCurrentTileY());
    }

    @Test
    void theOrderIsRefusedWhenTheOnlyWayIsThroughAnEnemy() {
        // A corridor one tile wide, closed by an enemy
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (y != 5) {
                    map.setTile(x, y, new Tile(x, y, TileType.DEEP_WATER));
                }
            }
        }
        state.getEnemies().add(new Slime(3, 5));

        assertFalse(players.movePlayer(map, 8, 5));
    }

    @Test
    void thePathfinderNeverEndsOrCrossesAnOccupiedTile() {
        final Set<PathNode> occupied = Set.of(new PathNode(3, 5));

        assertFalse(new Pathfinder(map, occupied).findPath(0, 5, 3, 5).reachable());
        final List<PathNode> around = new Pathfinder(map, occupied).findPath(0, 5, 6, 5).requireNodes();
        assertNotNull(around);
        assertFalse(around.contains(new PathNode(3, 5)));
    }

    @Test
    void aNextToSearchNeverEndsOnAnOccupiedTile() {
        // The target is at (5,5) and the cheapest stand tile, (4,5), is taken
        final List<PathNode> walk = new Pathfinder(map, Set.of(new PathNode(4, 5)))
                .findPathNextTo(0, 5, 5, 5, SIZE * SIZE).requireNodes();

        assertNotNull(walk);
        final PathNode end = walk.get(walk.size() - 1);
        assertFalse(end.x == 4 && end.y == 5);
        assertTrue(Math.abs(end.x - 5) <= 1 && Math.abs(end.y - 5) <= 1);
    }

    @Test
    void occupiedTilesHoldTheStandingAndTheSteppingTileOfEveryLivingEnemy() {
        final Slime moving = new Slime(2, 2);
        moving.setTargetTileX(3);
        moving.setTargetTileY(2);
        final Slime dead = new Slime(9, 9);
        dead.takeDamage(1_000);

        final Set<PathNode> tiles = OccupiedTiles.of(List.of(moving, dead));

        assertEquals(Set.of(new PathNode(2, 2), new PathNode(3, 2)), tiles);
        assertTrue(OccupiedTiles.of(null).isEmpty());
    }
}
