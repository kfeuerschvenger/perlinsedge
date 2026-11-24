package com.feuerschvenger.perlinsedge.domain.strategies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpawnSeedTest {

    @Test
    void seedIsPinnedForFixedTilesIncludingNegativeOnes() {
        assertEquals(0x0A3C_5B55_F34C_03D6L, SpawnSeed.of(5, 5));
        assertEquals(0xA480_6683_09A7_BA3AL, SpawnSeed.of(100, -100));
        assertEquals(0x35DA_DF3D_8DEC_1954L, SpawnSeed.of(Integer.MAX_VALUE, 0));
        assertEquals(0x2549_3CC6_3225_736CL, SpawnSeed.of(0, Integer.MIN_VALUE));
    }
}
