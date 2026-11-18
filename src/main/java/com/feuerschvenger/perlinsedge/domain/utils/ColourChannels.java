package com.feuerschvenger.perlinsedge.domain.utils;

/**
 * Something that has a red, a green and a blue channel from 0.0 to 1.0. The immutable {@link Rgba} is one, and so is the
 * tint of the sky, which is worked out in place every frame so reading it allocates nothing.
 */
public interface ColourChannels {

    /** @return the red channel, 0.0 to 1.0 */
    double r();

    /** @return the green channel, 0.0 to 1.0 */
    double g();

    /** @return the blue channel, 0.0 to 1.0 */
    double b();
}
