package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.canvas.Canvas;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pixel checks of the two hammer icons: the sprite the panels show and the drawn stand-in used when there is no sprite. */
class HammerIconTest {
    private static final int SIZE = 40;

    /** The columns of the first and last opaque pixel of a row, or null for an empty row. */
    private static int[] span(PixelReader reader, int y, int width) {
        int first = -1;
        int last = -1;
        for (int x = 0; x < width; x++) {
            if (reader.getColor(x, y).getOpacity() > 0.5) {
                if (first < 0) {
                    first = x;
                }
                last = x;
            }
        }
        return first < 0 ? null : new int[]{first, last};
    }

    @Test
    void theDrawnHammerHasItsHandleCentredOnTheHeadAndShowingAboveIt() {
        final Image image = FxToolkitSupport.onFx(() -> {
            Canvas canvas = new Canvas(SIZE, SIZE);
            FxItemDrawingHelper.drawFallbackShape(canvas.getGraphicsContext2D(), 0, 0, SIZE, SIZE, ItemType.HAMMER, 1.0);
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.TRANSPARENT);
            return canvas.snapshot(parameters, null);
        });
        final PixelReader reader = image.getPixelReader();

        // Rows 4 and 5 are above the head, which starts at row 6; the handle starts at row 4
        final int[] handle = span(reader, 5, SIZE);
        final int[] head = span(reader, 10, SIZE);
        assertNotNull(handle, "the handle shows above the head");
        assertNotNull(head);
        assertNull(span(reader, 1, SIZE), "nothing above the handle");
        assertTrue(Math.abs((handle[0] + handle[1]) - (head[0] + head[1])) <= 2,
                "the handle is centred on the head: handle " + handle[0] + ".." + handle[1] + ", head " + head[0] + ".." + head[1]);
        assertTrue(handle[1] - handle[0] < head[1] - head[0], "the handle is narrower than the head");
    }

    @Test
    void theHammerSpriteHasItsHandleThroughTheHeadWithTheTipShowingBeyondIt() {
        final Image sprite;
        try (InputStream in = getClass().getResourceAsStream("/assets/items/" + ItemType.HAMMER.getSpritePath() + ".png")) {
            assertNotNull(in);
            sprite = new Image(in);
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        final PixelReader reader = sprite.getPixelReader();

        // The handle colours, which the head never uses: the tip is two wood pixels clear of the head, high on the haft
        assertEquals(0xa0643f, reader.getArgb(12, 3) & 0xffffff, "the tip of the handle beyond the head");
        assertEquals(0x7a4a3a, reader.getArgb(13, 3) & 0xffffff, "the shaded side of the tip");
        // The head is drawn over the middle of the handle, so the stone covers the haft where it passes through
        assertEquals(0x7d7a8c, reader.getArgb(10, 5) & 0xffffff, "the head sits across the haft");
    }
}
