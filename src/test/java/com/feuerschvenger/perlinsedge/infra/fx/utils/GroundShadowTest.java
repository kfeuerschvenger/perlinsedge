package com.feuerschvenger.perlinsedge.infra.fx.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import org.junit.jupiter.api.Test;

class GroundShadowTest {

    @Test
    void theEllipseSizesAscendAndAreTwiceAsWideAsTall() {
        for (int i = 1; i < GroundShadow.bucketCount(); i++) {
            assertTrue(GroundShadow.widthOf(i) > GroundShadow.widthOf(i - 1));
        }
        for (int i = 0; i < GroundShadow.bucketCount(); i++) {
            int width = GroundShadow.widthOf(i);
            assertEquals(width / 2.0, GroundShadow.heightOf(width), 1.0);
        }
    }

    @Test
    void aWantedWidthPicksTheNearestSize() {
        assertEquals(0, GroundShadow.bucketFor(0));
        assertEquals(0, GroundShadow.bucketFor(6));
        assertEquals(1, GroundShadow.bucketFor(9.4));
        assertEquals(GroundShadow.bucketCount() - 1, GroundShadow.bucketFor(500));
        int sixteen = GroundShadow.bucketFor(16);
        assertEquals(16, GroundShadow.widthOf(sixteen));
    }

    @Test
    void theEllipseCoversItsCentreAndLeavesTheCornersEmpty() {
        for (int i = 0; i < GroundShadow.bucketCount(); i++) {
            int width = GroundShadow.widthOf(i);
            int height = GroundShadow.heightOf(width);
            assertTrue(GroundShadow.covers(width, height, width / 2, height / 2));
            assertFalse(GroundShadow.covers(width, height, 0, 0));
            assertFalse(GroundShadow.covers(width, height, width - 1, height - 1));
            assertTrue(GroundShadow.covers(width, height, width / 2, 0) || GroundShadow.covers(width, height, width / 2, 1));
        }
    }

    @Test
    void theEllipseIsSymmetricAndSolid() {
        int width = 22;
        int height = GroundShadow.heightOf(width);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                assertEquals(GroundShadow.covers(width, height, x, y),
                        GroundShadow.covers(width, height, width - 1 - x, y));
                assertEquals(GroundShadow.covers(width, height, x, y),
                        GroundShadow.covers(width, height, x, height - 1 - y));
            }
        }
    }

    @Test
    void theShadowIsBlackAtAboutThirtyPercent() {
        assertEquals(0.30, GroundShadow.OPACITY, 1e-9);
    }

    @Test
    void everyResourceAndBuildingTypeHasASizeRule() {
        for (ResourceType type : ResourceType.values()) {
            double width = GroundShadow.widthFor(type, 24);
            assertTrue(width > 0 && width <= 24, type.name());
        }
        for (BuildingType type : BuildingType.values()) {
            assertTrue(GroundShadow.widthFor(type) >= 0, type.name());
        }
    }

    @Test
    void aTreeShadowIsNarrowerThanItsCanopyAndARockShadowFillsItsSprite() {
        assertTrue(GroundShadow.widthFor(ResourceType.TREE, 30) < 0.7 * 30);
        assertTrue(GroundShadow.widthFor(ResourceType.STONE, 30) > 0.8 * 30);
    }

    @Test
    void bridgesAndDocksLieFlatAndCastNoShadow() {
        assertEquals(0.0, GroundShadow.widthFor(BuildingType.BRIDGE));
        assertEquals(0.0, GroundShadow.widthFor(BuildingType.DOCK));
        assertTrue(GroundShadow.widthFor(BuildingType.WALL) > GroundShadow.widthFor(BuildingType.STANDING_TORCH));
    }
}
