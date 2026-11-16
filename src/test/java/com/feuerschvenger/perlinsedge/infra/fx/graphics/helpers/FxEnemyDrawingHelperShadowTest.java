package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowList;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ZoomSteps;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FxEnemyDrawingHelperShadowTest {

    private static final double ZOOM = 1.0;
    private static final int FRAME_WIDTH = 16;
    private static final int FRAME_HEIGHT = 12;

    /** An opaque slime frame, the same one for every frame number. */
    private static Image frame() {
        return FxToolkitSupport.onFx(() -> {
            final WritableImage image = new WritableImage(FRAME_WIDTH, FRAME_HEIGHT);
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int x = 0; x < FRAME_WIDTH; x++) {
                    image.getPixelWriter().setColor(x, y, Color.GREEN);
                }
            }
            return image;
        });
    }

    @Test
    void theShadowOfASquashedSlimeIsAnchoredOnTheBaseItsBodyIsDrawnOn() {
        final Image frame = frame();
        final Slime slime = new Slime(5, 5);
        final double scale = ZoomSteps.artScale(ZOOM);
        // At the top of its breath the slime is drawn tall and thin, so the rounding differs from the unsquashed one
        final double gameTime = 2.4 * (0.25 - slime.getBreathPhase());
        final double widthFactor = SlimeSquash.width(slime.getWindupProgress(),
                SlimeSquash.breath(gameTime, slime.getBreathPhase()));
        final double heightFactor = SlimeSquash.height(slime.getWindupProgress(),
                SlimeSquash.breath(gameTime, slime.getBreathPhase()));
        final double drawnWidth = Math.rint(FRAME_WIDTH * scale * widthFactor);
        final double drawnHeight = Math.rint(FRAME_HEIGHT * scale * heightFactor);
        assertNotEquals(FRAME_WIDTH * scale, drawnWidth, "the body is squashed in this test");

        final double tileTopX = 100.37;
        final double tileTopY = 61.81;
        final ShadowList shadows = new ShadowList();
        FxEnemyDrawingHelper.addSlimeShadow(shadows, tileTopX, tileTopY, ZOOM, slime, gameTime, number -> frame);

        assertEquals(1, shadows.size());
        assertNotNull(shadows.silhouette(0));
        // The body is drawn with its left edge at rint(x - width / 2) and its top edge at rint(baseY - height)
        final double halfTileHeight = AppConfig.getInstance().graphics().getTileHalfHeight() * ZOOM;
        final double baseDrawY = tileTopY + halfTileHeight * 1.5 + slime.getRenderOffsetY();
        final double drawnBaseX = Math.rint(tileTopX - drawnWidth / 2) + drawnWidth / 2;
        final double drawnBaseY = Math.rint(baseDrawY - drawnHeight) + drawnHeight;
        assertEquals(drawnBaseX, ShadowAnchor.baseX(shadows.centreX(0), shadows.drawnWidth(0), scale), 1e-9);
        assertEquals(drawnBaseY, ShadowAnchor.baseY(shadows.centreY(0), shadows.drawnHeight(0), scale), 1e-9);
        assertEquals(drawnWidth / scale, shadows.drawnWidth(0), 1e-9);
        assertEquals(drawnHeight / scale, shadows.drawnHeight(0), 1e-9);
        assertEquals(drawnWidth / scale, shadows.widthArt(0), 1e-9, "a standing slime casts the width it is drawn at");
        assertEquals(FRAME_HEIGHT, shadows.groundRow(0), "the mask still reads the rows of the silhouette");
    }

    @Test
    void aSlimeAtRestWithoutBreathingKeepsTheWholeFrameSize() {
        final Image frame = frame();
        final Slime slime = new Slime(5, 5);
        // The breath crosses zero at the start of a breath, so the body is drawn at its frame size
        final double gameTime = 2.4 * (1.0 - slime.getBreathPhase());
        final ShadowList shadows = new ShadowList();

        FxEnemyDrawingHelper.addSlimeShadow(shadows, 50, 40, ZOOM, slime, gameTime, number -> frame);

        assertEquals(FRAME_WIDTH, shadows.drawnWidth(0), 1e-9);
        assertEquals(FRAME_HEIGHT, shadows.drawnHeight(0), 1e-9);
        assertEquals(FRAME_WIDTH, shadows.widthArt(0), 1e-9);
    }

    @Test
    void aSlimeWithoutArtGetsAnEllipse() {
        final ShadowList shadows = new ShadowList();

        FxEnemyDrawingHelper.addSlimeShadow(shadows, 50, 40, ZOOM, new Slime(5, 5), 0.0, number -> null);

        assertEquals(1, shadows.size());
        assertNull(shadows.silhouette(0));
    }
}
