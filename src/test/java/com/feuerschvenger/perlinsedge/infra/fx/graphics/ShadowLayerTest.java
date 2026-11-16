package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.domain.world.time.SunScreenMapping;
import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowLayerTest {

    private static final int SIZE = 120;
    private static final double AX = 60;
    private static final double AY = 40;
    private static final double NOON = Math.PI / 2;
    private static final double MORNING = Math.PI;
    private static final double EVENING = 0.0;

    /** A solid block, so the projected shadow is easy to measure. */
    private static Silhouette block(int width, int height) {
        final int[] pixels = new int[width * height];
        java.util.Arrays.fill(pixels, 0xFFFFFFFF);
        return new Silhouette(width, height, pixels);
    }

    private static ShadowLayer layer(double angle, double length) {
        final ShadowLayer layer = new ShadowLayer();
        layer.prepare(SIZE, SIZE, angle, length);
        return layer;
    }

    private static int count(ShadowLayer layer) {
        int cells = 0;
        for (int y = 0; y < layer.height(); y++) {
            for (int x = 0; x < layer.width(); x++) {
                if (layer.alphaAt(x, y) > 0) {
                    cells++;
                }
            }
        }
        return cells;
    }

    /** Lowest, highest, leftmost and rightmost shadow cell as {minX, minY, maxX, maxY}. */
    private static int[] extent(ShadowLayer layer) {
        final int[] box = {Integer.MAX_VALUE, Integer.MAX_VALUE, -1, -1};
        for (int y = 0; y < layer.height(); y++) {
            for (int x = 0; x < layer.width(); x++) {
                if (layer.alphaAt(x, y) > 0) {
                    box[0] = Math.min(box[0], x);
                    box[1] = Math.min(box[1], y);
                    box[2] = Math.max(box[2], x);
                    box[3] = Math.max(box[3], y);
                }
            }
        }
        return box;
    }

    @Test
    void aNoonShadowFallsStraightDownFromTheAnchorAndIsSymmetric() {
        final ShadowLayer layer = layer(NOON, 1.0);
        layer.castSprite(block(10, 20), AX, AY, 20, 1.0, 255);

        final int[] box = extent(layer);
        assertEquals((int) AY, box[1], "the shadow starts at the anchor row");
        assertEquals(AX - 5, box[0], 1.0);
        assertEquals(AX + 5, box[2] + 1, 1.0);
        for (int y = box[1]; y <= box[3]; y++) {
            for (int x = 0; x < SIZE; x++) {
                assertEquals(layer.alphaAt(x, y), layer.alphaAt(SIZE - 1 - x + (int) (2 * AX - SIZE), y),
                        "mirrored about the anchor column at " + x + "," + y);
            }
        }
    }

    @Test
    void theAnchorStaysFixedWhateverTheSunDoes() {
        for (double angle : new double[]{MORNING, MORNING * 0.8, NOON, EVENING + 0.3, EVENING}) {
            for (double length : new double[]{0.6, 1.0, 2.0}) {
                final ShadowLayer layer = layer(angle, length);
                layer.castSprite(block(10, 20), AX, AY, 20, 1.0, 255);

                boolean touchesAnchor = false;
                for (int y = (int) AY - 2; y <= (int) AY + 1 && !touchesAnchor; y++) {
                    for (int x = (int) AX - 2; x <= (int) AX + 1; x++) {
                        touchesAnchor |= layer.alphaAt(x, y) > 0;
                    }
                }
                assertTrue(touchesAnchor, "the base of the object is on the ground at angle " + angle + " length " + length);
            }
        }
    }

    @Test
    void theShadowLengthFollowsTheFactor() {
        final int[] shortBox = castAtNoon(1.0);
        final int[] longBox = castAtNoon(2.0);
        final int[] longest = castAtNoon(3.0);

        // At noon the ground is squashed to half, so a 20 high object casts length * 20 / 2 rows
        assertEquals(10, shortBox[3] - shortBox[1] + 1, 1);
        assertEquals(20, longBox[3] - longBox[1] + 1, 1);
        assertEquals(30, longest[3] - longest[1] + 1, 1);
    }

    private static int[] castAtNoon(double length) {
        final ShadowLayer layer = layer(NOON, length);
        layer.castSprite(block(10, 20), AX, AY, 20, 1.0, 255);
        return extent(layer);
    }

    @Test
    void aMorningShadowLeansLeftAndAnEveningShadowLeansRight() {
        final ShadowLayer morning = layer(MORNING, 1.0);
        morning.castSprite(block(6, 20), AX, AY, 20, 1.0, 255);
        final int[] morningBox = extent(morning);
        assertTrue(morningBox[0] < AX - 15, "reaches about the height of the object to the left");
        assertTrue(morningBox[2] <= AX + 3, "and not to the right");

        final ShadowLayer evening = layer(EVENING, 1.0);
        evening.castSprite(block(6, 20), AX, AY, 20, 1.0, 255);
        final int[] eveningBox = extent(evening);
        assertTrue(eveningBox[2] > AX + 15);
        assertTrue(eveningBox[0] >= AX - 4);
    }

    @Test
    void aLowSunStretchesTheShadowFurtherThanAHighSun() {
        final ShadowLayer high = layer(SunScreenMapping.shadowAngle(0.2), 0.8);
        high.castSprite(block(6, 20), AX, AY, 20, 1.0, 255);
        final ShadowLayer low = layer(SunScreenMapping.shadowAngle(0.2), 1.9);
        low.castSprite(block(6, 20), AX, AY, 20, 1.0, 255);

        assertTrue(count(low) > count(high));
    }

    @Test
    void theShadowHasTheFormOfTheObjectNotJustItsBox() {
        // A tall trunk with a wide canopy on top: the canopy must show as the far end of the shadow
        final int[] pixels = new int[9 * 12];
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 9; x++) {
                final boolean canopy = y < 4;
                final boolean trunk = x == 4 && y >= 4;
                pixels[y * 9 + x] = canopy || trunk ? 0xFFFFFFFF : 0;
            }
        }
        final ShadowLayer layer = layer(NOON, 2.0);
        layer.castSprite(new Silhouette(9, 12, pixels), AX, AY, 12, 1.0, 255);

        final int rowNearFoot = (int) AY + 1;
        final int rowAtCanopy = (int) AY + 10;
        int footCells = 0;
        int canopyCells = 0;
        for (int x = 0; x < SIZE; x++) {
            footCells += layer.alphaAt(x, rowNearFoot) > 0 ? 1 : 0;
            canopyCells += layer.alphaAt(x, rowAtCanopy) > 0 ? 1 : 0;
        }
        assertTrue(footCells <= 2, "only the trunk reaches the foot: " + footCells);
        assertTrue(canopyCells >= 7, "the canopy lands at the far end: " + canopyCells);
    }

    @Test
    void thePartOfTheSpriteBelowTheGroundRowCastsNothing() {
        final ShadowLayer layer = layer(NOON, 1.0);
        // Ten rows of block, the lowest five are padding under the ground row
        layer.castSprite(block(10, 10), AX, AY, 5, 1.0, 255);

        final int[] box = extent(layer);
        assertEquals(3, box[3] - box[1] + 1, 1, "only five rows of height cast: 5 * 1.0 / 2");
    }

    @Test
    void aNarrowerWidthFactorMakesANarrowerShadow() {
        final ShadowLayer wide = layer(NOON, 1.0);
        wide.castSprite(block(20, 10), AX, AY, 10, 1.0, 255);
        final ShadowLayer narrow = layer(NOON, 1.0);
        narrow.castSprite(block(20, 10), AX, AY, 10, 0.5, 255);

        assertEquals(10, extent(narrow)[2] - extent(narrow)[0] + 1, 1);
        assertEquals(20, extent(wide)[2] - extent(wide)[0] + 1, 1);
    }

    @Test
    void twoShadowsOverlappingAreAsDarkAsOne() {
        final ShadowLayer one = layer(NOON, 1.0);
        one.castSprite(block(10, 20), AX, AY, 20, 1.0, 255);

        final ShadowLayer two = layer(NOON, 1.0);
        two.castSprite(block(10, 20), AX, AY, 20, 1.0, 255);
        two.castSprite(block(10, 20), AX + 3, AY + 2, 20, 1.0, 255);
        two.castEllipse(AX, AY, 14, 255);

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                assertTrue(two.alphaAt(x, y) <= 255);
                if (one.alphaAt(x, y) > 0) {
                    assertEquals(255, two.alphaAt(x, y), "an overlap must not change a covered cell at " + x + "," + y);
                }
            }
        }
        assertTrue(count(two) >= count(one));
    }

    @Test
    void theStrongestShadowWinsAnOverlapInsteadOfTheSum() {
        final ShadowLayer layer = layer(NOON, 1.0);
        layer.castSprite(block(10, 20), AX, AY, 20, 1.0, 100);
        layer.castSprite(block(10, 20), AX, AY, 20, 1.0, 180);
        layer.castSprite(block(10, 20), AX, AY, 20, 1.0, 60);

        assertEquals(180, layer.alphaAt((int) AX, (int) AY + 2));
    }

    @Test
    void anEllipseLeansTowardTheShadowAndStartsAtTheBase() {
        final ShadowLayer noon = layer(NOON, 1.0);
        noon.castEllipse(AX, AY, 20, 255);
        final int[] noonBox = extent(noon);
        assertEquals(AX - 10, noonBox[0], 1.0);
        assertEquals(AX + 10, noonBox[2] + 1, 1.0);
        assertEquals(AY - 5, noonBox[1], 1.0, "a ground circle is half as tall as wide on the screen");

        final ShadowLayer morning = layer(MORNING, 2.0);
        morning.castEllipse(AX, AY, 20, 255);
        final int[] morningBox = extent(morning);
        assertTrue(morningBox[0] < noonBox[0] - 8, "stretched toward the left");
        assertTrue(morningBox[2] <= AX + 11, "and still starting at the base on the other side");
    }

    @Test
    void theEllipseStretchGrowsWithTheLengthFactor() {
        final ShadowLayer shortOne = layer(MORNING, 0.6);
        shortOne.castEllipse(AX, AY, 20, 255);
        final ShadowLayer longOne = layer(MORNING, 2.0);
        longOne.castEllipse(AX, AY, 20, 255);

        assertTrue(count(longOne) > count(shortOne));
    }

    @Test
    void preparingClearsTheCellsTheLastFrameStamped() {
        final ShadowLayer layer = layer(NOON, 1.0);
        layer.castSprite(block(10, 20), AX, AY, 20, 1.0, 255);
        assertTrue(count(layer) > 0);

        layer.prepare(SIZE, SIZE, NOON, 1.0);

        assertEquals(0, count(layer));
        assertFalse(layer.hasShadows());
    }

    @Test
    void theChangedBoxCoversWhatWasStampedAndWhatWasClearedUntilItIsHandedOn() {
        final ShadowLayer layer = layer(NOON, 1.0);
        layer.castSprite(block(10, 20), 20, 20, 20, 1.0, 255);
        final int[] first = layer.changedBox().clone();
        assertTrue(first[2] > first[0] && first[3] > first[1]);
        layer.handedOn();

        layer.prepare(SIZE, SIZE, NOON, 1.0);
        layer.castSprite(block(10, 20), 90, 60, 20, 1.0, 255);
        final int[] second = layer.changedBox();

        assertTrue(second[0] <= first[0] && second[2] >= 90, "takes in the old place to clear and the new one to fill");
        assertTrue(second[1] <= first[1] && second[3] >= layer.stampedBottom());
    }

    @Test
    void anEmptyFrameStillReportsTheClearedAreaOnceAndThenNothing() {
        final ShadowLayer layer = layer(NOON, 1.0);
        layer.castSprite(block(10, 20), AX, AY, 20, 1.0, 255);
        layer.handedOn();

        layer.prepare(SIZE, SIZE, NOON, 1.0);
        final int[] cleared = layer.changedBox();
        assertTrue(cleared[2] > cleared[0], "the old shadow still has to be wiped from the mirror");
        layer.handedOn();

        layer.prepare(SIZE, SIZE, NOON, 1.0);
        final int[] none = layer.changedBox();
        assertFalse(none[2] > none[0]);
    }

    @Test
    void shadowsPartlyOffTheLayerAreClippedWithoutError() {
        final ShadowLayer layer = layer(MORNING, 2.0);
        layer.castSprite(block(10, 20), 2, 2, 20, 1.0, 255);
        layer.castSprite(block(10, 20), SIZE + 5, SIZE + 5, 20, 1.0, 255);
        layer.castEllipse(-10, -10, 30, 255);
        layer.castEllipse(SIZE + 20, 5, 30, 255);

        assertTrue(layer.stampedLeft() >= 0 && layer.stampedRight() <= SIZE);
        assertTrue(layer.stampedTop() >= 0 && layer.stampedBottom() <= SIZE);
    }

    @Test
    void aSizeChangeStartsAFreshEmptyLayer() {
        final ShadowLayer layer = layer(NOON, 1.0);
        layer.castSprite(block(10, 20), AX, AY, 20, 1.0, 255);

        layer.prepare(SIZE + 10, SIZE + 4, NOON, 1.0);

        assertEquals(SIZE + 10, layer.width());
        assertEquals(SIZE + 4, layer.height());
        assertEquals(SIZE + 10, layer.pixels().length / (SIZE + 4));
        assertEquals(0, count(layer));
        assertArrayEquals(new int[]{0, 0, 0, 0}, layer.changedBox());
    }

    @Test
    void aZeroAlphaOrZeroWidthCastsNothing() {
        final ShadowLayer layer = layer(NOON, 1.0);
        layer.castSprite(block(10, 20), AX, AY, 20, 1.0, 0);
        layer.castSprite(block(10, 20), AX, AY, 20, 0.0, 255);
        layer.castEllipse(AX, AY, 0, 255);
        layer.castEllipse(AX, AY, 10, 0);

        assertEquals(0, count(layer));
    }
}
