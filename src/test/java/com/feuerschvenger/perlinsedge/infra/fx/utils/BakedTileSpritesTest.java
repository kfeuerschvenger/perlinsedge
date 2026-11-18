package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BakedTileSpritesTest {

    private static int argb(Image image, int x, int y) {
        return FxToolkitSupport.onFx(() -> image.getPixelReader().getArgb(x, y));
    }

    private static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }

    private static Image solid(int width, int height, int argb) {
        return FxToolkitSupport.onFx(() -> {
            final WritableImage image = new WritableImage(width, height);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.getPixelWriter().setArgb(x, y, argb);
                }
            }
            return image;
        });
    }

    // ---- debug diamonds ----

    @Test
    void everyDebugDiamondHasTheTileSizeAndItsOwnGrey() {
        for (int level = 0; level < GrayscaleTiles.LEVELS; level++) {
            final Image image = DebugTileSprites.get(level);

            assertEquals(GrayscaleTiles.WIDTH, (int) image.getWidth());
            assertEquals(GrayscaleTiles.HEIGHT, (int) image.getHeight());
            final int centre = argb(image, GrayscaleTiles.WIDTH / 2, GrayscaleTiles.HEIGHT / 2);
            assertEquals(GrayscaleTiles.greyOf(level), red(centre), "level " + level);
            assertEquals(red(centre), green(centre));
            assertEquals(red(centre), blue(centre));
            assertEquals(0xFF, centre >>> 24);
        }
    }

    @Test
    void theCornersOfADebugDiamondAreTransparent() {
        final Image image = DebugTileSprites.get(GrayscaleTiles.LEVELS - 1);

        assertEquals(0, argb(image, 0, 0) >>> 24);
        assertEquals(0, argb(image, GrayscaleTiles.WIDTH - 1, GrayscaleTiles.HEIGHT - 1) >>> 24);
    }

    @Test
    void theOutlineOfADebugDiamondIsDarkerThanItsFill() {
        final Image image = DebugTileSprites.get(GrayscaleTiles.LEVELS - 1);
        int outlineGrey = -1;
        for (int y = 0; y < GrayscaleTiles.HEIGHT && outlineGrey < 0; y++) {
            for (int x = 0; x < GrayscaleTiles.WIDTH; x++) {
                if (GrayscaleTiles.insideDiamond(x, y) && GrayscaleTiles.isOutline(x, y)) {
                    outlineGrey = red(argb(image, x, y));
                    break;
                }
            }
        }

        assertTrue(outlineGrey >= 0 && outlineGrey < GrayscaleTiles.greyOf(GrayscaleTiles.LEVELS - 1));
    }

    @Test
    void aBrighterLevelIsALighterDiamond() {
        final int dark = red(argb(DebugTileSprites.get(3), 16, 8));
        final int light = red(argb(DebugTileSprites.get(40), 16, 8));

        assertTrue(light > dark);
    }

    // ---- flat tiles ----

    @Test
    void everyTileTypeHasAFlatBlockOfTheBlockSize() {
        for (TileType type : TileType.values()) {
            final Image image = FlatTileSprites.get(type);

            assertNotNull(image, type + " has no flat sprite");
            assertEquals(FlatTiles.WIDTH, (int) image.getWidth());
            assertEquals(FlatTiles.HEIGHT, (int) image.getHeight());
            assertTrue(argb(image, FlatTiles.WIDTH / 2, FlatTiles.TOP_HEIGHT / 2) >>> 24 > 0, type + " top is empty");
            assertEquals(0, argb(image, 0, 0) >>> 24, type + " has a painted corner");
        }
    }

    @Test
    void theFlatBlocksFollowTheShapeRuleOfFlatTiles() {
        for (TileType type : TileType.values()) {
            final Image image = FlatTileSprites.get(type);
            final int base = FlatTiles.argbOf(type.getColor());
            for (int y = 0; y < FlatTiles.HEIGHT; y++) {
                for (int x = 0; x < FlatTiles.WIDTH; x++) {
                    assertEquals(FlatTiles.pixel(x, y, base, type.isWater()), argb(image, x, y),
                            type + " at " + x + "," + y);
                }
            }
        }
    }

    @Test
    void waterAndLandAreNotTheSameBlock() {
        assertNotEquals(argb(FlatTileSprites.get(TileType.GRASS), 16, 8), argb(FlatTileSprites.get(TileType.DEEP_WATER), 16, 8));
    }

    @Test
    void aFlatBlockIsBakedOnce() {
        assertSame(FlatTileSprites.get(TileType.GRASS), FlatTileSprites.get(TileType.GRASS));
    }

    // ---- hurt tints ----

    @Test
    void theRedPhaseMixesEveryVisiblePixelTowardRedAndKeepsTransparency() {
        final Image source = solid(2, 1, 0xFF404040);

        final Image tinted = TintedSprites.of(source, HurtReaction.RED);

        assertNotSame(source, tinted);
        final int pixel = argb(tinted, 0, 0);
        assertTrue(red(pixel) > 0x40 && green(pixel) < 0x80, "reddened");
        assertEquals(0xFF, pixel >>> 24);
        assertEquals(HurtReaction.tint(0xFF404040, HurtReaction.RED), pixel);

        final Image clear = TintedSprites.of(solid(1, 1, 0), HurtReaction.RED);
        assertEquals(0, argb(clear, 0, 0));
    }

    @Test
    void theWhitePhaseLightensAndEachPhaseKeepsItsOwnBake() {
        final Image source = solid(1, 1, 0xFF204060);

        final Image white = TintedSprites.of(source, HurtReaction.WHITE);
        final Image red = TintedSprites.of(source, HurtReaction.RED);

        assertTrue(blue(argb(white, 0, 0)) > 0x60 && red(argb(white, 0, 0)) > 0x20);
        assertNotSame(white, red);
        assertSame(white, TintedSprites.of(source, HurtReaction.WHITE), "cached per source");
        assertSame(red, TintedSprites.of(source, HurtReaction.RED), "cached per source");
    }

    @Test
    void noFlashLeavesTheSpriteAsItIs() {
        final Image source = solid(1, 1, 0xFF204060);

        assertSame(source, TintedSprites.of(source, HurtReaction.NO_FLASH));
    }
}
