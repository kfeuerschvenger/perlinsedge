package com.feuerschvenger.perlinsedge.infra.fx.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HurtReactionTest {

    @Test
    void thereIsNoReactionBeforeAHitOrAfterItEnds() {
        for (double since : new double[]{Double.POSITIVE_INFINITY, -0.1, 5.0}) {
            assertEquals(HurtReaction.NO_FLASH, HurtReaction.flashPhase(since));
            assertEquals(0.0, HurtReaction.shakeOffset(since));
            assertEquals(0.0, HurtReaction.glowOpacity(since));
        }
        assertEquals(HurtReaction.NO_FLASH, HurtReaction.flashPhase(HurtReaction.FLASH_SECONDS));
        assertEquals(0.0, HurtReaction.shakeOffset(HurtReaction.FLASH_SECONDS));
        assertEquals(0.0, HurtReaction.glowOpacity(HurtReaction.GLOW_SECONDS));
    }

    @Test
    void theFlashAlternatesBetweenRedAndWhiteStartingWithRed() {
        assertEquals(HurtReaction.RED, HurtReaction.flashPhase(0.0));
        assertEquals(HurtReaction.WHITE, HurtReaction.flashPhase(HurtReaction.PHASE_SECONDS * 1.5));
        assertEquals(HurtReaction.RED, HurtReaction.flashPhase(HurtReaction.PHASE_SECONDS * 2.5));
        assertEquals(HurtReaction.WHITE, HurtReaction.flashPhase(HurtReaction.PHASE_SECONDS * 3.5));
    }

    @Test
    void theFlashLastsBetweenOneAndTwoTenthsOfASecond() {
        assertTrue(HurtReaction.FLASH_SECONDS >= 0.15 && HurtReaction.FLASH_SECONDS <= 0.2);
        assertTrue(HurtReaction.GLOW_SECONDS >= HurtReaction.FLASH_SECONDS);
    }

    @Test
    void theShakeSwingsToBothSidesWithinTheAmplitudeAndDecays() {
        boolean left = false;
        boolean right = false;
        double early = 0;
        double late = 0;
        for (double s = 0; s < HurtReaction.FLASH_SECONDS; s += 0.0005) {
            double offset = HurtReaction.shakeOffset(s);
            assertTrue(Math.abs(offset) <= HurtReaction.SHAKE_AMPLITUDE_ART_PIXELS + 1e-9);
            left |= offset < -0.3;
            right |= offset > 0.3;
            if (s < HurtReaction.FLASH_SECONDS / 3) {
                early = Math.max(early, Math.abs(offset));
            } else if (s > HurtReaction.FLASH_SECONDS * 2 / 3) {
                late = Math.max(late, Math.abs(offset));
            }
        }
        assertTrue(left && right);
        assertTrue(late < early);
    }

    @Test
    void theScreenShakeIsWholePixelsAndScalesWithTheArt() {
        for (double s = 0; s < HurtReaction.FLASH_SECONDS; s += 0.003) {
            double offset = HurtReaction.shakeScreenOffset(s, 3.0);
            assertEquals(Math.rint(offset), offset);
            assertTrue(Math.abs(offset) <= 3.0);
        }
        assertEquals(0.0, HurtReaction.shakeScreenOffset(1.0, 4.0));
    }

    @Test
    void theGlowStartsAtItsMaximumAndFadesSmoothlyAndStaysSubtle() {
        assertEquals(HurtReaction.GLOW_MAX_OPACITY, HurtReaction.glowOpacity(0.0), 1e-9);
        assertTrue(HurtReaction.GLOW_MAX_OPACITY <= 0.35);
        double previous = HurtReaction.glowOpacity(0.0);
        for (double s = 0.005; s < HurtReaction.GLOW_SECONDS; s += 0.005) {
            double now = HurtReaction.glowOpacity(s);
            assertTrue(now < previous && now > 0.0, "at " + s);
            previous = now;
        }
    }

    @Test
    void tintingMixesTheFlashColourIntoPaintedPixelsAndKeepsTheirAlpha() {
        int pixel = 0xCC204060;
        int red = HurtReaction.tint(pixel, HurtReaction.RED);
        int white = HurtReaction.tint(pixel, HurtReaction.WHITE);
        assertEquals(0xCC, red >>> 24);
        assertEquals(0xCC, white >>> 24);
        assertTrue(((red >> 16) & 0xFF) > 0xA0 && (red & 0xFF) < 0x60, "red leans red");
        assertTrue((white & 0xFF) > 0xC0 && ((white >> 8) & 0xFF) > 0xC0, "white leans white");
    }

    @Test
    void tintingLeavesTransparentPixelsAndUnknownPhasesAlone() {
        assertEquals(0, HurtReaction.tint(0, HurtReaction.RED));
        assertEquals(0x00FFFFFF, HurtReaction.tint(0x00FFFFFF, HurtReaction.WHITE));
        assertEquals(0xFF123456, HurtReaction.tint(0xFF123456, HurtReaction.NO_FLASH));
    }
}
