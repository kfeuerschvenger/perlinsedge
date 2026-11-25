package com.feuerschvenger.perlinsedge.domain.entities.creatures;

/**
 * The kinds of creature the game knows. A species names what a {@link CreatureFactory} builds, so no caller needs to
 * know the class behind it. The animal species join this list with the animal branch.
 */
public enum Species {
    /** A hopping slime. */
    SLIME,
    /** A walking skeleton. */
    SKELETON,
    /** A chest that wakes when the player comes close. */
    MIMIC
}
