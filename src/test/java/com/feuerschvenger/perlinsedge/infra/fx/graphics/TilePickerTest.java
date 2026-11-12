package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.infra.fx.utils.TileDepths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TilePickerTest {

    private static final double HALF_W = 32;
    private static final double HALF_H = 16;
    private static final int SIZE = 8;

    private static TileMap mapOf(TileType type) {
        TileMap map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, type));
            }
        }
        return map;
    }

    private static void set(TileMap map, int x, int y, TileType type) {
        map.setTile(x, y, new Tile(x, y, type));
    }

    private static int[] pick(TileMap map, double screenX, double screenY, double cameraX, double cameraY, double zoom) {
        int[] out = {-1, -1};
        return TilePicker.pick(map, screenX, screenY, cameraX, cameraY, zoom, HALF_W, HALF_H, out) ? out : null;
    }

    @Test
    void everyInteriorPointOfALandTileIsThatTile() {
        TileMap map = mapOf(TileType.GRASS);
        final double zoom = 1.5;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (double u = 0.05; u < 1; u += 0.1) {
                    for (double v = 0.05; v < 1; v += 0.1) {
                        double screenX = 40 + (x + u - (y + v)) * HALF_W * zoom;
                        double screenY = 25 + (x + u + (y + v)) * HALF_H * zoom;
                        assertArrayEquals(new int[]{x, y}, pick(map, screenX, screenY, 40, 25, zoom),
                                "point " + u + "," + v + " of tile " + x + "," + y);
                    }
                }
            }
        }
    }

    @Test
    void aPointJustInsideAnEdgeBelongsToTheTileItIsInside() {
        TileMap map = mapOf(TileType.GRASS);
        // Tile 3,3 has its top vertex at x 0, y 96 and its left vertex at x -32, y 112
        assertArrayEquals(new int[]{3, 3}, pick(map, -2, 112, 0, 0, 1));
        assertArrayEquals(new int[]{3, 3}, pick(map, 0, 97, 0, 0, 1));
        // Just outside the left vertex is the tile whose right vertex it is, tile 2,4
        assertArrayEquals(new int[]{2, 4}, pick(map, -33, 112, 0, 0, 1));
    }

    @Test
    void aPointOutsideTheMapPicksNothing() {
        TileMap map = mapOf(TileType.GRASS);
        assertNull(pick(map, -500, 0, 0, 0, 1));
        assertNull(pick(map, 0, -20, 0, 0, 1));
        assertNull(pick(map, 0, 5000, 0, 0, 1));
    }

    private static void assertNull(int[] value) {
        assertEquals(null, value);
    }

    @Test
    void theWaterSheetIsPickedWhereItIsDrawnBelowTheLandTops() {
        TileMap map = mapOf(TileType.SHALLOW_WATER);
        // The sheet of tile 2,2 is drawn 10 px lower than its grid cell, so it reaches into the cell 3,3 below it,
        // which is the answer of a picker that ignores the sheet
        assertArrayEquals(new int[]{2, 2}, pick(map, 0, 97, 0, 0, 1));
    }

    @Test
    void landInFrontOfWaterWinsOverTheWaterBehindIt() {
        TileMap map = mapOf(TileType.SHALLOW_WATER);
        set(map, 3, 3, TileType.GRASS);
        // Tile 2,2 draws its sheet over this point, tile 3,3 is drawn after it and covers it
        assertArrayEquals(new int[]{3, 3}, pick(map, 0, 97, 0, 0, 1));
    }

    @Test
    void theSideFaceOfALandTileAboveLowerWaterBelongsToTheLandTile() {
        TileMap map = mapOf(TileType.SHALLOW_WATER);
        set(map, 2, 2, TileType.GRASS);
        // The point is 10 px under the bottom left edge of tile 2,2, over the water cell 2,3
        assertArrayEquals(new int[]{2, 2}, pick(map, -10, 100, 0, 0, 1));
    }

    @Test
    void theCenterOfAShoreTileIsTheLandTileWhateverTheCamera() {
        TileMap map = mapOf(TileType.GRASS);
        set(map, 4, 3, TileType.COAST);
        for (double zoom : new double[]{0.5, 1.0, 1.5, 2.0}) {
            double cameraX = 123;
            double cameraY = -45;
            double landX = cameraX + (3 - 3) * HALF_W * zoom;
            double landY = cameraY + (3 + 3) * HALF_H * zoom + HALF_H * zoom;
            assertArrayEquals(new int[]{3, 3}, pick(map, landX, landY, cameraX, cameraY, zoom), "zoom " + zoom);
        }
    }

    /** What the player sees at a point: the last tile in draw order whose drawn top diamond holds it. */
    private static int[] visibleTile(TileMap map, double px, double py, double zoom) {
        final double halfW = HALF_W * zoom;
        final double halfH = HALF_H * zoom;
        int[] found = null;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                final TileType type = map.getTile(x, y).getType();
                final double drop = type.isWater() ? TileDepths.SURFACE_PIXELS * ZoomSteps.artScale(zoom) : 0;
                final double centerX = (x - y) * halfW;
                final double centerY = (x + y) * halfH + halfH + drop;
                if (Math.abs(px - centerX) / halfW + Math.abs(py - centerY) / halfH < 1) {
                    found = new int[]{x, y};
                }
            }
        }
        return found;
    }

    @Test
    void thePickerAgreesWithWhatIsDrawnOnAShoreyMap() {
        TileMap map = mapOf(TileType.GRASS);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if ((x * 3 + y * 5) % 7 < 3) {
                    set(map, x, y, (x + y) % 2 == 0 ? TileType.SHALLOW_WATER : TileType.DEEP_WATER);
                }
            }
        }
        int checked = 0;
        for (double zoom : new double[]{1.0, 2.0}) {
            // The steps are irrational-looking on purpose, so no sample lands exactly on the edge between two tiles
            for (double px = -300 * zoom + 0.3713; px < 300 * zoom; px += 3.3171) {
                for (double py = 2.2117; py < 260 * zoom; py += 2.9113) {
                    int[] expected = visibleTile(map, px, py, zoom);
                    if (expected == null) {
                        continue;
                    }
                    assertArrayEquals(expected, pick(map, px, py, 0, 0, zoom),
                            "zoom " + zoom + " point " + px + "," + py);
                    checked++;
                }
            }
        }
        assertTrue(checked > 1000, "the check covered many points: " + checked);
        assertFalse(pick(map, 0, 2, 0, 0, 1) == null);
    }
}
