package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpriteLayoutTest {

    @Test
    void aTileDirectionShowsTheScreenDirectionItHeadsTo() {
        assertEquals(1, SpriteLayout.playerColumn(Entity.DIR_UP));
        assertEquals(2, SpriteLayout.playerColumn(Entity.DIR_UP_RIGHT));
        assertEquals(3, SpriteLayout.playerColumn(Entity.DIR_RIGHT));
        assertEquals(4, SpriteLayout.playerColumn(Entity.DIR_DOWN_RIGHT));
        assertEquals(5, SpriteLayout.playerColumn(Entity.DIR_DOWN));
        assertEquals(6, SpriteLayout.playerColumn(Entity.DIR_DOWN_LEFT));
        assertEquals(7, SpriteLayout.playerColumn(Entity.DIR_LEFT));
    }

    @Test
    void theLastDirectionWrapsToTheFirstColumn() {
        assertEquals(0, SpriteLayout.playerColumn(Entity.DIR_UP_LEFT));
    }

    @Test
    void aSlimeOnTheGroundShowsTheRestFrame() {
        assertEquals(0, SpriteLayout.slimeFrame(false, 0.7));
    }

    @Test
    void aJumpWalksThroughTheFiveJumpFrames() {
        assertEquals(1, SpriteLayout.slimeFrame(true, 0.0));
        assertEquals(2, SpriteLayout.slimeFrame(true, 0.25));
        assertEquals(3, SpriteLayout.slimeFrame(true, 0.45));
        assertEquals(4, SpriteLayout.slimeFrame(true, 0.65));
        assertEquals(5, SpriteLayout.slimeFrame(true, 0.85));
    }

    @Test
    void aPhaseOutsideTheJumpIsClamped() {
        assertEquals(5, SpriteLayout.slimeFrame(true, 1.0));
        assertEquals(5, SpriteLayout.slimeFrame(true, 3.0));
        assertEquals(1, SpriteLayout.slimeFrame(true, -1.0));
    }
}
