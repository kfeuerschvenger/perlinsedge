package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.pathfinding.Pathfinder;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FenceGateTest {

    private static final int SIZE = 9;
    private TileMap map;

    @BeforeEach
    void setUp() {
        map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
    }

    /** A fence line along x = 4 with a gate at (4, 4), the only opening between the two halves of the map. */
    private FenceGate fenceWithGate() {
        for (int y = 0; y < SIZE; y++) {
            map.getTile(4, y).setBuilding(new Fence(4, y));
        }
        FenceGate gate = new FenceGate(4, 4);
        map.getTile(4, 4).setBuilding(gate);
        return gate;
    }

    @Test
    void aNewGateIsClosedAndSolid() {
        FenceGate gate = new FenceGate(1, 1);

        assertFalse(gate.isOpen());
        assertTrue(gate.isSolid());
        assertTrue(gate.isInteractable());
    }

    @Test
    void interactingTogglesTheGateAndItsSolidity() {
        FenceGate gate = new FenceGate(4, 4);
        map.getTile(4, 4).setBuilding(gate);

        assertTrue(gate.onInteract(map.getTile(4, 4)));
        assertTrue(gate.isOpen());
        assertFalse(gate.isSolid());
        assertTrue(map.getTile(4, 4).isWalkable());

        assertTrue(gate.onInteract(map.getTile(4, 4)));
        assertFalse(gate.isOpen());
        assertFalse(map.getTile(4, 4).isWalkable());
    }

    @Test
    void aGateIsNotToggledThroughANullTileOrATileItDoesNotStandOn() {
        FenceGate gate = new FenceGate(4, 4);
        map.getTile(4, 4).setBuilding(gate);

        assertThrows(NullPointerException.class, () -> gate.toggleOpen(null));
        assertThrows(IllegalArgumentException.class, () -> gate.toggleOpen(map.getTile(1, 1)));
        assertFalse(gate.isOpen());
    }

    @Test
    void togglingTellsTheMapSoShadowsAreCastAgain() {
        FenceGate gate = new FenceGate(4, 4);
        map.getTile(4, 4).setBuilding(gate);
        final long before = map.getRevision();

        gate.onInteract(map.getTile(4, 4));

        assertTrue(map.getRevision() > before);
    }

    @Test
    void aClosedGateBlocksTheWayLikeAFence() {
        fenceWithGate();

        assertFalse(new Pathfinder(map).findPath(1, 4, 7, 4).reachable(), "the whole line is closed");
    }

    @Test
    void anOpenGateLetsPathfindingThroughAndClosingItBlocksAgain() {
        FenceGate gate = fenceWithGate();
        gate.toggleOpen(map.getTile(4, 4));

        var path = new Pathfinder(map).findPath(1, 4, 7, 4).requireNodes();
        assertNotNull(path);
        assertTrue(path.stream().anyMatch(n -> n.x == 4 && n.y == 4), "the path goes through the gate");

        gate.toggleOpen(map.getTile(4, 4));
        assertFalse(new Pathfinder(map).findPath(1, 4, 7, 4).reachable());
    }

    @Test
    void anEnemyPathfindingWithItsOccupiedTilesAlsoTreatsAClosedGateAsBlocked() {
        // The enemies find their way with the same walkability rule and a set of tiles other enemies stand on
        fenceWithGate();
        Pathfinder enemy = new Pathfinder(map);

        assertFalse(enemy.findPath(1, 4, 7, 4, 5000).reachable());
    }

    @Test
    void fencesBlockMovementWhateverTheirSides() {
        map.getTile(3, 3).setBuilding(new Fence(3, 3));

        assertFalse(map.getTile(3, 3).isWalkable());
    }

    @Test
    void theGateRunsTowardTheNeighboursOnItsAxis() {
        FenceGate alongX = new FenceGate(1, 1);
        FenceGate alongY = new FenceGate(1, 1);
        alongY.setRotation(1);

        assertTrue(alongX.runsToward(1, 0));
        assertTrue(alongX.runsToward(-1, 0));
        assertFalse(alongX.runsToward(0, 1));
        assertTrue(alongY.runsToward(0, 1));
        assertTrue(alongY.runsToward(0, -1));
        assertFalse(alongY.runsToward(1, 0));
    }

    @Test
    void aGateTurnsBetweenTwoAxesOnly() {
        FenceGate gate = new FenceGate(1, 1);

        gate.setRotation(3);

        assertEquals(1, gate.getRotation());
        assertEquals(2, BuildingType.FENCE_GATE.rotationCount());
        assertTrue(Arrays.stream(BuildingType.values()).anyMatch(t -> t == BuildingType.FENCE_GATE));
    }
}
