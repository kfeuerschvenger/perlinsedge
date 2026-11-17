package com.feuerschvenger.perlinsedge.domain.world.model;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A tile belongs to one map at a time, and a tile that was replaced stops reporting its changes to the map.
 */
class TileOwnershipTest {

    @Test
    void aTileOwnedByAnotherMapIsRejectedAndNothingChanges() {
        final TileMap first = new TileMap(3, 3);
        final TileMap second = new TileMap(3, 3);
        final Tile tile = new Tile(1, 1, TileType.GRASS);
        first.setTile(1, 1, tile);
        final long revision = second.getRevision();

        assertThrows(IllegalStateException.class, () -> second.setTile(0, 0, tile));

        assertEquals(null, second.getTile(0, 0), "the grid was not touched");
        assertEquals(revision, second.getRevision());
        assertTrue(tile.isOwnedBy(first));
    }

    @Test
    void putTheSameTileBackIntoItsOwnMapIsAllowed() {
        final TileMap map = new TileMap(3, 3);
        final Tile tile = new Tile(1, 1, TileType.GRASS);
        map.setTile(1, 1, tile);

        map.setTile(1, 1, tile);

        assertSame(tile, map.getTile(1, 1));
        assertTrue(tile.isOwnedBy(map));
    }

    @Test
    void aReplacedTileIsDetachedAndNoLongerMarksTheMapChanged() {
        final TileMap map = new TileMap(3, 3);
        final Tile old = new Tile(1, 1, TileType.GRASS);
        map.setTile(1, 1, old);
        map.setTile(1, 1, new Tile(1, 1, TileType.SAND));
        final long revision = map.getRevision();

        old.setBuilding(new Fence(1, 1));

        assertFalse(old.isOwnedBy(map));
        assertEquals(revision, map.getRevision(), "a tile that left the map does not count as a change of it");
    }

    @Test
    void aDetachedTileCanJoinAnotherMap() {
        final TileMap first = new TileMap(3, 3);
        final TileMap second = new TileMap(3, 3);
        final Tile tile = new Tile(1, 1, TileType.GRASS);
        first.setTile(1, 1, tile);
        first.setTile(1, 1, null);

        second.setTile(1, 1, tile);

        assertTrue(tile.isOwnedBy(second));
    }
}
