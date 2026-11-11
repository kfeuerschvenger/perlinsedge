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
    void grassyLandIsDrawnAsDunesInTheDesertAndAsSnowInTheTundra() {
        for (TileType type : new TileType[]{TileType.GRASS, TileType.FOREST, TileType.SAVANNA, TileType.HILL, TileType.SWAMP}) {
            assertEquals("dune", AssetPaths.tileArt(MapType.DESERT, type), type.name());
            assertEquals("snow", AssetPaths.tileArt(MapType.TUNDRA, type), type.name());
            assertEquals(AssetPaths.tileArt(type), AssetPaths.tileArt(MapType.MEDITERRANEAN, type), type.name());
            assertEquals(AssetPaths.tileArt(type), AssetPaths.tileArt(null, type), type.name());
        }
    }

    @Test
    void biomeArtLeavesTheTypesThatAreNotGrassyAlone() {
        for (MapType map : MapType.values()) {
            for (TileType type : new TileType[]{TileType.SAND, TileType.BEACH, TileType.MOUNTAIN, TileType.SNOW,
                    TileType.ICE, TileType.DEEP_WATER, TileType.SHALLOW_WATER}) {
                assertEquals(AssetPaths.tileArt(type), AssetPaths.tileArt(map, type), map + " " + type);
            }
        }
    }

    @Test
    void everyTileArtAMapCanDrawIsShippedAsACompleteBlock() throws IOException {
        assertTrue(AssetPaths.allTileArts().containsAll(java.util.Set.of("dune", "snow", "grass", "bed", "bed-deep")));
        for (String art : AssetPaths.allTileArts()) {
            BufferedImage image = read(AssetPaths.tile(art, 1));
            assertEquals(32, image.getWidth(), art);
            assertEquals(21, image.getHeight(), art);
        }
    }

    @Test
    void theDuneIsWarmerAndDarkerThanSand() throws IOException {
        int dune = read(AssetPaths.tile("dune", 1)).getRGB(16, 8) & 0xFFFFFF;
        int sand = read(AssetPaths.tile("sand", 1)).getRGB(16, 8) & 0xFFFFFF;
        assertTrue(brightness(dune) < brightness(sand));
        assertTrue(((dune >> 16) & 0xFF) > (dune & 0xFF) + 60, "a warm tone keeps red well above blue");
    }

    @Test
    void everyTileTypeHasArt() {
        for (TileType type : TileType.values()) {
            assertNotNull(AssetPaths.tileArt(type), type.name());
        }
    }

    @Test
    void everyWaterTileSitsOnASandBedUnderOneSharedSheet() {
        for (TileType type : TileType.values()) {
            if (type.isWater()) {
                assertEquals(type == TileType.DEEP_WATER ? "bed-deep" : "bed", AssetPaths.tileArt(type), type.name());
                assertEquals(AssetPaths.WATER_SHEET, AssetPaths.waterArt(type), type.name());
            } else {
                assertNull(AssetPaths.waterArt(type), type.name());
            }
        }
    }

    @Test
    void theShippedWaterSheetIsAThirtyPercentTopDiamondOfTheWaterColour() throws IOException {
        BufferedImage image = read(AssetPaths.waterTop(AssetPaths.WATER_SHEET));
        assertEquals(32, image.getWidth());
        assertEquals(16, image.getHeight());
        int argb = image.getRGB(16, 8);
        assertEquals(255 * 0.30, argb >>> 24, 1.0);
        assertEquals(0x18a8c8, argb & 0xFFFFFF);
    }

    /** Mixes a channel the way the pixel tool fogs a bed. */
    private static int fog(int channel, int water, double amount) {
        return (int) Math.round(channel * (1 - amount) + water * amount);
    }

    @Test
    void theBedsAreSandBlocksFoggedTowardTheWaterColourByDepth() throws IOException {
        // The sand top is 0xf0d48e and the water 0x18a8c8, every pixel of the bed is mixed, the grid line included
        int[] sand = {0xf0, 0xd4, 0x8e};
        int[] water = {0x18, 0xa8, 0xc8};
        double[] amounts = {0.45, 0.75};
        String[] arts = {"bed", "bed-deep"};
        for (int i = 0; i < arts.length; i++) {
            int centre = read(AssetPaths.tile(arts[i], 1)).getRGB(16, 8) & 0xFFFFFF;
            for (int channel = 0; channel < 3; channel++) {
                int actual = (centre >> (16 - 8 * channel)) & 0xFF;
                assertEquals(fog(sand[channel], water[channel], amounts[i]), actual, arts[i] + " channel " + channel);
            }
        }
    }

    @Test
    void theBedsKeepTheGridEdgeOfLandSoItFadesWithDepthInsteadOfSittingOnTheWater() throws IOException {
        for (String art : new String[]{"bed", "bed-deep"}) {
            BufferedImage bed = read(AssetPaths.tile(art, 1));
            int centre = bed.getRGB(16, 8) & 0xFFFFFF;
            int edge = bed.getRGB(16, 0) & 0xFFFFFF;
            assertTrue(brightness(edge) < brightness(centre), art);
        }
        assertTrue(brightness(read(AssetPaths.tile("bed-deep", 1)).getRGB(16, 8) & 0xFFFFFF)
                != brightness(read(AssetPaths.tile("bed", 1)).getRGB(16, 8) & 0xFFFFFF));
    }

    private static int brightness(int rgb) {
        return ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
    }

    @Test
    void everyTileArtShipsAFaceStripThatContinuesItsFacesBelowTheBlock() throws IOException {
        for (String art : AssetPaths.allTileArts()) {
            BufferedImage block = read(AssetPaths.tile(art, 1));
            BufferedImage strip = read(AssetPaths.faceStrip(art));
            assertEquals(32, strip.getWidth(), art);
            assertEquals(block.getHeight() + TileDepths.FACE_PIXELS, strip.getHeight(), art);
            int leftFace = block.getRGB(4, 13) & 0xFFFFFF;
            int rightFace = block.getRGB(27, 13) & 0xFFFFFF;
            int stripPixels = 0;
            for (int y = 0; y < strip.getHeight(); y++) {
                for (int x = 0; x < 32; x++) {
                    int argb = strip.getRGB(x, y);
                    if ((argb >>> 24) == 0) {
                        continue;
                    }
                    stripPixels++;
                    boolean covered = y < block.getHeight() && (block.getRGB(x, y) >>> 24) != 0;
                    assertFalse(covered, art + " strip pixel over the block at " + x + "," + y);
                    assertEquals(x < 16 ? leftFace : rightFace, argb & 0xFFFFFF, art + " " + x + "," + y);
                }
            }
            // Five new rows in each of the 32 columns
            assertEquals(32 * TileDepths.FACE_PIXELS, stripPixels, art);
        }
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
    void theShippedLandTilesAndTheBedsAreAllBlocks() throws IOException {
        for (TileType type : TileType.values()) {
            String art = AssetPaths.tileArt(type);
            BufferedImage image = read(AssetPaths.tile(art, 1));
            assertEquals(32, image.getWidth(), art);
            assertEquals(21, image.getHeight(), art);
        }
    }

    @Test
    void theShippedWaterDiamondsTileWithoutOverlap() throws IOException {
        BufferedImage water = read(AssetPaths.waterTop(AssetPaths.WATER_SHEET));
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
