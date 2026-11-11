package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxResourceFallbackPainterTest {

    @Test
    void everyResourceTypeHasAVisibleFallbackShape() {
        for (ResourceType type : ResourceType.values()) {
            assertFalse(FxResourceFallbackPainter.blocksFor(type).isEmpty(), type + " would be invisible");
        }
    }

    @Test
    void shapesStandOnTheTileBaseAndFitInsideATile() {
        for (ResourceType type : ResourceType.values()) {
            for (FxResourceFallbackPainter.Block block : FxResourceFallbackPainter.blocksFor(type)) {
                assertTrue(block.y() + block.height() <= 0, type + " has a block below the tile base");
                assertTrue(block.width() > 0 && block.height() > 0, type + " has an empty block");
                assertTrue(Math.abs(block.x()) * FxResourceFallbackPainter.CELL <= 32,
                        type + " reaches beyond half a tile width");
                assertTrue(block.x() + block.width() <= 16, type + " reaches beyond half a tile width");
            }
        }
    }

    @Test
    void shapesOfDifferentResourcesAreTellableApart() {
        for (ResourceType a : ResourceType.values()) {
            for (ResourceType b : ResourceType.values()) {
                if (a != b) {
                    assertFalse(FxResourceFallbackPainter.blocksFor(a).equals(FxResourceFallbackPainter.blocksFor(b)));
                }
            }
        }
    }
}
