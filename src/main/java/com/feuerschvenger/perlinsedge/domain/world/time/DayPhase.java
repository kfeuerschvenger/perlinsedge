package com.feuerschvenger.perlinsedge.domain.world.time;

/**
 * The four stretches of a day, in the order they follow each other. A cycle starts at the beginning of the dawn.
 */
public enum DayPhase {
    /** The sun comes up in the east and the tint goes from the night blue to a warm orange and then on to neutral. */
    DAWN,
    /** Full daylight, with the sun crossing the sky and the shadows at their strongest. */
    DAY,
    /** The sun goes down in the west and the tint goes from neutral to a warm orange and then to the night blue. */
    DUSK,
    /** The sun is below the horizon: no shadows, a deep blue tint. */
    NIGHT
}
