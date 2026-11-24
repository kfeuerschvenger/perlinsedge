package com.feuerschvenger.perlinsedge.domain.world.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileMapSpawnTest {

    private static final int WIDTH = 8;
    private static final int HEIGHT = 6;

    @Test
    void aMapBuiltWithoutASpawnHasNone() {
        assertTrue(new TileMap(WIDTH, HEIGHT).getSpawn().isEmpty());
    }

    @Test
    void aMapKeepsTheSpawnItWasBuiltWith() {
        final var spawn = new SpawnPoint(WIDTH - 1, HEIGHT - 1);

        assertEquals(spawn, new TileMap(WIDTH, HEIGHT, spawn).getSpawn().orElseThrow());
    }

    @Test
    void aSpawnOutsideTheMapIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TileMap(WIDTH, HEIGHT, new SpawnPoint(WIDTH, 0)));
        assertThrows(IllegalArgumentException.class, () -> new TileMap(WIDTH, HEIGHT, new SpawnPoint(0, HEIGHT)));
    }

    @Test
    void aNegativeSpawnPointCannotBeCreated() {
        assertThrows(IllegalArgumentException.class, () -> new SpawnPoint(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new SpawnPoint(0, -1));
    }
}
