package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.pathfinding.EnemyOccupancy;
import com.feuerschvenger.perlinsedge.domain.strategies.SkeletonMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The update of an enemy is one method, and the behaviour a subclass adds to it runs on every call. */
class EnemyUpdateHookTest {

    private static TileMap grass() {
        final TileMap map = new TileMap(10, 10);
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    @Test
    void theHookOfASkeletonRunsOnEveryUpdateThroughTheOneAndOnlyOverload() {
        final int[] calls = {0};
        final Skeleton skeleton = new Skeleton(2, 2) {
            @Override
            protected void afterUpdate(Player player) {
                calls[0]++;
                super.afterUpdate(player);
            }
        };

        for (int i = 0; i < 5; i++) {
            skeleton.update(1.0 / 60.0, grass(), new Player(8, 8), new EnemyOccupancy());
        }

        assertEquals(5, calls[0]);
    }

    @Test
    void theHookRunsAfterTheMovementOfTheSameUpdate() {
        final TileMap map = grass();
        final double[] xAtHook = {-1};
        final Skeleton skeleton = new Skeleton(2, 2) {
            @Override
            protected void afterUpdate(Player player) {
                xAtHook[0] = getPosX();
            }
        };
        final Player player = new Player(8, 2);
        ((SkeletonMovementStrategy) skeleton.getMovementStrategy()).alert();

        skeleton.update(0.25, map, player, new EnemyOccupancy());

        assertTrue(skeleton.getPosX() > 2.0, "precondition: the skeleton moved");
        assertEquals(skeleton.getPosX(), xAtHook[0], 1e-12, "the hook sees the position after the move");
    }
}
