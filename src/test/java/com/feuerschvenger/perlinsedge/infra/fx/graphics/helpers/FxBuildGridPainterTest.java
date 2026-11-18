package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxBuildGridPainterTest {

    @Test
    void theHoveredTileHasTheStrongestOutlineAndItIsFaint() {
        assertEquals(FxBuildGridPainter.MAX_OPACITY, FxBuildGridPainter.opacity(0, 0), 1e-9);
        assertTrue(FxBuildGridPainter.MAX_OPACITY <= 0.35, "low alpha");
    }

    @Test
    void theOutlinesFadeWithTheDistanceAndVanishPastTheRadius() {
        assertTrue(FxBuildGridPainter.opacity(1, 0) > FxBuildGridPainter.opacity(2, 0));
        assertTrue(FxBuildGridPainter.opacity(2, 0) > FxBuildGridPainter.opacity(3, 0));
        assertTrue(FxBuildGridPainter.opacity(3, 0) > 0, "three tiles away is still inside the grid");
        assertEquals(0.0, FxBuildGridPainter.opacity(4, 0), 1e-9);
        assertEquals(0.0, FxBuildGridPainter.opacity(3, 3), 1e-9);
    }

    @Test
    void theFadeIsTheSameInEveryDirection() {
        assertEquals(FxBuildGridPainter.opacity(2, 1), FxBuildGridPainter.opacity(-1, 2), 1e-9);
        assertEquals(FxBuildGridPainter.opacity(0, 3), FxBuildGridPainter.opacity(-3, 0), 1e-9);
    }
}
