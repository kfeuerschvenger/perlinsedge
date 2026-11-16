package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycleSettings;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxSkyTintTest {

    private static final double DAWN = 90;
    private static final double DAY = 600;
    private static final double DUSK = 90;
    private static final double NIGHT = 420;

    private static DayCycle at(double seconds) {
        final DayCycle cycle = new DayCycle(new DayCycleSettings(DAWN, DAY, DUSK, NIGHT, 0, 17, 0.6, 2.0));
        cycle.advance(seconds);
        return cycle;
    }

    /** Lays the tint of a moment on a mid grey canvas and returns the colour of its centre as {r, g, b} in 0 to 255. */
    private static int[] centreOver(FxSkyTint tint, DayCycle sky) {
        return FxToolkitSupport.onFx(() -> {
            final Canvas canvas = new Canvas(8, 8);
            final GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(Color.rgb(128, 128, 128));
            gc.fillRect(0, 0, 8, 8);
            tint.draw(gc, sky, 8, 8);
            final int argb = canvas.snapshot(null, null).getPixelReader().getArgb(4, 4);
            return new int[]{(argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF};
        });
    }

    @Test
    void midDayLeavesTheWorldUntouched() {
        final int[] noon = centreOver(new FxSkyTint(), at(390));

        assertEquals(128, noon[0], 1);
        assertEquals(128, noon[1], 1);
        assertEquals(128, noon[2], 1);
    }

    @Test
    void nightDarkensTheWorldTowardBlueWithoutBlackingItOut() {
        final int[] night = centreOver(new FxSkyTint(), at(DAWN + DAY + DUSK + NIGHT / 2));

        assertTrue(night[2] > night[0] && night[2] > night[1], "blue wins");
        assertTrue(night[0] < 70 && night[1] < 70 && night[2] < 90, "clearly darker than the mid grey below");
        assertTrue(night[0] > 30 && night[1] > 30 && night[2] > 55, "not black, so the night stays barely playable");
    }

    @Test
    void dawnAndDuskShiftTheWorldTowardWarm() {
        final int[] dawn = centreOver(new FxSkyTint(), at(DAWN / 2));
        final int[] dusk = centreOver(new FxSkyTint(), at(DAWN + DAY + DUSK / 2));

        assertTrue(dawn[0] > dawn[2], "red over blue at dawn");
        assertTrue(dusk[0] > dusk[2], "red over blue at dusk");
    }

    @Test
    void aSecondFrameOfTheSameSkyGivesTheSameColour() {
        final FxSkyTint tint = new FxSkyTint();
        final DayCycle night = at(DAWN + DAY + DUSK + 100);

        assertEquals(centreOver(tint, night)[2], centreOver(tint, night)[2]);
    }
}
