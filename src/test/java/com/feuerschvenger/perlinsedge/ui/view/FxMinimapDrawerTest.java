package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FxMinimapDrawerTest {

    @Test
    void opaqueColoursPackIntoArgb() {
        assertEquals(0xFFFF0000, FxMinimapDrawer.toArgb(new Rgba(1, 0, 0, 1)));
        assertEquals(0xFF00FF00, FxMinimapDrawer.toArgb(new Rgba(0, 1, 0, 1)));
        assertEquals(0xFF0000FF, FxMinimapDrawer.toArgb(new Rgba(0, 0, 1, 1)));
    }

    @Test
    void channelsAreRoundedAndClamped() {
        assertEquals(0x80102030, FxMinimapDrawer.toArgb(Rgba.rgb(16, 32, 48).withOpacityFactor(128 / 255.0)));
        assertEquals(0xFF000000, FxMinimapDrawer.toArgb(new Rgba(0, 0, 0, 1)));
        assertEquals(0xFFFFFFFF, FxMinimapDrawer.toArgb(new Rgba(1, 1, 1, 1)));
    }

    @Test
    void namedTerrainColourPacksWithFullOpacity() {
        assertEquals(0xFF5A5A5A, FxMinimapDrawer.toArgb(Rgba.rgb(90, 90, 90)));
    }
}
