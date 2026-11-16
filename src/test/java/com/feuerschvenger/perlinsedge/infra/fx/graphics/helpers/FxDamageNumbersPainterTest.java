package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.utils.DamageNumbers;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxDamageNumbersPainterTest {

    @Test
    void everyKindHasItsOwnColour() {
        Set<Color> colours = new HashSet<>();
        for (DamageNumbers.Kind kind : DamageNumbers.Kind.values()) {
            colours.add(FxDamageNumbersPainter.color(kind));
        }

        assertEquals(DamageNumbers.Kind.values().length, colours.size());
    }

    @Test
    void resourcesAreWhiteEnemiesYellowThePlayerRedAndHealsGreen() {
        Color white = FxDamageNumbersPainter.color(DamageNumbers.Kind.RESOURCE);
        Color yellow = FxDamageNumbersPainter.color(DamageNumbers.Kind.ENEMY);
        Color red = FxDamageNumbersPainter.color(DamageNumbers.Kind.PLAYER);
        Color green = FxDamageNumbersPainter.color(DamageNumbers.Kind.HEAL);

        assertTrue(white.getRed() > 0.9 && white.getGreen() > 0.9 && white.getBlue() > 0.8);
        assertTrue(yellow.getRed() > 0.9 && yellow.getGreen() > 0.7 && yellow.getBlue() < 0.4);
        assertTrue(red.getRed() > 0.8 && red.getGreen() < 0.5 && red.getBlue() < 0.5);
        assertTrue(green.getGreen() > 0.8 && green.getRed() < 0.6);
    }

    @Test
    void aResourceNumberStartsHigherThanAnEntityNumber() {
        assertTrue(FxDamageNumbersPainter.lift(DamageNumbers.Kind.RESOURCE)
                > FxDamageNumbersPainter.lift(DamageNumbers.Kind.ENEMY));
        assertEquals(FxDamageNumbersPainter.lift(DamageNumbers.Kind.ENEMY),
                FxDamageNumbersPainter.lift(DamageNumbers.Kind.PLAYER));
    }

    @Test
    void theOutlineIsTheDarkTextColourOfTheGui() {
        assertEquals(Color.web("#1d1830"), FxDamageNumbersPainter.OUTLINE);
    }
}
