package com.feuerschvenger.perlinsedge.domain.entities;

import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerMoveOrderTest {

    private TileMap map;
    private Player player;

    @BeforeEach
    void setUp() {
        map = new TileMap(6, 6);
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 6; y++) {
                map.setTile(x, y, new Tile(x, y, x == 4 ? TileType.DEEP_WATER : TileType.GRASS));
            }
        }
        player = new Player(0, 0);
    }

    @Test
    void aReachableTileStartsAWalk() {
        assertTrue(player.moveTo(map, 3, 3));
        assertTrue(player.isMoving());
    }

    @Test
    void theTileUnderThePlayerIsNothingToDo() {
        assertFalse(player.moveTo(map, 0, 0));
        assertFalse(player.isMoving());
    }

    @Test
    void waterIsRefused() {
        assertFalse(player.moveTo(map, 4, 2));
        assertFalse(player.isMoving());
    }

    @Test
    void aTileBehindWaterIsRefused() {
        assertFalse(player.moveTo(map, 5, 2));
    }

    @Test
    void aDeadPlayerDoesNotWalk() {
        player.takeDamage(1000);

        assertFalse(player.moveTo(map, 3, 3));
        assertFalse(player.walkAlong(List.of(new PathNode(1, 0))));
    }

    @Test
    void walkingAlongAPathFollowsIt() {
        assertTrue(player.walkAlong(List.of(new PathNode(1, 0), new PathNode(2, 0))));
        for (int i = 0; i < 120; i++) {
            player.update(1.0 / 60.0);
        }

        assertTrue(player.getCurrentTileX() == 2 && player.getCurrentTileY() == 0);
    }

    @Test
    void aWalkOfEveryLengthEndsOnTheTileThatWasClicked() {
        for (int steps = 1; steps <= 3; steps++) {
            Player walker = new Player(0, 0);
            assertTrue(walker.moveTo(map, steps, 0), "walk of " + steps);
            for (int i = 0; i < 240; i++) {
                walker.update(1.0 / 60.0);
            }

            assertTrue(walker.getCurrentTileX() == steps && walker.getCurrentTileY() == 0,
                    "the walk of " + steps + " tiles ended on " + walker.getCurrentTileX() + "," + walker.getCurrentTileY());
            assertFalse(walker.isMoving());
        }
    }

    @Test
    void anEmptyPathChangesNothing() {
        assertFalse(player.walkAlong(List.of()));
        assertFalse(player.walkAlong(null));
        assertFalse(player.isMoving());
    }
}
