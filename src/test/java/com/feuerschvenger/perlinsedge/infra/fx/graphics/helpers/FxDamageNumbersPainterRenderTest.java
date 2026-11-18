package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.utils.DamageNumbers;
import com.feuerschvenger.perlinsedge.ui.view.CanvasProbe;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxDamageNumbersPainterRenderTest {

    private static final int SIZE = 200;
    private static final FxDamageNumbersPainter.TileProjection PROJECTION =
            new FxDamageNumbersPainter.TileProjection(32, 16, 100, 100, 1.0);

    private static DamageNumbers numbers(DamageNumbers.Kind kind, double tileX, double tileY) {
        final DamageNumbers numbers = new DamageNumbers(new Random(7));
        numbers.spawn(kind, 25, tileX, tileY, 0);
        return numbers;
    }

    private static CanvasProbe draw(DamageNumbers numbers, double time) {
        return CanvasProbe.render(SIZE, SIZE,
                gc -> FxDamageNumbersPainter.draw(gc, numbers, time, PROJECTION, SIZE, SIZE));
    }

    @Test
    void anActiveNumberIsDrawnInTheColourOfItsKind() {
        for (DamageNumbers.Kind kind : DamageNumbers.Kind.values()) {
            final CanvasProbe shot = draw(numbers(kind, 2, 2), 0.1);

            assertTrue(shot.painted() > 0, kind + " number not drawn");
            assertTrue(shot.near(FxDamageNumbersPainter.color(kind), 24) > 0, kind + " colour missing");
        }
    }

    @Test
    void aNumberOutsideTheScreenIsSkipped() {
        assertEquals(0, draw(numbers(DamageNumbers.Kind.ENEMY, 400, 0), 0.1).painted());
        assertEquals(0, draw(numbers(DamageNumbers.Kind.ENEMY, -400, 0), 0.1).painted());
    }

    @Test
    void aNumberThatHasFadedIsNotDrawn() {
        assertEquals(0, draw(numbers(DamageNumbers.Kind.ENEMY, 2, 2), DamageNumbers.LIFETIME_SECONDS + 1).painted());
    }

    @Test
    void anEmptyPoolDrawsNothing() {
        assertEquals(0, draw(new DamageNumbers(new Random(1)), 0).painted());
    }

    @Test
    void theNumberRisesAndFadesWithTime() {
        final DamageNumbers numbers = numbers(DamageNumbers.Kind.ENEMY, 2, 2);

        final CanvasProbe early = draw(numbers, 0.05);
        final CanvasProbe late = draw(numbers, DamageNumbers.LIFETIME_SECONDS * 0.7);

        assertFalse(early.sameAs(late));
        assertTrue(late.firstPaintedRow() < early.firstPaintedRow(), "the number moves up the screen");
    }

    @Test
    void aResourceNumberStartsHigherThanAnEnemyNumber() {
        final CanvasProbe resource = draw(numbers(DamageNumbers.Kind.RESOURCE, 2, 2), 0.05);
        final CanvasProbe enemy = draw(numbers(DamageNumbers.Kind.ENEMY, 2, 2), 0.05);

        assertTrue(resource.firstPaintedRow() < enemy.firstPaintedRow());
    }

    @Test
    void aBiggerZoomDrawsABiggerNumber() {
        final DamageNumbers numbers = numbers(DamageNumbers.Kind.ENEMY, 2, 2);
        final FxDamageNumbersPainter.TileProjection zoomed =
                new FxDamageNumbersPainter.TileProjection(64, 32, 100, 100, 2.0);

        final CanvasProbe small = draw(numbers, 0.05);
        final CanvasProbe big = CanvasProbe.render(SIZE * 2, SIZE * 2,
                gc -> FxDamageNumbersPainter.draw(gc, numbers, 0.05, zoomed, SIZE * 2, SIZE * 2));

        assertTrue(big.painted() > small.painted());
    }
}
