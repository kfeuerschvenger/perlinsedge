package com.feuerschvenger.perlinsedge.ui.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HintLabelTest {

    @Test
    void aLabelThatIsFullyVisibleContinuesAtTheStartOfTheHold() {
        assertEquals(120, HintLabel.resumePositionMs(1), 1e-9);
    }

    @Test
    void aLabelThatIsFadingContinuesAtTheMatchingPointOfTheFadeIn() {
        assertEquals(60, HintLabel.resumePositionMs(0.5), 1e-9);
        assertEquals(12, HintLabel.resumePositionMs(0.1), 1e-9);
    }

    @Test
    void theResumePositionIsClampedToTheFadeIn() {
        assertEquals(0, HintLabel.resumePositionMs(-0.3), 1e-9);
        assertEquals(120, HintLabel.resumePositionMs(1.7), 1e-9);
    }

    @Test
    void showingAgainMidFadeKeepsTheCurrentOpacityInsteadOfPoppingToFull() {
        double opacity = FxToolkitSupport.onFx(() -> {
            HintLabel label = new HintLabel();
            label.show("first");
            label.setOpacity(0.35);

            label.show("second");
            return label.getOpacity();
        });

        assertEquals(0.35, opacity, 0.05);
    }

    @Test
    void aHiddenLabelStartsFromTransparent() {
        double opacity = FxToolkitSupport.onFx(() -> {
            HintLabel label = new HintLabel();
            label.show("hello");
            return label.getOpacity();
        });

        assertTrue(opacity < 0.5, "the fade in begins at zero, not at full opacity: " + opacity);
    }
}
