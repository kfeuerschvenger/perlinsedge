package com.feuerschvenger.perlinsedge.domain.entities;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Wall;
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
    void aWalkVisitsEveryNodeOfThePathAndNeverCutsAroundAnObstacle() {
        map.getTile(1, 1).setBuilding(new Wall(1, 1));
        assertTrue(player.moveTo(map, 2, 2));

        for (int i = 0; i < 240; i++) {
            player.update(1.0 / 60.0);
            final int tileX = (int) Math.round(player.getPosX());
            final int tileY = (int) Math.round(player.getPosY());
            assertFalse(tileX == 1 && tileY == 1, "the player entered the blocked tile at frame " + i);
        }

        assertTrue(player.getCurrentTileX() == 2 && player.getCurrentTileY() == 2);
    }

    @Test
    void stoppingMidStepFinishesTheStepAndEndsOnATile() {
        assertTrue(player.walkAlong(List.of(new PathNode(1, 0), new PathNode(2, 0), new PathNode(3, 0))));
        for (int i = 0; i < 4; i++) {
            player.update(1.0 / 60.0);
        }
        assertTrue(player.getPosX() != Math.rint(player.getPosX()), "the player is between two tiles");

        player.stop();
        for (int i = 0; i < 120; i++) {
            player.update(1.0 / 60.0);
        }

        assertFalse(player.isMoving());
        assertTrue(player.getPosX() == Math.rint(player.getPosX()) && player.getCurrentTileX() < 3);
    }

    @Test
    void stoppingOnATileStopsThere() {
        assertTrue(player.walkAlong(List.of(new PathNode(1, 0), new PathNode(2, 0))));

        player.stop();

        assertFalse(player.isMoving());
        assertTrue(player.getCurrentTileX() == 0 && player.getPosX() == 0);
    }

    private void updateFrames(int frames) {
        for (int i = 0; i < frames; i++) {
            player.update(1.0 / 60.0);
        }
    }

    @Test
    void aMoveOrderMidStepFinishesTheStepBeforeTurningAndNeverCutsABlockedCorner() {
        map.getTile(1, 1).setBuilding(new Wall(1, 1));
        assertTrue(player.walkAlong(List.of(new PathNode(1, 0), new PathNode(2, 0), new PathNode(3, 0))));
        updateFrames(4);
        assertTrue(player.getPosX() > 0 && player.getPosX() < 1, "the player is between (0, 0) and (1, 0)");

        assertTrue(player.moveTo(map, 0, 2));

        int tileChanges = 0;
        for (int i = 0; i < 240; i++) {
            final int before = player.getCurrentTileX() * 10 + player.getCurrentTileY();
            player.update(1.0 / 60.0);
            final int after = player.getCurrentTileX() * 10 + player.getCurrentTileY();
            if (before != after && tileChanges++ == 0) {
                assertTrue(player.getCurrentTileX() == 1 && player.getCurrentTileY() == 0,
                        "the step in progress ends on the tile that was being approached");
            }
            final int tileX = (int) Math.round(player.getPosX());
            final int tileY = (int) Math.round(player.getPosY());
            assertFalse(tileX == 1 && tileY == 1, "the player entered the blocked tile at frame " + i);
        }

        assertTrue(player.getCurrentTileX() == 0 && player.getCurrentTileY() == 2);
        assertFalse(player.isMoving());
    }

    @Test
    void aMoveOrderToTheTileBeingApproachedEndsThereWithoutTurningBack() {
        assertTrue(player.walkAlong(List.of(new PathNode(1, 0), new PathNode(2, 0), new PathNode(3, 0))));
        updateFrames(4);

        assertTrue(player.moveTo(map, 1, 0));
        updateFrames(120);

        assertTrue(player.getCurrentTileX() == 1 && player.getCurrentTileY() == 0);
        assertFalse(player.isMoving());
    }

    @Test
    void theWalkOriginIsTheTileBeingApproachedWhileBetweenTilesAndTheCurrentTileOtherwise() {
        assertTrue(player.getWalkOriginX() == 0 && player.getWalkOriginY() == 0);

        assertTrue(player.walkAlong(List.of(new PathNode(1, 0), new PathNode(2, 0))));
        assertTrue(player.getWalkOriginX() == 0, "still standing on the first tile");
        updateFrames(4);

        assertTrue(player.getWalkOriginX() == 1 && player.getWalkOriginY() == 0);
    }

    @Test
    void stoppingWithALongQueueMidStepThenWalkingAlongAgainFollowsOnlyTheNewPath() {
        assertTrue(player.walkAlong(List.of(new PathNode(1, 0), new PathNode(2, 0), new PathNode(3, 0),
                new PathNode(3, 1))));
        updateFrames(4);
        player.stop();

        // The new path starts next to the tile the player is stepping to
        assertTrue(player.walkAlong(List.of(new PathNode(1, 1), new PathNode(1, 2))));
        for (int i = 0; i < 240; i++) {
            player.update(1.0 / 60.0);
            assertTrue(player.getPosX() <= 1.0 + 1e-9, "no step of the earlier queue was left over, at frame " + i);
        }

        assertTrue(player.getCurrentTileX() == 1 && player.getCurrentTileY() == 2);
        assertFalse(player.isMoving());
    }

    @Test
    void aFailedMoveOrderMidStepStopsOnTheTileBeingApproached() {
        assertTrue(player.walkAlong(List.of(new PathNode(1, 0), new PathNode(2, 0), new PathNode(3, 0))));
        updateFrames(4);

        assertFalse(player.moveTo(map, 4, 2));
        updateFrames(120);

        assertTrue(player.getCurrentTileX() == 1 && player.getCurrentTileY() == 0);
        assertFalse(player.isMoving());
    }

    @Test
    void anEmptyPathChangesNothing() {
        assertFalse(player.walkAlong(List.of()));
        assertFalse(player.walkAlong(null));
        assertFalse(player.isMoving());
    }
}
