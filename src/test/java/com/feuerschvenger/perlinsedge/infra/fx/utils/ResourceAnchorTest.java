package com.feuerschvenger.perlinsedge.infra.fx.utils;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceAnchorTest {

    private static final List<String> FOLDERS = List.of("default", "desert", "tundra", "swamp", "dense_forest",
            "mediterranean");
    private static final List<String> LOW = List.of("bush", "rock", "iron");
    private static final List<String> TALL = List.of("tree", "palm", "cactus");

    @Test
    void rocksOreAndBushesAreLowAndTreesAreNot() {
        for (String kind : new String[]{"rock", "iron", "bush", "shrub", "fern", "reed"}) {
            assertTrue(ResourceAnchor.isLow(kind), kind);
        }
        for (String kind : new String[]{"tree", "pine", "palm", "cactus", "willow", "crystal"}) {
            assertFalse(ResourceAnchor.isLow(kind), kind);
        }
    }

    @Test
    void aLowSpriteIsMovedDownByItsPaddingPlusTheContactDepth() {
        assertEquals(3 + ResourceAnchor.CONTACT_DEPTH_ART_PIXELS, ResourceAnchor.dropArtPixels("rock", 3));
        assertEquals(ResourceAnchor.CONTACT_DEPTH_ART_PIXELS, ResourceAnchor.dropArtPixels("bush", 0));
        assertEquals(ResourceAnchor.CONTACT_DEPTH_ART_PIXELS, ResourceAnchor.contactArtPixels("iron"));
    }

    @Test
    void aTallSpriteIsLeftWhereItIs() {
        assertEquals(0, ResourceAnchor.dropArtPixels("tree", 0));
        assertEquals(0, ResourceAnchor.dropArtPixels("palm", 4));
        assertEquals(0, ResourceAnchor.contactArtPixels("cactus"));
    }

    @Test
    void theContactDepthStaysInsideTheTileDiamond() {
        assertTrue(ResourceAnchor.CONTACT_DEPTH_ART_PIXELS >= 2 && ResourceAnchor.CONTACT_DEPTH_ART_PIXELS <= 3);
    }

    @Test
    void theBottomPaddingCountsTheEmptyRowsUnderThePaint() {
        assertEquals(2, ResourceAnchor.bottomPadding(6, row -> row >= 4 || row == 0));
        assertEquals(0, ResourceAnchor.bottomPadding(6, row -> row < 3));
        assertEquals(6, ResourceAnchor.bottomPadding(6, row -> true));
        assertEquals(0, ResourceAnchor.bottomPadding(0, row -> true));
    }

    @Test
    void everyShippedLowSpriteEndsAboveItsCanvasBottomSoItNeedsTheDrop() throws IOException {
        int checked = 0;
        for (String folder : FOLDERS) {
            for (String kind : LOW) {
                for (int variant = 1; variant < 10; variant++) {
                    BufferedImage image = read(AssetPaths.resource(folder, kind, variant));
                    if (image == null) {
                        break;
                    }
                    int padding = paddingOf(image);
                    assertTrue(padding > 0, folder + "/" + kind + "-" + variant);
                    // The canvas bottom lands on the tile centre plus the drop, and the paint ends padding rows above it
                    int paintBelowCentre = ResourceAnchor.dropArtPixels(kind, padding) - padding;
                    assertEquals(ResourceAnchor.CONTACT_DEPTH_ART_PIXELS, paintBelowCentre, folder + "/" + kind + "-" + variant);
                    checked++;
                }
            }
        }
        assertTrue(checked >= 15, "checked " + checked);
    }

    @Test
    void everyShippedTallSpriteTouchesItsCanvasBottomSoItDoesNotFloat() throws IOException {
        int checked = 0;
        for (String folder : FOLDERS) {
            for (String kind : TALL) {
                for (int variant = 1; variant < 10; variant++) {
                    BufferedImage image = read(AssetPaths.resource(folder, kind, variant));
                    if (image == null) {
                        break;
                    }
                    assertEquals(0, paddingOf(image), folder + "/" + kind + "-" + variant);
                    checked++;
                }
            }
        }
        assertTrue(checked >= 20, "checked " + checked);
    }

    @Test
    void theAnchorAndTheSilhouetteAgreeOnTheLowestPaintedRowOfEveryShippedLowSprite() throws IOException {
        int checked = 0;
        for (String folder : FOLDERS) {
            for (String kind : LOW) {
                for (int variant = 1; variant < 10; variant++) {
                    BufferedImage image = read(AssetPaths.resource(folder, kind, variant));
                    if (image == null) {
                        break;
                    }
                    final int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0,
                            image.getWidth());
                    final Silhouette silhouette = new Silhouette(image.getWidth(), image.getHeight(), pixels);
                    assertEquals(image.getHeight() - silhouette.paintedBottom(), paddingOf(image),
                            folder + "/" + kind + "-" + variant);
                    checked++;
                }
            }
        }
        assertTrue(checked >= 15, "checked " + checked);
    }

    @Test
    void aFaintRowDoesNotCountAsPaintedForTheAnchorOrTheSilhouette() {
        final int faint = (Silhouette.PAINTED_ALPHA - 1) << 24;
        final int solid = Silhouette.PAINTED_ALPHA << 24;
        final int[] pixels = {solid, solid, faint, faint};
        final Silhouette silhouette = new Silhouette(2, 2, pixels);

        assertEquals(1, silhouette.paintedBottom());
        assertEquals(1, ResourceAnchor.bottomPadding(2, row -> (pixels[row * 2] >>> 24) < Silhouette.PAINTED_ALPHA
                && (pixels[row * 2 + 1] >>> 24) < Silhouette.PAINTED_ALPHA));
    }

    private static int paddingOf(BufferedImage image) {
        return ResourceAnchor.bottomPadding(image.getHeight(), row -> {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, row) >>> 24) >= Silhouette.PAINTED_ALPHA) {
                    return false;
                }
            }
            return true;
        });
    }

    private BufferedImage read(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            BufferedImage image = ImageIO.read(in);
            assertNotNull(image, path);
            return image;
        }
    }
}
