package com.feuerschvenger.perlinsedge.domain.world.model;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Wall;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The revision of a map goes up with every change of what stands on its tiles, and only then.
 */
class TileMapRevisionTest {

    private static TileMap mapWithTile() {
        final TileMap map = new TileMap(4, 4);
        map.setTile(1, 1, new Tile(1, 1, TileType.GRASS));
        return map;
    }

    @Test
    void putting_a_tile_into_the_map_changes_the_revision() {
        final TileMap map = new TileMap(4, 4);
        final long before = map.getRevision();

        map.setTile(0, 0, new Tile(0, 0, TileType.GRASS));

        assertNotEquals(before, map.getRevision());
    }

    @Test
    void a_tile_put_outside_the_map_changes_nothing() {
        final TileMap map = new TileMap(4, 4);
        final long before = map.getRevision();

        map.setTile(9, 9, new Tile(9, 9, TileType.GRASS));

        assertEquals(before, map.getRevision());
    }

    @Test
    void setting_a_resource_changes_the_revision() {
        final TileMap map = mapWithTile();
        final long before = map.getRevision();

        map.getTile(1, 1).setWorldResource(new Resource(ResourceType.TREE, 1));

        assertNotEquals(before, map.getRevision());
    }

    @Test
    void removing_a_resource_changes_the_revision() {
        final TileMap map = mapWithTile();
        map.getTile(1, 1).setWorldResource(new Resource(ResourceType.TREE, 1));
        final long before = map.getRevision();

        map.getTile(1, 1).setWorldResource(null);

        assertNotEquals(before, map.getRevision());
    }

    @Test
    void replacing_a_resource_changes_the_revision() {
        final TileMap map = mapWithTile();
        map.getTile(1, 1).setWorldResource(new Resource(ResourceType.TREE, 1));
        final long before = map.getRevision();

        map.getTile(1, 1).setWorldResource(new Resource(ResourceType.STONE, 2));

        assertNotEquals(before, map.getRevision());
    }

    @Test
    void placing_and_removing_a_building_change_the_revision() {
        final TileMap map = mapWithTile();
        final long start = map.getRevision();

        map.getTile(1, 1).setBuilding(new Wall(1, 1));
        final long placed = map.getRevision();
        map.getTile(1, 1).setBuilding(null);
        final long removed = map.getRevision();

        assertNotEquals(start, placed);
        assertNotEquals(placed, removed);
    }

    @Test
    void changing_the_terrain_type_changes_the_revision() {
        final TileMap map = mapWithTile();
        final long before = map.getRevision();

        map.getTile(1, 1).setType(TileType.SAND);

        assertNotEquals(before, map.getRevision());
    }

    @Test
    void reading_the_map_never_changes_the_revision() {
        final TileMap map = mapWithTile();
        map.getTile(1, 1).setWorldResource(new Resource(ResourceType.TREE, 1));
        final long before = map.getRevision();

        final Tile tile = map.getTile(1, 1);
        tile.hasResource();
        tile.hasBuilding();
        tile.getWorldResource();
        tile.getBuilding();
        tile.isWalkable();
        tile.isBuildable();
        map.getTile(3, 3);
        map.isValidCoordinate(2, 2);

        assertEquals(before, map.getRevision());
    }

    @Test
    void a_tile_that_is_in_no_map_changes_no_revision() {
        final TileMap map = mapWithTile();
        final long before = map.getRevision();
        final Tile loose = new Tile(2, 2, TileType.GRASS);

        loose.setWorldResource(new Resource(ResourceType.TREE, 1));
        loose.setBuilding(new Wall(2, 2));

        assertEquals(before, map.getRevision());
    }
}
