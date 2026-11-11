package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetPathsTest {

    @Test
    void tilesWithArtMapToTheirName() {
        assertEquals("grass", AssetPaths.tileArt(TileType.GRASS));
        assertEquals("forest", AssetPaths.tileArt(TileType.FOREST));
        assertEquals("savanna", AssetPaths.tileArt(TileType.SAVANNA));
        assertEquals("hill", AssetPaths.tileArt(TileType.HILL));
        assertEquals("sand", AssetPaths.tileArt(TileType.SAND));
        assertEquals("sand", AssetPaths.tileArt(TileType.BEACH));
        assertEquals("mountain", AssetPaths.tileArt(TileType.MOUNTAIN));
        assertEquals("snow", AssetPaths.tileArt(TileType.SNOW));
        assertEquals("ice", AssetPaths.tileArt(TileType.ICE));
        assertEquals("swamp", AssetPaths.tileArt(TileType.SWAMP));
    }

    @Test
    void everyTileTypeHasArt() {
        for (TileType type : TileType.values()) {
            assertNotNull(AssetPaths.tileArt(type), type.name());
        }
    }

    @Test
    void everyWaterTileSitsOnABedWhoseWaterSitsAboveIt() {
        assertTrue(AssetPaths.WATER_SURFACE_SINK_PIXELS < AssetPaths.BED_SINK_PIXELS);
        assertTrue(AssetPaths.WATER_SURFACE_SINK_PIXELS > 0);
        for (TileType type : TileType.values()) {
            if (type.isWater()) {
                assertEquals(type == TileType.DEEP_WATER ? "bed-deep" : "bed", AssetPaths.tileArt(type), type.name());
                assertNotNull(AssetPaths.waterArt(type), type.name());
            } else {
                assertNull(AssetPaths.waterArt(type), type.name());
            }
        }
    }

    @Test
    void theShippedWaterLayersAreTranslucentTopDiamonds() throws IOException {
        for (TileType type : TileType.values()) {
            String art = AssetPaths.waterArt(type);
            if (art == null) {
                continue;
            }
            BufferedImage image = read(AssetPaths.waterTop(art));
            assertEquals(32, image.getWidth(), art);
            assertEquals(16, image.getHeight(), art);
            int alpha = image.getRGB(16, 8) >>> 24;
            assertTrue(alpha > 80 && alpha < 220, art + " alpha " + alpha);
        }
        assertTrue((read(AssetPaths.waterTop("deep")).getRGB(16, 8) >>> 24) > (read(AssetPaths.waterTop("shallow")).getRGB(16, 8) >>> 24));
    }

    @Test
    void theShimmerLinesAreThinAndShipped() throws IOException {
        for (int line = 0; line < WaterShimmer.MAX_LINES; line++) {
            assertEquals(1, read(AssetPaths.shimmer(line)).getHeight(), "line " + line);
        }
    }

    @Test
    void pathsFollowTheToolLayout() {
        assertEquals("/assets/tiles/grass-1.png", AssetPaths.tile("grass", 1));
        assertEquals("/assets/items/wood_log.png", AssetPaths.item(ItemType.WOOD_LOG));
        assertEquals("/assets/resources/default/tree-1.png", AssetPaths.resource("default", "tree", 1));
    }

    @Test
    void everyMapTypeHasABiomeFolder() {
        assertEquals("default", AssetPaths.biomeFolder(MapType.MEDITERRANEAN));
        assertEquals("default", AssetPaths.biomeFolder(MapType.ISLANDS));
        assertEquals("dense_forest", AssetPaths.biomeFolder(MapType.DENSE_FOREST));
        assertEquals("tundra", AssetPaths.biomeFolder(MapType.TUNDRA));
    }

    @Test
    void everyDrawnResourceKindShipsContiguousVariantsInTheDefaultFolder() {
        int[] expected = {8, 3, 4, 3};
        String[] kinds = {"tree", "bush", "rock", "iron"};
        for (int i = 0; i < kinds.length; i++) {
            for (int variant = 1; variant <= expected[i]; variant++) {
                assertNotNull(getClass().getResource(AssetPaths.resource("default", kinds[i], variant)), kinds[i] + "-" + variant);
            }
            assertNull(getClass().getResource(AssetPaths.resource("default", kinds[i], expected[i] + 1)), kinds[i] + " has a gap or an extra");
        }
    }

    @Test
    void ironOreIsDrawnFromItsOwnKind() {
        assertEquals(List.of("iron"), AssetPaths.resourceKinds(ResourceType.IRON_ORE));
    }

    @Test
    void everyResourceTypeHasKinds() {
        for (ResourceType type : ResourceType.values()) {
            assertFalse(AssetPaths.resourceKinds(type).isEmpty(), type.name());
        }
    }

    @Test
    void theShippedLandTilesAreBlocksAndTheBedsAreFlatDiamonds() throws IOException {
        for (TileType type : TileType.values()) {
            String art = AssetPaths.tileArt(type);
            BufferedImage image = read(AssetPaths.tile(art, 1));
            assertEquals(32, image.getWidth(), art);
            assertEquals(type.isWater() ? 16 : 21, image.getHeight(), art);
        }
    }

    @Test
    void theShippedWaterDiamondsTileWithoutOverlap() throws IOException {
        BufferedImage water = read(AssetPaths.waterTop("shallow"));
        BufferedImage bed = read(AssetPaths.tile("bed", 1));
        int overlaps = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 32; x++) {
                boolean here = (water.getRGB(x, y) >>> 24) > 0;
                // The neighbour one tile to the right and one half tile down sits at an offset of (16, 8)
                int nx = x - 16;
                int ny = y - 8;
                boolean there = nx >= 0 && ny >= 0 && ny < 16 && (water.getRGB(nx, ny) >>> 24) > 0;
                if (here && there) {
                    overlaps++;
                }
            }
        }
        assertEquals(0, overlaps);
        assertEquals(255, bed.getRGB(16, 8) >>> 24);
    }

    @Test
    void thePlayerSheetMatchesTheConfiguredGrid() throws IOException {
        AppConfig.PlayerConfig.SpriteSheetConfig config = AppConfig.getInstance().player().spriteSheet;
        BufferedImage sheet = read(config.path);
        assertEquals(config.startX + config.columns * config.columnOffset, sheet.getWidth());
        assertEquals(config.rowYCoordinates[config.rows - 1] + config.spriteHeight, sheet.getHeight());
    }

    @Test
    void theSlimeStripHoldsTheExpectedFrames() throws IOException {
        BufferedImage strip = read(AssetPaths.SLIME_STRIP);
        assertEquals(0, strip.getWidth() % SpriteLayout.SLIME_FRAMES);
        assertEquals(16, strip.getHeight());
    }

    @Test
    void theHeartIconsAreAllShipped() throws IOException {
        for (String path : new String[]{AssetPaths.HEART_FULL, AssetPaths.HEART_HALF, AssetPaths.HEART_EMPTY}) {
            assertEquals(16, read(path).getWidth(), path);
        }
    }

    private BufferedImage read(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, path);
            return ImageIO.read(in);
        }
    }
}
