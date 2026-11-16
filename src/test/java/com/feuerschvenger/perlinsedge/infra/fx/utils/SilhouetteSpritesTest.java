package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class SilhouetteSpritesTest {

    private static WritableImage sprite() {
        final WritableImage image = new WritableImage(4, 3);
        final PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                writer.setArgb(x, y, 0);
            }
        }
        writer.setArgb(1, 0, 0xFF3A7D2C);
        writer.setArgb(1, 1, 0xFFD2B48C);
        writer.setArgb(2, 1, 0xFFD2B48C);
        return image;
    }

    @Test
    void paintedPixelsBecomeBlackOpaqueAndTheRestStaysTransparent() {
        final Silhouette silhouette = FxToolkitSupport.onFx(() -> SilhouetteSprites.of(sprite()));

        assertEquals(4, silhouette.width());
        assertEquals(3, silhouette.height());
        assertEquals(0xFF000000, silhouette.argbAt(1, 0));
        assertEquals(0xFF000000, silhouette.argbAt(1, 1));
        assertEquals(0xFF000000, silhouette.argbAt(2, 1));
        assertEquals(0, silhouette.argbAt(0, 0));
        assertEquals(0, silhouette.argbAt(3, 2));
        assertEquals(2, silhouette.paintedBottom());
    }

    @Test
    void theSameImageGivesTheSameSilhouetteAndAnotherImageGivesAnother() {
        final WritableImage first = sprite();
        final WritableImage second = sprite();

        final Silhouette[] seen = FxToolkitSupport.onFx(() -> new Silhouette[]{
                SilhouetteSprites.of(first), SilhouetteSprites.of(first), SilhouetteSprites.of(second)});

        assertSame(seen[0], seen[1], "baked once per image identity");
        assertNotSame(seen[0], seen[2], "two images with equal pixels are still two sprites");
    }
}
