package com.feuerschvenger.perlinsedge.domain.world.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MapTypeTest {

    @Test
    void theDenseForestIsShownByItsTitleCaseName() {
        assertEquals("Dense Forest", MapType.DENSE_FOREST.getDisplayName());
    }

    @Test
    void everyMapTypeHasANameToShow() {
        for (MapType type : MapType.values()) {
            assertFalse(type.getDisplayName().isBlank(), type.name());
        }
    }
}
