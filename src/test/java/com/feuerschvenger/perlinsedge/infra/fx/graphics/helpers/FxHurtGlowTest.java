package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.ui.view.CanvasProbe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxHurtGlowTest {

    private static final int WIDTH = 100;
    private static final int HEIGHT = 80;

    private static CanvasProbe glow(double width, double height, double opacity) {
        return CanvasProbe.render(WIDTH, HEIGHT, gc -> FxHurtGlow.draw(gc, width, height, opacity));
    }

    private static int alpha(CanvasProbe shot, int x, int y) {
        return shot.argb(x, y) >>> 24;
    }

    @Test
    void nothingIsDrawnAtZeroOpacity() {
        assertEquals(0, glow(WIDTH, HEIGHT, 0).painted());
        assertEquals(0, glow(WIDTH, HEIGHT, -1).painted());
    }

    @Test
    void theGlowIsRedAtTheCornersAndClearInTheMiddle() {
        final CanvasProbe shot = glow(WIDTH, HEIGHT, 1.0);

        assertEquals(0, alpha(shot, WIDTH / 2, HEIGHT / 2), "the centre stays clear");
        assertTrue(alpha(shot, 1, 1) > 100, "the corner is covered");
        final int corner = shot.argb(1, 1);
        assertTrue(((corner >> 16) & 0xFF) > ((corner >> 8) & 0xFF) * 3, "the corner is red");
    }

    @Test
    void theOpacityScalesTheGlow() {
        final int faint = alpha(glow(WIDTH, HEIGHT, 0.25), 1, 1);
        final int strong = alpha(glow(WIDTH, HEIGHT, 1.0), 1, 1);

        assertTrue(faint > 0);
        assertTrue(strong > faint);
    }

    @Test
    void aNewCanvasSizeRebuildsTheGradient() {
        final CanvasProbe narrow = glow(WIDTH / 2.0, HEIGHT / 2.0, 1.0);
        final CanvasProbe full = glow(WIDTH, HEIGHT, 1.0);

        assertFalse(narrow.sameAs(full));
        assertTrue(alpha(full, WIDTH - 2, HEIGHT - 2) > 100, "the far corner is lit once the size is full again");
    }
}
