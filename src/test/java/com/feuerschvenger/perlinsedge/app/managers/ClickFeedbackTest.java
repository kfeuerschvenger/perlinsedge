package com.feuerschvenger.perlinsedge.app.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClickFeedbackTest {

    @Test
    void showsNothingBeforeAnyClick() {
        ClickFeedback feedback = new ClickFeedback();

        assertEquals(ClickFeedback.Kind.NONE, feedback.kind(0));
        assertEquals(0.0, feedback.strength(0));
        assertEquals(-1, feedback.getTileX());
    }

    @Test
    void anAcceptedClickShowsAcceptedUntilTheFlashEnds() {
        ClickFeedback feedback = new ClickFeedback();
        feedback.flash(4, 7, true, 10.0);

        assertEquals(ClickFeedback.Kind.ACCEPTED, feedback.kind(10.0));
        assertEquals(ClickFeedback.Kind.ACCEPTED, feedback.kind(10.0 + ClickFeedback.DURATION_SECONDS - 0.01));
        assertEquals(ClickFeedback.Kind.NONE, feedback.kind(10.0 + ClickFeedback.DURATION_SECONDS + 1e-6));
        assertEquals(4, feedback.getTileX());
        assertEquals(7, feedback.getTileY());
    }

    @Test
    void aRefusedClickShowsRefused() {
        ClickFeedback feedback = new ClickFeedback();
        feedback.flash(1, 1, false, 0.0);

        assertEquals(ClickFeedback.Kind.REJECTED, feedback.kind(0.1));
    }

    @Test
    void theFlashFadesFromFullToNothing() {
        ClickFeedback feedback = new ClickFeedback();
        feedback.flash(1, 1, true, 5.0);

        assertEquals(1.0, feedback.strength(5.0), 1e-9);
        assertEquals(0.5, feedback.strength(5.0 + ClickFeedback.DURATION_SECONDS / 2), 1e-9);
        assertEquals(0.0, feedback.strength(5.0 + ClickFeedback.DURATION_SECONDS + 1e-6));
    }

    @Test
    void aLaterClickReplacesTheEarlierOne() {
        ClickFeedback feedback = new ClickFeedback();
        feedback.flash(1, 1, true, 0.0);
        feedback.flash(2, 3, false, 0.1);

        assertEquals(ClickFeedback.Kind.REJECTED, feedback.kind(0.2));
        assertEquals(2, feedback.getTileX());
        assertEquals(3, feedback.getTileY());
    }

    @Test
    void aTimeBeforeTheClickShowsNothing() {
        ClickFeedback feedback = new ClickFeedback();
        feedback.flash(1, 1, true, 5.0);

        assertEquals(ClickFeedback.Kind.NONE, feedback.kind(4.0), "a game that was reset restarts its clock");
    }

    @Test
    void clearingStopsTheFlash() {
        ClickFeedback feedback = new ClickFeedback();
        feedback.flash(1, 1, true, 0.0);
        feedback.clear();

        assertEquals(ClickFeedback.Kind.NONE, feedback.kind(0.1));
    }
}
