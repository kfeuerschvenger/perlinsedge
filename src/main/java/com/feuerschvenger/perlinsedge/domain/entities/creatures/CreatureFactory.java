package com.feuerschvenger.perlinsedge.domain.entities.creatures;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;

/**
 * Factory of creatures: it builds the class that goes with a species and wires its strategies, so no caller calls a
 * species constructor directly and a new species is one more case here. The switch has no default, so the compiler
 * refuses a species that nobody builds. The factory holds no state, so it is used through its static method.
 */
public final class CreatureFactory {

    /** Private so the factory is never instantiated: it is used through its static method. */
    private CreatureFactory() {
        // Nothing to initialise: the factory has no state
    }

    /**
     * Builds a creature of a species on a tile.
     *
     * @param species the kind of creature to build
     * @param tileX   tile column it starts on
     * @param tileY   tile row it starts on
     * @return a new creature, with the strategies of its species
     */
    public static Creature create(Species species, int tileX, int tileY) {
        return switch (species) {
            case SLIME -> new Slime(tileX, tileY);
            case SKELETON -> new Skeleton(tileX, tileY);
            case MIMIC -> new Mimic(tileX, tileY);
        };
    }
}
