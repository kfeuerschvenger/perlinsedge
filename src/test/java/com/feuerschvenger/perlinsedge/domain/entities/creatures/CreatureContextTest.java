package com.feuerschvenger.perlinsedge.domain.entities.creatures;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.pathfinding.CreatureOccupancy;
import com.feuerschvenger.perlinsedge.domain.strategies.SkeletonAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** The context hands strategies the map, the player and the occupancy, and nothing else. */
class CreatureContextTest {
    private static final int MAP_SIDE = 6;
    private static final double STEP = 1.0 / 60.0;
    private static final int SKELETON_TILE = 2;
    private static final int PLAYER_TILE = 3;

    private static TileMap grass() {
        final TileMap map = new TileMap(MAP_SIDE, MAP_SIDE);
        for (int x = 0; x < MAP_SIDE; x++) {
            for (int y = 0; y < MAP_SIDE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    @Test
    void theContextGivesBackTheThreeParts() {
        final TileMap map = grass();
        final Player player = new Player(PLAYER_TILE, PLAYER_TILE);
        final CreatureOccupancy occupancy = new CreatureOccupancy();

        final CreatureContext context = new CreatureContext(map, player, occupancy);

        assertSame(map, context.map());
        assertSame(player, context.player());
        assertSame(occupancy, context.occupancy());
    }

    @Test
    void anAttackStrategyReadsThePlayerFromTheContext() {
        final Skeleton skeleton = new Skeleton(SKELETON_TILE, SKELETON_TILE);
        final Player player = new Player(PLAYER_TILE, PLAYER_TILE);
        final double healthBefore = player.getHealth();

        new SkeletonAttackStrategy().updateAttack(skeleton, STEP,
                new CreatureContext(grass(), player, new CreatureOccupancy()));

        assertEquals(healthBefore - skeleton.getAttackDamage(), player.getHealth(), 0.0);
    }
}
