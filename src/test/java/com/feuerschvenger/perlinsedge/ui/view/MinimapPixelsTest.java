package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.infra.fx.utils.TilePalette;
import com.feuerschvenger.perlinsedge.ui.view.FxMinimapDrawer.Layout;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that the markers of the minimap follow the resources of the map as they are harvested. */
class MinimapPixelsTest {

    private static final int MAP_SIZE = 20;
    private static final int IMAGE_WIDTH = 100;
    private static final int IMAGE_HEIGHT = 60;
    private static final double TILE_WIDTH = 2;
    private static final double TILE_HEIGHT = 1;
    private static final double OFFSET_X = 50;
    private static final double OFFSET_Y = 5;
    private static final int STONE_X = 6;
    private static final int STONE_Y = 5;
    private static final int OTHER_X = 12;
    private static final int OTHER_Y = 13;
    private static final int STONE_MARKER = 0xFFCFCFCF;
    private static final TilePalette FLAT_PALETTE = TilePalette.of(Map.of(), null);
    private static final Layout LAYOUT = new Layout(IMAGE_WIDTH, IMAGE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, OFFSET_X,
            OFFSET_Y);

    /** Builds a map of rocky ground with a stone on two tiles. */
    private static TileMap mapWithTwoStones() {
        final var map = new TileMap(MAP_SIZE, MAP_SIZE);
        for (var x = 0; x < MAP_SIZE; x++) {
            for (var y = 0; y < MAP_SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.ROCKY_GROUND));
            }
        }
        map.getTile(STONE_X, STONE_Y).setWorldResource(new Resource(ResourceType.STONE, 1L));
        map.getTile(OTHER_X, OTHER_Y).setWorldResource(new Resource(ResourceType.STONE, 1L));
        return map;
    }

    /** Builds the pixels of a map the way the drawer does. */
    private static MinimapPixels pixelsOf(TileMap map) {
        return new MinimapPixels(map, FxMinimapDrawer.samplePixels(map, FLAT_PALETTE, MapType.DENSE_FOREST, LAYOUT),
                LAYOUT);
    }

    /** The index of the pixel under the centre of a tile. */
    private static int centreIndex(int x, int y) {
        final int px = (int) Math.floor((x - y) * TILE_WIDTH + OFFSET_X);
        final int py = (int) Math.floor((x + y + 1) * TILE_HEIGHT + OFFSET_Y);
        return py * IMAGE_WIDTH + px;
    }

    @Test
    void aMarkerDisappearsWhenItsResourceIsRemoved() {
        final var map = mapWithTwoStones();
        final var pixels = pixelsOf(map);
        assertEquals(STONE_MARKER, pixels.pixels()[centreIndex(STONE_X, STONE_Y)]);

        map.getTile(STONE_X, STONE_Y).setWorldResource(null);
        assertTrue(pixels.isStale());
        final int[] refreshed = pixels.refresh();

        final int ground = FLAT_PALETTE.argb(MapType.DENSE_FOREST, TileType.ROCKY_GROUND);
        assertEquals(ground, refreshed[centreIndex(STONE_X, STONE_Y)]);
        assertEquals(STONE_MARKER, refreshed[centreIndex(OTHER_X, OTHER_Y)], "the other stone keeps its marker");
    }

    @Test
    void aMapThatDidNotChangeIsNotStale() {
        final var pixels = pixelsOf(mapWithTwoStones());

        assertFalse(pixels.isStale());
    }

    @Test
    void refreshingBringsThePixelsUpToDate() {
        final var map = mapWithTwoStones();
        final var pixels = pixelsOf(map);
        map.getTile(OTHER_X, OTHER_Y).setWorldResource(null);

        pixels.refresh();

        assertFalse(pixels.isStale());
    }
}
