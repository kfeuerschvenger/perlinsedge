package com.feuerschvenger.perlinsedge.domain.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HashesTest {

    @Test
    void mixIsPinnedForFixedInputs() {
        assertEquals(0xE220_A839_7B1D_CDAFL, Hashes.mix(0, 0));
        assertEquals(0xB370_3AD8_9450_7022L, Hashes.mix(1, 2));
        assertEquals(0xE4D9_7177_1B65_2C20L, Hashes.mix(-1, -1));
        assertEquals(0xB772_7CEE_ABBA_EE39L, Hashes.mix(-7, 3));
        assertEquals(0x1939_1B55_6C95_9740L, Hashes.mix(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void mixDependsOnTheOrderOfItsArguments() {
        assertNotEquals(Hashes.mix(1, 2), Hashes.mix(2, 1));
    }
}
