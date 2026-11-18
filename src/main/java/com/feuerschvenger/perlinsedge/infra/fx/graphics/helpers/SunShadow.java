package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundShadow;

/**
 * What the sun asks of the shadows of a frame: the way they fall, how long they are and how dark.
 *
 * @param angle    direction of the shadows on the screen in radians
 * @param length   how long shadows are as a multiple of the height of what casts them
 * @param strength how dark shadows are as a share of their full opacity, 0 to 1
 */
public record SunShadow(double angle, double length, double strength) {

    /** @return whether the shadows are strong enough to be cast at all */
    public boolean visible() {
        return strength >= FxShadowHelper.MIN_VISIBLE_STRENGTH;
    }

    /** @return the opacity the shadow layers are drawn at: the opacity of a shadow times the strength, at most 1 */
    public double opacity() {
        return GroundShadow.OPACITY * Math.min(1.0, strength);
    }
}
