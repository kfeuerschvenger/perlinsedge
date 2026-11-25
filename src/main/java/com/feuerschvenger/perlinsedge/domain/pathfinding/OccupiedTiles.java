package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Works out which tiles the living creatures take up, so the player's walks neither cross them nor end on them.
 * A creature takes up the tile it stands on and the tile it is stepping to.
 */
public final class OccupiedTiles {

    /** Not instantiable: the class only holds static rules. */
    private OccupiedTiles() {}

    /**
     * Collects the tiles taken by creatures.
     *
     * @param creatures the creatures, null for none
     * @return the tiles the living ones stand on or step to, never null
     */
    public static Set<PathNode> of(Collection<? extends Creature> creatures) {
        final Set<PathNode> tiles = new HashSet<>();
        if (creatures == null) {
            return tiles;
        }
        for (Creature creature : creatures) {
            if (creature.isDead()) {
                continue;
            }
            tiles.add(new PathNode(creature.getCurrentTileX(), creature.getCurrentTileY()));
            tiles.add(new PathNode(creature.getTargetTileX(), creature.getTargetTileY()));
        }
        return tiles;
    }

    /**
     * Tells whether a tile is taken by a creature, without building the set of {@link #of(Collection)}, so a check that
     * runs every tick allocates nothing.
     *
     * @param creatures the creatures, null for none
     * @param x         tile column
     * @param y         tile row
     * @return true when a living creature stands on the tile or is stepping to it
     */
    public static boolean contains(Collection<? extends Creature> creatures, int x, int y) {
        if (creatures == null) {
            return false;
        }
        for (Creature creature : creatures) {
            if (creature.isDead()) {
                continue;
            }
            if ((creature.getCurrentTileX() == x && creature.getCurrentTileY() == y)
                    || (creature.getTargetTileX() == x && creature.getTargetTileY() == y)) {
                return true;
            }
        }
        return false;
    }
}
