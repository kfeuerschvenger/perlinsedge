package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxBuildingPainterTest {

    @Test
    void chestLidIsADiamondWithAPositiveHeight() {
        FxBuildingPainter.ChestLid lid = FxBuildingPainter.chestLid(100, 8);

        assertTrue(lid.topY() < lid.sideY(), "back vertex above the sides");
        assertTrue(lid.sideY() < lid.bottomY(), "sides above the front vertex");
        assertEquals(16, lid.bottomY() - lid.topY(), 1e-9);
    }

    @Test
    void chestLidSidesSitHalfwayBetweenBackAndFront() {
        FxBuildingPainter.ChestLid lid = FxBuildingPainter.chestLid(40, 5);

        assertEquals((lid.topY() + lid.bottomY()) / 2, lid.sideY(), 1e-9);
    }
}
