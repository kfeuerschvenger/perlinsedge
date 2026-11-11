package com.feuerschvenger.perlinsedge.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

class PlayerDeathTest {

    private static TileMap grass(int size) {
        TileMap map = new TileMap(size, size);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    @Test
    void aPlayerThatDiesMidWalkStopsWalking() {
        TileMap map = grass(20);
        Player player = new Player(2, 2);
        player.moveTo(map, 10, 10);
        assertTrue(player.isMoving());

        player.takeDamage(1_000_000);

        assertTrue(player.isDead());
        assertFalse(player.isMoving());
    }

    @Test
    void aDeadPlayerIgnoresMoveOrders() {
        TileMap map = grass(20);
        Player player = new Player(2, 2);
        player.takeDamage(1_000_000);

        player.moveTo(map, 10, 10);
        for (int i = 0; i < 120; i++) {
            player.update(1.0 / 60.0);
        }

        assertEquals(2, player.getCurrentTileX());
        assertEquals(2, player.getCurrentTileY());
    }
}
