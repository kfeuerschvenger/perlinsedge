package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Works out which tiles the living enemies take up, so the player's walks neither cross them nor end on them.
 * An enemy takes up the tile it stands on and the tile it is stepping to.
 */
public final class OccupiedTiles {

    /** Not instantiable: the class only holds static rules. */
    private OccupiedTiles() {}

    /**
     * Collects the tiles taken by enemies.
     *
     * @param enemies the enemies, null for none
     * @return the tiles the living ones stand on or step to, never null
     */
    public static Set<PathNode> of(Collection<? extends Enemy> enemies) {
        final Set<PathNode> tiles = new HashSet<>();
        if (enemies == null) {
            return tiles;
        }
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) {
                continue;
            }
            tiles.add(new PathNode(enemy.getCurrentTileX(), enemy.getCurrentTileY()));
            tiles.add(new PathNode(enemy.getTargetTileX(), enemy.getTargetTileY()));
        }
        return tiles;
    }
}
