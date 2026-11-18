package com.feuerschvenger.perlinsedge.domain.utils;

/**
 * Exact comparisons of floating point values, for the places where a value is a sentinel or an exact result rather
 * than the outcome of an accumulated calculation. Each one behaves exactly like the {@code ==} test it replaces,
 * including the treatment of negative zero and not-a-number.
 */
public final class Doubles {

    /** Not instantiable: the class only holds static helpers. */
    private Doubles() {
    }

    /**
     * Tells whether a value is exactly zero, positive or negative. Not-a-number is not zero.
     *
     * @param value the value to test
     * @return true when {@code value == 0.0}
     */
    public static boolean isZero(double value) {
        return Double.compare(Math.abs(value), 0.0) == 0;
    }

    /**
     * Tells whether two values are exactly equal as numbers. Negative zero equals positive zero, and not-a-number
     * equals nothing, not even itself.
     *
     * @param first  the first value
     * @param second the second value
     * @return true when {@code first == second}
     */
    public static boolean areEqual(double first, double second) {
        return isZero(first - second) || (Double.compare(first, second) == 0 && !Double.isNaN(first));
    }
}
