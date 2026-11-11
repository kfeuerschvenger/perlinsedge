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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertEquals("water-deep", AssetPaths.tileArt(TileType.DEEP_WATER));
    }

    @Test
    void everyLandTileHasArt() {
        for (TileType type : TileType.values()) {
            if (type.isLand()) {
                assertNotNull(AssetPaths.tileArt(type), type.name());
            }
        }
    }

    @Test
    void tilesWithoutArtKeepTheProceduralDrawing() {
        for (TileType type : new TileType[]{TileType.RIVER, TileType.COAST}) {
            assertNull(AssetPaths.tileArt(type), type.name());
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
        assertEquals("dark_forest", AssetPaths.biomeFolder(MapType.DARK_FOREST));
        assertEquals("tundra", AssetPaths.biomeFolder(MapType.TUNDRA));
    }

    @Test
    void everyResourceTypeHasKinds() {
        for (ResourceType type : ResourceType.values()) {
            assertFalse(AssetPaths.resourceKinds(type).isEmpty(), type.name());
        }
    }

    @Test
    void theShippedTilesAreBlocksOfTheDocumentedSize() throws IOException {
        for (TileType type : TileType.values()) {
            String art = AssetPaths.tileArt(type);
            if (art == null) {
                continue;
            }
            BufferedImage image = read(AssetPaths.tile(art, 1));
            assertEquals(32, image.getWidth(), art);
            assertEquals(type.isWater() ? 19 : 21, image.getHeight(), art);
        }
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
