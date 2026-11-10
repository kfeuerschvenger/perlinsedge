package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Bridge;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Wall;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathfinderTest {

    private static final int SIZE = 7;

    private static TileMap grassMap() {
        TileMap map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    private static void wall(TileMap map, int x, int y) {
        map.getTile(x, y).setBuilding(new Wall(x, y));
    }

    private static PathNode last(List<PathNode> path) {
        return path.get(path.size() - 1);
    }

    @Test
    void findsStraightPathExcludingTheStart() {
        List<PathNode> path = new Pathfinder(grassMap()).findPath(0, 0, 3, 0);

        assertNotNull(path);
        assertEquals(3, path.size());
        assertEquals(3, last(path).x);
        assertEquals(0, last(path).y);
        assertTrue(path.stream().noneMatch(n -> n.x == 0 && n.y == 0), "start tile must not be part of the path");
    }

    @Test
    void pathToTheStartTileIsEmpty() {
        List<PathNode> path = new Pathfinder(grassMap()).findPath(2, 2, 2, 2);

        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    @Test
    void usesDiagonalNeighboursWhenTheyAreShorter() {
        List<PathNode> path = new Pathfinder(grassMap()).findPath(0, 0, 3, 3);

        assertNotNull(path);
        assertEquals(3, path.size(), "three diagonal steps beat six cardinal ones");
        assertEquals(3, last(path).x);
        assertEquals(3, last(path).y);
    }

    @Test
    void consecutiveNodesAreNeighbours() {
        List<PathNode> path = new Pathfinder(grassMap()).findPath(0, 0, 5, 3);

        assertNotNull(path);
        int prevX = 0;
        int prevY = 0;
        for (PathNode node : path) {
            assertTrue(Math.abs(node.x - prevX) <= 1 && Math.abs(node.y - prevY) <= 1, "steps move at most one tile");
            prevX = node.x;
            prevY = node.y;
        }
    }

    @Test
    void routesAroundASolidBuilding() {
        TileMap map = grassMap();
        for (int y = 0; y < SIZE - 1; y++) {
            wall(map, 3, y);
        }

        List<PathNode> path = new Pathfinder(map).findPath(0, 0, 6, 0);

        assertNotNull(path);
        assertTrue(path.stream().anyMatch(n -> n.y == SIZE - 1), "the only gap is on the last row");
        assertTrue(path.stream().noneMatch(n -> n.x == 3 && n.y < SIZE - 1), "walls are never entered");
    }

    @Test
    void returnsNullWhenTheTargetIsNotWalkable() {
        TileMap map = grassMap();
        map.setTile(4, 4, new Tile(4, 4, TileType.MOUNTAIN));

        assertNull(new Pathfinder(map).findPath(0, 0, 4, 4));
    }

    @Test
    void returnsNullWhenTheStartIsNotWalkable() {
        TileMap map = grassMap();
        map.setTile(0, 0, new Tile(0, 0, TileType.DEEP_WATER));

        assertNull(new Pathfinder(map).findPath(0, 0, 3, 3));
    }

    @Test
    void returnsNullWhenTheTargetIsWalledIn() {
        TileMap map = grassMap();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    wall(map, 4 + dx, 4 + dy);
                }
            }
        }

        assertNull(new Pathfinder(map).findPath(0, 0, 4, 4));
    }

    @Test
    void returnsNullForOutOfBoundsPositions() {
        Pathfinder pathfinder = new Pathfinder(grassMap());

        assertNull(pathfinder.findPath(0, 0, SIZE, SIZE));
        assertNull(pathfinder.findPath(-1, 0, 2, 2));
    }

    @Test
    void waterBlocksTheWayUnlessABridgeIsBuilt() {
        TileMap map = grassMap();
        for (int y = 0; y < SIZE; y++) {
            map.setTile(3, y, new Tile(3, y, TileType.DEEP_WATER));
        }
        assertNull(new Pathfinder(map).findPath(0, 3, 6, 3));

        map.getTile(3, 3).setBuilding(new Bridge(3, 3));

        List<PathNode> path = new Pathfinder(map).findPath(0, 3, 6, 3);
        assertNotNull(path);
        assertTrue(path.stream().anyMatch(n -> n.x == 3 && n.y == 3));
    }

    @Test
    void diagonalStepsMayCutBetweenTwoBlockedTiles() {
        TileMap map = grassMap();
        wall(map, 1, 0);
        wall(map, 0, 1);

        List<PathNode> path = new Pathfinder(map).findPath(0, 0, 1, 1);

        assertNotNull(path);
        assertEquals(1, path.size(), "current behaviour: no corner-cutting check");
    }

    @Test
    void constructorRejectsNullMap() {
        assertThrows(NullPointerException.class, () -> new Pathfinder(null));
    }
}
