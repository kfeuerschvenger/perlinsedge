package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapRequestTest {

    private static final long SEED = 7L;
    private static final int WIDTH = 30;
    private static final int HEIGHT = 20;

    @Test
    void keepsWhatItWasGiven() {
        final var request = new MapRequest(MapType.SWAMP, SEED, WIDTH, HEIGHT);

        assertEquals(MapType.SWAMP, request.type());
        assertEquals(SEED, request.seed());
        assertEquals(WIDTH, request.width());
        assertEquals(HEIGHT, request.height());
    }

    @Test
    void refusesAMissingType() {
        assertThrows(NullPointerException.class, () -> new MapRequest(null, SEED, WIDTH, HEIGHT));
    }

    @Test
    void refusesAnEmptySide() {
        assertThrows(IllegalArgumentException.class, () -> new MapRequest(MapType.SWAMP, SEED, 0, HEIGHT));
        assertThrows(IllegalArgumentException.class, () -> new MapRequest(MapType.SWAMP, SEED, WIDTH, -1));
    }
}
