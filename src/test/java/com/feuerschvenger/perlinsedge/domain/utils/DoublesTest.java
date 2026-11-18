package com.feuerschvenger.perlinsedge.domain.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoublesTest {

    @Test
    void differsIsTrueForValuesThatAreNotTheSameNumber() {
        assertTrue(Doubles.differs(1.0, 2.0));
        assertTrue(Doubles.differs(0.1 + 0.2, 0.3));
        assertTrue(Doubles.differs(Double.MIN_VALUE, 0.0));
        assertTrue(Doubles.differs(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertFalse(Doubles.differs(1.5, 1.5));
    }

    @Test
    void notANumberDiffersFromItselfAndNegativeZeroDoesNotDifferFromZero() {
        assertTrue(Doubles.differs(Double.NaN, Double.NaN));
        assertFalse(Doubles.differs(0.0, -0.0));
        assertFalse(Doubles.differs(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    @Test
    void infinitiesAreNeverZeroAndEqualOnlyToThemselves() {
        assertFalse(Doubles.isZero(Double.POSITIVE_INFINITY));
        assertFalse(Doubles.isZero(Double.NEGATIVE_INFINITY));
        assertTrue(Doubles.areEqual(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertTrue(Doubles.areEqual(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertFalse(Doubles.areEqual(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertFalse(Doubles.areEqual(Double.POSITIVE_INFINITY, Double.MAX_VALUE));
        assertTrue(Doubles.differs(Double.POSITIVE_INFINITY, Double.MAX_VALUE));
        assertTrue(Doubles.differs(Double.NEGATIVE_INFINITY, 0.0));
        assertTrue(Doubles.differs(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    @Test
    void notANumberIsNotZeroAndEqualToNothing() {
        assertFalse(Doubles.isZero(Double.NaN));
        assertFalse(Doubles.areEqual(Double.NaN, Double.NaN));
        assertFalse(Doubles.areEqual(Double.NaN, 0.0));
        assertFalse(Doubles.areEqual(Double.NaN, Double.POSITIVE_INFINITY));
        assertTrue(Doubles.differs(Double.NaN, Double.POSITIVE_INFINITY));
        assertTrue(Doubles.differs(1.0, Double.NaN));
    }
}
