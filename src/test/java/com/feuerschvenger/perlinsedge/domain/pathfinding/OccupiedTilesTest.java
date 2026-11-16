package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The tile check that runs every tick answers exactly what the set of taken tiles holds. */
class OccupiedTilesTest {

    @Test
    void anEnemyTakesTheTileItStandsOnAndTheTileItSteppingTo() {
        Skeleton skeleton = new Skeleton(3, 3);
        skeleton.setTargetTileX(4);
        skeleton.setTargetTileY(3);
        List<Skeleton> enemies = List.of(skeleton);

        assertTrue(OccupiedTiles.contains(enemies, 3, 3));
        assertTrue(OccupiedTiles.contains(enemies, 4, 3));
        assertFalse(OccupiedTiles.contains(enemies, 5, 3));
        assertFalse(OccupiedTiles.contains(enemies, 3, 4));
    }

    @Test
    void aDeadEnemyAndNoEnemiesTakeNothing() {
        Skeleton skeleton = new Skeleton(3, 3);
        skeleton.takeDamage(1000);

        assertFalse(OccupiedTiles.contains(List.of(skeleton), 3, 3));
        assertFalse(OccupiedTiles.contains(null, 3, 3));
        assertFalse(OccupiedTiles.contains(List.of(), 3, 3));
    }

    @Test
    void theCheckAgreesWithTheSetOfTakenTiles() {
        Skeleton first = new Skeleton(1, 1);
        Skeleton second = new Skeleton(5, 2);
        second.setTargetTileX(5);
        second.setTargetTileY(3);
        List<Skeleton> enemies = List.of(first, second);

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                assertEquals(OccupiedTiles.of(enemies).contains(new PathNode(x, y)),
                        OccupiedTiles.contains(enemies, x, y), "tile " + x + "," + y);
            }
        }
    }
}
