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

class FxMinimapDrawerTest {

    private static final int OPAQUE_RED = 0xFFFF0000;
    private static final int FAINT_BLUE = 0x400000FF;
    private static final int MARKER_MAP_SIZE = 20;
    private static final int MARKER_IMAGE_W = 100;
    private static final int MARKER_IMAGE_H = 60;
    private static final double MARKER_TILE_W = 2;
    private static final double MARKER_TILE_H = 1;
    private static final double MARKER_OFFSET_X = 50;
    private static final double MARKER_OFFSET_Y = 5;
    private static final int STONE_X = 6;
    private static final int STONE_Y = 5;
    private static final int IRON_X = 10;
    private static final int IRON_Y = 4;
    private static final int CRYSTALS_X = 12;
    private static final int CRYSTALS_Y = 13;
    private static final int TREE_X = 3;
    private static final int TREE_Y = 8;
    private static final MapType MAP_TYPE = MapType.DENSE_FOREST;
    /** A palette without art, so every tile type has its flat colour. */
    private static final TilePalette FLAT_PALETTE = TilePalette.of(Map.of(), null);

    private static TileMap uniformMap(int size, TileType type) {
        TileMap map = new TileMap(size, size);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                map.setTile(x, y, new Tile(x, y, type));
            }
        }
        return map;
    }

    /** Counts the unpainted pixels among those that lie well inside the map, where nothing may be missing. */
    private static int holesInside(double tileW, double tileH, double offsetX, double offsetY, int size) {
        TileMap map = uniformMap(size, TileType.ROCKY_GROUND);
        int width = 400;
        int height = 240;
        int[] pixels = FxMinimapDrawer.samplePixels(map, FLAT_PALETTE, MAP_TYPE,
                new Layout(width, height, tileW, tileH, offsetX, offsetY));
        int holes = 0;
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                double compX = (px + 0.5 - offsetX) / tileW;
                double compY = (py + 0.5 - offsetY) / tileH;
                double tileX = (compX + compY) / 2.0;
                double tileY = (compY - compX) / 2.0;
                boolean wellInside = tileX > 2 && tileY > 2 && tileX < size - 2 && tileY < size - 2;
                if (wellInside && pixels[py * width + px] >>> 24 != 0xFF) {
                    holes++;
                }
            }
        }
        return holes;
    }

    @Test
    void everyPixelInsideTheMapIsPaintedAtAWholePixelScale() {
        assertEquals(0, holesInside(4, 2, 200, 20, 60));
    }

    @Test
    void everyPixelInsideTheMapIsPaintedAtAFractionalScale() {
        assertEquals(0, holesInside(1.37, 0.685, 200, 20, 90));
        assertEquals(0, holesInside(0.61, 0.305, 200, 20, 200));
    }

    @Test
    void pixelsOutsideTheMapStayTransparent() {
        TileMap map = uniformMap(10, TileType.ROCKY_GROUND);

        int[] pixels = FxMinimapDrawer.samplePixels(map, FLAT_PALETTE, MAP_TYPE, new Layout(100, 60, 2, 1, 50, 5));

        assertEquals(0, pixels[0], "the corner of the image is far from the map");
        assertEquals(0, pixels[59 * 100 + 99]);
    }

    @Test
    void aTranslucentSampleWeighsLessInTheColourOfAPixel() {
        int mixed = FxMinimapDrawer.blend(new int[]{OPAQUE_RED, FAINT_BLUE, 0, 0});

        // Alpha: (255 + 64) / 4 of a full pixel. Colour: red 255 / 319 = 0.8 and blue 64 / 319 = 0.2
        assertEquals(0x50CC0033, mixed);
    }

    @Test
    void aPixelHalfOffTheMapKeepsTheExactColourOfTheTilesUnderIt() {
        int rocky = 0xFF336699;

        int edge = FxMinimapDrawer.blend(new int[]{rocky, rocky, 0, 0});

        assertEquals(0x80, edge >>> 24, "half the samples are off the map");
        assertEquals(rocky & 0xFFFFFF, edge & 0xFFFFFF);
        assertEquals(0, FxMinimapDrawer.blend(new int[]{0, 0, 0, 0}));
    }

    @Test
    void thePaintedPixelsTakeTheColourOfTheTile() {
        TileMap map = uniformMap(20, TileType.ROCKY_GROUND);
        int rocky = FLAT_PALETTE.argb(MAP_TYPE, TileType.ROCKY_GROUND);

        int[] pixels = FxMinimapDrawer.samplePixels(map, FLAT_PALETTE, MAP_TYPE, new Layout(100, 60, 2, 1, 50, 5));

        // Tile (10, 10) is at the middle of the map, so the pixel under its top corner and a little lower is inside
        assertEquals(rocky, pixels[(5 + 20 + 3) * 100 + 50]);
    }

    @Test
    void thePaintedPixelsTakeTheColourOfThePaletteForTheMapType() {
        TileMap map = uniformMap(20, TileType.GRASS);

        int[] pixels = FxMinimapDrawer.samplePixels(map, FLAT_PALETTE, MapType.DESERT,
                new Layout(100, 60, 2, 1, 50, 5));

        assertEquals(FLAT_PALETTE.argb(MapType.DESERT, TileType.GRASS), pixels[(5 + 20 + 3) * 100 + 50]);
    }

    private static TileMap mapWithResources() {
        TileMap map = uniformMap(MARKER_MAP_SIZE, TileType.ROCKY_GROUND);
        map.getTile(STONE_X, STONE_Y).setWorldResource(new Resource(ResourceType.STONE, 1L));
        map.getTile(IRON_X, IRON_Y).setWorldResource(new Resource(ResourceType.IRON_ORE, 1L));
        map.getTile(CRYSTALS_X, CRYSTALS_Y).setWorldResource(new Resource(ResourceType.CRYSTALS, 1L));
        map.getTile(TREE_X, TREE_Y).setWorldResource(new Resource(ResourceType.TREE, 1L));
        return map;
    }

    /** The index of the pixel under the centre of a tile at the scale and offset the marker tests use. */
    private static int centreIndex(int x, int y) {
        int px = (int) Math.floor((x - y) * MARKER_TILE_W + MARKER_OFFSET_X);
        int py = (int) Math.floor((x + y + 1) * MARKER_TILE_H + MARKER_OFFSET_Y);
        return py * MARKER_IMAGE_W + px;
    }

    private static Layout markerLayout(double offsetX) {
        return new Layout(MARKER_IMAGE_W, MARKER_IMAGE_H, MARKER_TILE_W, MARKER_TILE_H, offsetX, MARKER_OFFSET_Y);
    }

    private static int[] paint(TileMap map, int[] pixels, double offsetX) {
        FxMinimapDrawer.paintMarkers(map, pixels, markerLayout(offsetX));
        return pixels;
    }

    private static int[] tilePixels(TileMap map) {
        return FxMinimapDrawer.samplePixels(map, FLAT_PALETTE, MAP_TYPE, markerLayout(MARKER_OFFSET_X));
    }

    @Test
    void stoneIronAndCrystalsGetOneOpaquePixelAtTheCentreOfTheirTile() {
        TileMap map = mapWithResources();

        int[] pixels = paint(map, tilePixels(map), MARKER_OFFSET_X);

        assertEquals(0xFFCFCFCF, pixels[centreIndex(STONE_X, STONE_Y)]);
        assertEquals(0xFFB5502E, pixels[centreIndex(IRON_X, IRON_Y)]);
        assertEquals(0xFF4FE0E8, pixels[centreIndex(CRYSTALS_X, CRYSTALS_Y)]);
    }

    @Test
    void treesAndEmptyTilesKeepTheColourOfTheirTile() {
        TileMap map = mapWithResources();
        int[] before = tilePixels(map);

        int[] pixels = paint(map, tilePixels(map), MARKER_OFFSET_X);

        assertEquals(before[centreIndex(TREE_X, TREE_Y)], pixels[centreIndex(TREE_X, TREE_Y)]);
        assertEquals(before[centreIndex(0, 0)], pixels[centreIndex(0, 0)]);
    }

    @Test
    void aDepletedResourceHasNoMarker() {
        TileMap map = uniformMap(MARKER_MAP_SIZE, TileType.ROCKY_GROUND);
        Resource stone = new Resource(ResourceType.STONE, 1L);
        map.getTile(STONE_X, STONE_Y).setWorldResource(stone);
        stone.takeDamage(Integer.MAX_VALUE);

        int[] pixels = paint(map, new int[MARKER_IMAGE_W * MARKER_IMAGE_H], MARKER_OFFSET_X);

        assertEquals(0, pixels[centreIndex(STONE_X, STONE_Y)]);
    }

    @Test
    void aMarkerOutsideTheBufferIsSkipped() {
        TileMap map = mapWithResources();

        int[] pixels = paint(map, new int[MARKER_IMAGE_W * MARKER_IMAGE_H], MARKER_OFFSET_X + MARKER_IMAGE_W);

        for (int pixel : pixels) {
            assertEquals(0, pixel);
        }
    }
}
