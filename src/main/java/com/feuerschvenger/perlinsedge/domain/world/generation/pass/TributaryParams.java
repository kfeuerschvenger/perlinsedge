package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How a river branches. A river has to be long enough before it may branch, and then branches with a small chance on
 * every step.
 *
 * @param minRiverLength how many steps a river has run before it may branch, 0 or more
 * @param chance         the chance of a branch on each step after that, from 0 to 1
 * @param minLength      the length of the shortest branch in steps, at least 1
 * @param lengthSpread   how many different lengths a branch can have above the shortest, at least 1
 */
public record TributaryParams(int minRiverLength, float chance, int minLength, int lengthSpread) {

    /** Checks the lengths are usable and the chance is a probability. */
    public TributaryParams {
        if (minRiverLength < 0) {
            throw new IllegalArgumentException("the river length before a branch cannot be negative: "
                    + minRiverLength);
        }
        if (Float.isNaN(chance) || chance < 0F || chance > 1F) {
            throw new IllegalArgumentException("the branch chance must be from 0 to 1: " + chance);
        }
        if (minLength < 1 || lengthSpread < 1) {
            throw new IllegalArgumentException("a branch needs a length and a spread of at least 1: " + minLength
                    + " and " + lengthSpread);
        }
    }

    /**
     * Returns the number of steps of the longest branch.
     *
     * @return the shortest length plus the spread less one
     */
    public int maxLength() {
        return minLength + lengthSpread - 1;
    }
}
