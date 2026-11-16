package com.feuerschvenger.perlinsedge.domain.world.time;

/**
 * The four stretches of a day, in the order they follow each other. A cycle starts at the beginning of the dawn.
 */
public enum DayPhase {
    /** The sun comes up in the east and the world warms from the night blue to the neutral day light. */
    DAWN,
    /** Full daylight, with the sun crossing the sky and the shadows at their strongest. */
    DAY,
    /** The sun goes down in the west and the world cools from the neutral day light to the night blue. */
    DUSK,
    /** The sun is below the horizon: no shadows, a moderate blue tint. */
    NIGHT
}
