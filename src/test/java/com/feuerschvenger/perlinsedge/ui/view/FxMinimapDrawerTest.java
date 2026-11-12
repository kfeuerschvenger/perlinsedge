package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FxMinimapDrawerTest {

    @Test
    void opaqueColoursPackIntoArgb() {
        assertEquals(0xFFFF0000, FxMinimapDrawer.toArgb(new Rgba(1, 0, 0, 1)));
        assertEquals(0xFF00FF00, FxMinimapDrawer.toArgb(new Rgba(0, 1, 0, 1)));
        assertEquals(0xFF0000FF, FxMinimapDrawer.toArgb(new Rgba(0, 0, 1, 1)));
    }

    @Test
    void channelsAreRoundedAndClamped() {
        assertEquals(0x80102030, FxMinimapDrawer.toArgb(Rgba.rgb(16, 32, 48).withOpacityFactor(128 / 255.0)));
        assertEquals(0xFF000000, FxMinimapDrawer.toArgb(new Rgba(0, 0, 0, 1)));
        assertEquals(0xFFFFFFFF, FxMinimapDrawer.toArgb(new Rgba(1, 1, 1, 1)));
    }

    @Test
    void namedTerrainColourPacksWithFullOpacity() {
        assertEquals(0xFF5A5A5A, FxMinimapDrawer.toArgb(Rgba.rgb(90, 90, 90)));
    }

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
        TileMap map = uniformMap(size, TileType.HILL);
        int width = 400;
        int height = 240;
        int[] pixels = FxMinimapDrawer.samplePixels(map, width, height, tileW, tileH, offsetX, offsetY);
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
        TileMap map = uniformMap(10, TileType.HILL);

        int[] pixels = FxMinimapDrawer.samplePixels(map, 100, 60, 2, 1, 50, 5);

        assertEquals(0, pixels[0], "the corner of the image is far from the map");
        assertEquals(0, pixels[59 * 100 + 99]);
    }

    @Test
    void thePaintedPixelsTakeTheColourOfTheTile() {
        TileMap map = uniformMap(20, TileType.HILL);
        Rgba hill = map.getTile(5, 5).getColor();

        int[] pixels = FxMinimapDrawer.samplePixels(map, 100, 60, 2, 1, 50, 5);

        // Tile (10, 10) is at the middle of the map, so the pixel under its top corner and a little lower is inside
        assertEquals(FxMinimapDrawer.toArgb(hill), pixels[(5 + 20 + 3) * 100 + 50]);
    }
}
