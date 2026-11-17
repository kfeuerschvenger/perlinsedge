package com.feuerschvenger.perlinsedge.infra.fx.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import org.junit.jupiter.api.Test;

class GroundShadowTest {

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
        assertTrue(GroundShadow.widthFor(BuildingType.WORKBENCH) > GroundShadow.widthFor(BuildingType.STANDING_TORCH));
    }
}
