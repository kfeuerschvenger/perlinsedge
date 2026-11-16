package com.feuerschvenger.perlinsedge.domain.world.light;

/**
 * How the light of every source is drawn. Inside a light the overlay of the sky turns toward the colour of the light and
 * its opacity toward a modest warm strength, by how much light there is, so a lit patch is a warm wash and never a
 * hole in the tint that shows the plain ground.
 *
 * @param warmStrength      the opacity of the warm overlay at the centre of a light in the deepest dark, 0 to 1
 * @param referenceDarkness the tint strength of the deepest dark, which the warm strength is scaled against: it is full
 *                          at this strength and fades out as the tint weakens, so a light does nothing at midday,
 *                          above zero
 */
public record LightStyle(double warmStrength, double referenceDarkness) {

    /**
     * Validates the style.
     *
     * @throws IllegalArgumentException if the warm strength is outside 0 to 1 or the reference darkness is not above zero
     */
    public LightStyle {
        if (!(warmStrength >= 0.0 && warmStrength <= 1.0)) {
            throw new IllegalArgumentException("The warm strength of a light must be from 0 to 1");
        }
        if (!(referenceDarkness > 0.0)) {
            throw new IllegalArgumentException("The reference darkness must be above zero");
        }
    }
}
