package com.feuerschvenger.perlinsedge.app.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RespawnCountdownTest {

    @Test
    void aFullTimerShowsItsWholeSeconds() {
        assertEquals(5, RespawnCountdown.secondsRemaining(5.0));
    }

    @Test
    void aPartialSecondRoundsUp() {
        assertEquals(5, RespawnCountdown.secondsRemaining(4.99));
        assertEquals(4, RespawnCountdown.secondsRemaining(4.0));
        assertEquals(1, RespawnCountdown.secondsRemaining(0.01));
    }

    @Test
    void aDeadPlayerNeverSeesZero() {
        assertEquals(1, RespawnCountdown.secondsRemaining(0));
        assertEquals(1, RespawnCountdown.secondsRemaining(-1));
    }

    @Test
    void theLabelCarriesTheSeconds() {
        assertEquals("Respawning in 3", RespawnCountdown.label(2.2));
    }
}
