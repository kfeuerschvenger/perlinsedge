package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections.LOWER_LEFT;
import static com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections.LOWER_RIGHT;
import static com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections.UPPER_LEFT;
import static com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections.UPPER_RIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A fence joins its four edge neighbours that are fences, and the gates that run along the line between the two.
 * Tile (x + 1, y) is drawn toward the lower right edge, (x, y + 1) toward the lower left, (x - 1, y) toward the upper
 * left and (x, y - 1) toward the upper right.
 */
class FenceConnectionsTest {

    private TileMap map;

    @BeforeEach
    void setUp() {
        map = new TileMap(7, 7);
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
    }

    private void fence(int x, int y) {
        map.getTile(x, y).setBuilding(new Fence(x, y));
        FenceConnections.refresh(map, x, y);
    }

    private FenceGate gate(int x, int y, int rotation) {
        FenceGate gate = new FenceGate(x, y);
        gate.setRotation(rotation);
        map.getTile(x, y).setBuilding(gate);
        FenceConnections.refresh(map, x, y);
        return gate;
    }

    private int mask(int x, int y) {
        return ((Fence) map.getTile(x, y).getBuilding()).getConnections();
    }

    @Test
    void aLonePostJoinsNothing() {
        fence(3, 3);

        assertEquals(0, mask(3, 3));
    }

    @Test
    void aStraightRunJoinsOnlyAlongItsLine() {
        fence(2, 3);
        fence(3, 3);
        fence(4, 3);

        assertEquals(LOWER_RIGHT, mask(2, 3), "its neighbour at +x is drawn toward the lower right");
        assertEquals(UPPER_LEFT | LOWER_RIGHT, mask(3, 3));
        assertEquals(UPPER_LEFT, mask(4, 3));
    }

    @Test
    void theOtherDiagonalRunUsesTheOtherPairOfSides() {
        fence(3, 2);
        fence(3, 3);
        fence(3, 4);

        assertEquals(UPPER_RIGHT | LOWER_LEFT, mask(3, 3));
        assertEquals(LOWER_LEFT, mask(3, 2));
        assertEquals(UPPER_RIGHT, mask(3, 4));
    }

    @Test
    void aCornerJoinsTwoSidesAtARightAngle() {
        fence(3, 3);
        fence(4, 3);
        fence(3, 4);

        assertEquals(LOWER_RIGHT | LOWER_LEFT, mask(3, 3));
        assertEquals(UPPER_LEFT, mask(4, 3));
        assertEquals(UPPER_RIGHT, mask(3, 4));
    }

    @Test
    void aTJoinsThreeSides() {
        fence(3, 3);
        fence(2, 3);
        fence(4, 3);
        fence(3, 4);

        assertEquals(UPPER_LEFT | LOWER_RIGHT | LOWER_LEFT, mask(3, 3));
    }

    @Test
    void aCrossJoinsAllFourSides() {
        fence(3, 3);
        fence(2, 3);
        fence(4, 3);
        fence(3, 2);
        fence(3, 4);

        assertEquals(UPPER_LEFT | UPPER_RIGHT | LOWER_LEFT | LOWER_RIGHT, mask(3, 3));
    }

    @Test
    void aDiagonalNeighbourIsNotJoined() {
        fence(3, 3);
        fence(4, 4);

        assertEquals(0, mask(3, 3));
        assertEquals(0, mask(4, 4));
    }

    @Test
    void aGateIsJoinedOnlyAlongItsAxis() {
        // Rotation 0 runs along the tile X axis, between the upper left and the lower right edge
        gate(4, 3, 0);
        fence(3, 3);
        assertEquals(LOWER_RIGHT, mask(3, 3), "the gate at +x runs toward the fence");

        fence(3, 2);
        // The gate at (4, 3) is not on the line between (3, 2) and its neighbours
        assertEquals(LOWER_LEFT, mask(3, 2), "only the fence below it");
    }

    @Test
    void aGateAcrossItsAxisIsNotJoined() {
        // Rotation 1 runs along the Y axis, so a fence at -x of it is beside the gate, not in line with it
        gate(4, 3, 1);
        fence(3, 3);

        assertEquals(0, mask(3, 3));
        FenceGate along = gate(3, 5, 1);
        fence(3, 4);
        assertEquals(UPPER_RIGHT | LOWER_LEFT, mask(3, 4), "the fence above it and the gate below it, which runs toward it");
        assertEquals(1, along.getRotation());
    }

    @Test
    void removingAFenceUpdatesItsNeighbours() {
        fence(2, 3);
        fence(3, 3);
        assertEquals(LOWER_RIGHT, mask(2, 3));

        map.getTile(3, 3).setBuilding(null);
        FenceConnections.refresh(map, 3, 3);

        assertEquals(0, mask(2, 3));
    }

    @Test
    void aGateJoiningOrLeavingChangesTheFenceNextToItAndTheRevision() {
        fence(3, 3);
        final long before = map.getRevision();

        gate(4, 3, 0);

        assertEquals(LOWER_RIGHT, mask(3, 3));
        assertTrue(map.getRevision() > before, "the map knows the fence changed");
    }

    @Test
    void maskAtAnEmptyTileSaysWhatAFenceWouldJoin() {
        fence(2, 3);
        fence(3, 4);

        assertEquals(UPPER_LEFT | LOWER_LEFT, FenceConnections.maskAt(map, 3, 3));
    }
}
