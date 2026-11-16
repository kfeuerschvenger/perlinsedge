package com.feuerschvenger.perlinsedge.domain.world.light;

/**
 * How the light of every source is drawn: the number of rings its fade is cut into and how strong the warm glow near
 * its centre is.
 *
 * @param rings            the number of flat rings the fade of a light is cut into, above zero
 * @param glowStrength     the opacity of the warm glow at the centre of a light in the deepest dark, 0 to 1
 * @param referenceDarkness the tint strength of the deepest dark, which the glow is scaled against: the glow is full
 *                          at this strength, and fades out as the tint weakens, above zero
 */
public record LightStyle(int rings, double glowStrength, double referenceDarkness) {

    /**
     * Validates the style.
     *
     * @throws IllegalArgumentException if there are no rings, the glow is outside 0 to 1 or the reference darkness is
     *                                  not above zero
     */
    public LightStyle {
        if (rings < 1) {
            throw new IllegalArgumentException("A light needs at least one ring");
        }
        if (!(glowStrength >= 0.0 && glowStrength <= 1.0)) {
            throw new IllegalArgumentException("The glow of a light must be from 0 to 1");
        }
        if (!(referenceDarkness > 0.0)) {
            throw new IllegalArgumentException("The reference darkness must be above zero");
        }
    }
}
