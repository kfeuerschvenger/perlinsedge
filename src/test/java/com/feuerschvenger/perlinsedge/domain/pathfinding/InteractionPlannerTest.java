package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Wall;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionPlannerTest {

    private static final int SIZE = 9;

    private static TileMap grassMap() {
        TileMap map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    private static PathNode last(List<PathNode> path) {
        return path.get(path.size() - 1);
    }

    @Test
    void reachIsTheEightTilesAroundAndTheTileItself() {
        assertTrue(InteractionPlanner.inReach(4, 4, 4, 4));
        assertTrue(InteractionPlanner.inReach(4, 4, 5, 5));
        assertTrue(InteractionPlanner.inReach(4, 4, 3, 4));
        assertFalse(InteractionPlanner.inReach(4, 4, 6, 4));
        assertFalse(InteractionPlanner.inReach(4, 4, 4, 2));
    }

    @Test
    void aPlayerInReachNeedsNoWalk() {
        List<PathNode> path = InteractionPlanner.pathToReach(grassMap(), 4, 4, 5, 5);

        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    @Test
    void theWalkEndsOnATileNextToTheTarget() {
        List<PathNode> path = InteractionPlanner.pathToReach(grassMap(), 0, 4, 6, 4);

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertTrue(InteractionPlanner.inReach(last(path).x, last(path).y, 6, 4));
        assertFalse(last(path).x == 6 && last(path).y == 4, "the walk does not step onto the target");
    }

    @Test
    void theWalkEndsOnTheNearestSideOfTheTarget() {
        List<PathNode> path = InteractionPlanner.pathToReach(grassMap(), 0, 4, 6, 4);

        assertEquals(5, last(path).x, "coming from the left, the left side is the nearest");
        assertEquals(4, last(path).y);
    }

    @Test
    void aSolidTargetIsApproachedThroughTheTilesAroundIt() {
        TileMap map = grassMap();
        map.getTile(6, 4).setBuilding(new Wall(6, 4));

        List<PathNode> path = InteractionPlanner.pathToReach(map, 0, 4, 6, 4);

        assertNotNull(path);
        assertTrue(path.stream().noneMatch(node -> node.x == 6 && node.y == 4));
    }

    @Test
    void aBlockedSideIsSkippedForAnOpenOne() {
        TileMap map = grassMap();
        map.getTile(5, 4).setBuilding(new Wall(5, 4));

        List<PathNode> path = InteractionPlanner.pathToReach(map, 0, 4, 6, 4);

        assertNotNull(path);
        assertTrue(InteractionPlanner.inReach(last(path).x, last(path).y, 6, 4));
        assertFalse(last(path).x == 5 && last(path).y == 4, "the walled tile is not a place to stand");
    }

    @Test
    void aTargetWithNoWalkableTileAroundItCannotBeReached() {
        TileMap map = grassMap();
        for (int x = 5; x <= 7; x++) {
            for (int y = 3; y <= 5; y++) {
                if (x != 6 || y != 4) {
                    map.setTile(x, y, new Tile(x, y, TileType.DEEP_WATER));
                }
            }
        }

        assertNull(InteractionPlanner.pathToReach(map, 0, 4, 6, 4));
    }

    @Test
    void aTargetOnAnIslandTheLandCannotReachGivesNoPath() {
        TileMap map = grassMap();
        for (int y = 0; y < SIZE; y++) {
            map.setTile(4, y, new Tile(4, y, TileType.DEEP_WATER));
        }

        assertNull(InteractionPlanner.pathToReach(map, 0, 4, 7, 4));
    }
}
