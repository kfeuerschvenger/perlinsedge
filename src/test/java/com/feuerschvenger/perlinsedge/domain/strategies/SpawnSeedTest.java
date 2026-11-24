package com.feuerschvenger.perlinsedge.domain.strategies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpawnSeedTest {

    @Test
    void seedIsPinnedForFixedTilesIncludingNegativeOnes() {
        assertEquals(0xE220_A839_7B1D_CDAFL, SpawnSeed.of(0, 0));
        assertEquals(0xB370_3AD8_9450_7022L, SpawnSeed.of(1, 2));
        assertEquals(0xE4D9_7177_1B65_2C20L, SpawnSeed.of(-1, -1));
        assertEquals(0xB772_7CEE_ABBA_EE39L, SpawnSeed.of(-7, 3));
        assertEquals(0x1939_1B55_6C95_9740L, SpawnSeed.of(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }
}
