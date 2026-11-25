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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    private static final int TREE_X = 3;
    private static final int TREE_Y = 8;
    private static final int STONE_MARKER = 0xFFCFCFCF;
    private static final int IRON_MARKER = 0xFFB5502E;
    private static final int CRYSTALS_MARKER = 0xFF4FE0E8;
    private static final int WIDE_WIDTH = 30;
    private static final int WIDE_HEIGHT = 12;
    private static final int WIDE_STONE_X = 25;
    private static final int WIDE_STONE_Y = 3;
    private static final int WIDE_CORNER_X = 0;
    private static final int WIDE_CORNER_Y = 11;
    private static final double SMALL_TILE_WIDTH = 0.5;
    private static final double SMALL_TILE_HEIGHT = 0.25;
    private static final MapType MAP_TYPE = MapType.DENSE_FOREST;
    private static final TilePalette FLAT_PALETTE = TilePalette.of(Map.of(), null);
    private static final Layout LAYOUT = new Layout(IMAGE_WIDTH, IMAGE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, OFFSET_X,
            OFFSET_Y);
    /** A layout in which the tiles (6, 5), (6, 6) and (7, 6) fall on the same pixel. */
    private static final Layout SMALL_LAYOUT = new Layout(IMAGE_WIDTH, IMAGE_HEIGHT, SMALL_TILE_WIDTH,
            SMALL_TILE_HEIGHT, OFFSET_X, OFFSET_Y);

    /** Builds a map of rocky ground with the given size. */
    private static TileMap groundMap(int width, int height) {
        final var map = new TileMap(width, height);
        for (var x = 0; x < width; x++) {
            for (var y = 0; y < height; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.ROCKY_GROUND));
            }
        }
        return map;
    }

    /** Builds a map of rocky ground with a stone on two tiles. */
    private static TileMap mapWithTwoStones() {
        final var map = groundMap(MAP_SIZE, MAP_SIZE);
        put(map, STONE_X, STONE_Y, ResourceType.STONE);
        put(map, OTHER_X, OTHER_Y, ResourceType.STONE);
        return map;
    }

    /** Puts a resource of a type on a tile. */
    private static void put(TileMap map, int x, int y, ResourceType type) {
        map.getTile(x, y).setWorldResource(new Resource(type, 1L));
    }

    /** Builds the pixels of a map the way the drawer does. */
    private static MinimapPixels pixelsOf(TileMap map, Layout layout) {
        return new MinimapPixels(map, FxMinimapDrawer.samplePixels(map, FLAT_PALETTE, MAP_TYPE, layout), layout);
    }

    /** The index of the pixel under the centre of a tile. */
    private static int centreIndex(Layout layout, int x, int y) {
        final int px = (int) Math.floor((x - y) * layout.tileW() + layout.offsetX());
        final int py = (int) Math.floor((x + y + 1) * layout.tileH() + layout.offsetY());
        return py * layout.imageWidth() + px;
    }

    private static int ground() {
        return FLAT_PALETTE.argb(MAP_TYPE, TileType.ROCKY_GROUND);
    }

    @Test
    void stoneIronAndCrystalsGetOneOpaquePixelAtTheCentreOfTheirTile() {
        final var map = groundMap(MAP_SIZE, MAP_SIZE);
        put(map, STONE_X, STONE_Y, ResourceType.STONE);
        put(map, OTHER_X, OTHER_Y, ResourceType.CRYSTALS);
        put(map, TREE_X, TREE_Y, ResourceType.IRON_ORE);

        final var pixels = pixelsOf(map, LAYOUT);

        assertEquals(STONE_MARKER, pixels.pixelAt(centreIndex(LAYOUT, STONE_X, STONE_Y)));
        assertEquals(CRYSTALS_MARKER, pixels.pixelAt(centreIndex(LAYOUT, OTHER_X, OTHER_Y)));
        assertEquals(IRON_MARKER, pixels.pixelAt(centreIndex(LAYOUT, TREE_X, TREE_Y)));
    }

    @Test
    void treesAndEmptyTilesKeepTheColourOfTheirTile() {
        final var map = groundMap(MAP_SIZE, MAP_SIZE);
        put(map, TREE_X, TREE_Y, ResourceType.TREE);
        final var before = FxMinimapDrawer.samplePixels(map, FLAT_PALETTE, MAP_TYPE, LAYOUT);

        final var pixels = pixelsOf(map, LAYOUT);

        assertEquals(before[centreIndex(LAYOUT, TREE_X, TREE_Y)], pixels.pixelAt(centreIndex(LAYOUT, TREE_X, TREE_Y)));
        assertEquals(before[centreIndex(LAYOUT, 0, 0)], pixels.pixelAt(centreIndex(LAYOUT, 0, 0)));
    }

    @Test
    void aDepletedResourceHasNoMarker() {
        final var map = groundMap(MAP_SIZE, MAP_SIZE);
        final var stone = new Resource(ResourceType.STONE, 1L);
        map.getTile(STONE_X, STONE_Y).setWorldResource(stone);
        stone.takeDamage(Integer.MAX_VALUE);

        final var pixels = pixelsOf(map, LAYOUT);

        assertEquals(ground(), pixels.pixelAt(centreIndex(LAYOUT, STONE_X, STONE_Y)));
    }

    @Test
    void aMarkerOutsideTheBufferIsSkipped() {
        final var map = mapWithTwoStones();
        final var outside = new Layout(IMAGE_WIDTH, IMAGE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, OFFSET_X + IMAGE_WIDTH,
                OFFSET_Y);

        final var pixels = new MinimapPixels(map, new int[IMAGE_WIDTH * IMAGE_HEIGHT], outside);

        for (var pixel : pixels.snapshot()) {
            assertEquals(0, pixel);
        }
    }

    @Test
    void aMarkerDisappearsWhenItsResourceIsRemoved() {
        final var map = mapWithTwoStones();
        final var pixels = pixelsOf(map, LAYOUT);
        assertEquals(STONE_MARKER, pixels.pixelAt(centreIndex(LAYOUT, STONE_X, STONE_Y)));

        map.getTile(STONE_X, STONE_Y).setWorldResource(null);
        assertTrue(pixels.isStale());
        final var changed = pixels.refresh();

        assertArrayEquals(new int[]{centreIndex(LAYOUT, STONE_X, STONE_Y)}, changed);
        assertEquals(ground(), pixels.pixelAt(centreIndex(LAYOUT, STONE_X, STONE_Y)));
        assertEquals(STONE_MARKER, pixels.pixelAt(centreIndex(LAYOUT, OTHER_X, OTHER_Y)),
                "the other stone keeps its marker");
    }

    @Test
    void aMapThatDidNotChangeIsNotStale() {
        final var pixels = pixelsOf(mapWithTwoStones(), LAYOUT);

        assertFalse(pixels.isStale());
        assertEquals(0, pixels.refresh().length);
    }

    @Test
    void aChangeThatLeavesTheMarkersAloneRewritesNoPixel() {
        final var map = mapWithTwoStones();
        final var pixels = pixelsOf(map, LAYOUT);
        map.getTile(0, 0).setWorldResource(new Resource(ResourceType.TREE, 1L));

        assertTrue(pixels.isStale());
        assertEquals(0, pixels.refresh().length);
        assertFalse(pixels.isStale());
    }

    @Test
    void refreshingBringsThePixelsUpToDate() {
        final var map = mapWithTwoStones();
        final var pixels = pixelsOf(map, LAYOUT);
        map.getTile(OTHER_X, OTHER_Y).setWorldResource(null);

        pixels.refresh();

        assertFalse(pixels.isStale());
    }

    @Test
    void theHighestPriorityMarkerShowsWhenTilesShareAPixel() {
        final var map = groundMap(MAP_SIZE, MAP_SIZE);
        put(map, 6, 5, ResourceType.STONE);
        put(map, 6, 6, ResourceType.CRYSTALS);
        put(map, 7, 6, ResourceType.IRON_ORE);

        final var pixels = pixelsOf(map, SMALL_LAYOUT);

        assertEquals(centreIndex(SMALL_LAYOUT, 6, 5), centreIndex(SMALL_LAYOUT, 7, 6));
        assertEquals(CRYSTALS_MARKER, pixels.pixelAt(centreIndex(SMALL_LAYOUT, 6, 5)));
    }

    @Test
    void harvestingOneResourceOfASharedPixelLeavesTheMarkerOfTheOthers() {
        final var map = groundMap(MAP_SIZE, MAP_SIZE);
        put(map, 6, 5, ResourceType.STONE);
        put(map, 6, 6, ResourceType.CRYSTALS);
        put(map, 7, 6, ResourceType.IRON_ORE);
        final var pixels = pixelsOf(map, SMALL_LAYOUT);
        final var shared = centreIndex(SMALL_LAYOUT, 6, 5);

        map.getTile(6, 6).setWorldResource(null);
        pixels.refresh();
        assertEquals(IRON_MARKER, pixels.pixelAt(shared), "the iron is next in priority");

        map.getTile(7, 6).setWorldResource(null);
        pixels.refresh();
        assertEquals(STONE_MARKER, pixels.pixelAt(shared), "the stone is the last one left");

        map.getTile(6, 5).setWorldResource(null);
        pixels.refresh();
        assertEquals(ground(), pixels.pixelAt(shared), "nothing is left on the pixel");
    }

    @Test
    void aLowerPriorityMarkerHarvestedFirstDoesNotHideAHigherOne() {
        final var map = groundMap(MAP_SIZE, MAP_SIZE);
        put(map, 6, 5, ResourceType.STONE);
        put(map, 6, 6, ResourceType.CRYSTALS);
        final var pixels = pixelsOf(map, SMALL_LAYOUT);

        map.getTile(6, 5).setWorldResource(null);
        pixels.refresh();

        assertEquals(CRYSTALS_MARKER, pixels.pixelAt(centreIndex(SMALL_LAYOUT, 6, 6)));
    }

    @Test
    void aMapThatIsNotSquareKeepsItsMarkersAndLosesThemOnHarvest() {
        final var layout = new Layout(IMAGE_WIDTH, IMAGE_HEIGHT, TILE_WIDTH, TILE_HEIGHT, OFFSET_X, OFFSET_Y);
        final var map = groundMap(WIDE_WIDTH, WIDE_HEIGHT);
        put(map, WIDE_STONE_X, WIDE_STONE_Y, ResourceType.STONE);
        put(map, WIDE_CORNER_X, WIDE_CORNER_Y, ResourceType.CRYSTALS);
        final var pixels = pixelsOf(map, layout);
        assertEquals(STONE_MARKER, pixels.pixelAt(centreIndex(layout, WIDE_STONE_X, WIDE_STONE_Y)));
        assertEquals(CRYSTALS_MARKER, pixels.pixelAt(centreIndex(layout, WIDE_CORNER_X, WIDE_CORNER_Y)));

        map.getTile(WIDE_STONE_X, WIDE_STONE_Y).setWorldResource(null);
        pixels.refresh();

        assertEquals(ground(), pixels.pixelAt(centreIndex(layout, WIDE_STONE_X, WIDE_STONE_Y)));
        assertEquals(CRYSTALS_MARKER, pixels.pixelAt(centreIndex(layout, WIDE_CORNER_X, WIDE_CORNER_Y)));
    }

    @Test
    void theCallerArraysAndTheLeakedCopiesDoNotReachTheInternalPixels() {
        final var map = mapWithTwoStones();
        final var colours = FxMinimapDrawer.samplePixels(map, FLAT_PALETTE, MAP_TYPE, LAYOUT);
        final var pixels = new MinimapPixels(map, colours, LAYOUT);
        final var stone = centreIndex(LAYOUT, STONE_X, STONE_Y);

        colours[stone] = 0;
        pixels.snapshot()[stone] = 0;
        map.getTile(OTHER_X, OTHER_Y).setWorldResource(null);
        pixels.refresh();

        assertEquals(STONE_MARKER, pixels.pixelAt(stone));
        assertEquals(ground(), pixels.pixelAt(centreIndex(LAYOUT, OTHER_X, OTHER_Y)));
    }
}
