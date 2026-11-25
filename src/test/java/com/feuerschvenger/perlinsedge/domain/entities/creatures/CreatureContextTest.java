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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Strategies and the creature read the player from the context they are given, not from anywhere else. */
class CreatureContextTest {
    private static final int MAP_SIDE = 12;
    private static final double STEP = 1.0 / 60.0;
    private static final int SKELETON_TILE = 2;
    private static final int ADJACENT_TILE = 3;
    private static final int FAR_TILE = 10;

    private static TileMap grass() {
        final TileMap map = new TileMap(MAP_SIDE, MAP_SIDE);
        for (int x = 0; x < MAP_SIDE; x++) {
            for (int y = 0; y < MAP_SIDE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    private static CreatureContext contextWithPlayerAt(int tile) {
        return new CreatureContext(grass(), new Player(tile, tile), new CreatureOccupancy());
    }

    @Test
    void anAttackStrikesTheAdjacentPlayerOfTheContextAndStartsTheCooldown() {
        final Skeleton skeleton = new Skeleton(SKELETON_TILE, SKELETON_TILE);
        final CreatureContext context = contextWithPlayerAt(ADJACENT_TILE);
        final double healthBefore = context.player().getHealth();

        new SkeletonAttackStrategy().updateAttack(skeleton, STEP, context);

        assertEquals(healthBefore - skeleton.getAttackDamage(), context.player().getHealth(), 0.0);
        assertFalse(skeleton.isReadyToAttack());
    }

    @Test
    void anAttackLeavesAFarPlayerOfTheContextAlone() {
        final Skeleton skeleton = new Skeleton(SKELETON_TILE, SKELETON_TILE);
        final CreatureContext context = contextWithPlayerAt(FAR_TILE);
        final double healthBefore = context.player().getHealth();

        new SkeletonAttackStrategy().updateAttack(skeleton, STEP, context);

        assertEquals(healthBefore, context.player().getHealth(), 0.0);
        assertTrue(skeleton.isReadyToAttack());
    }

    @Test
    void aCreatureAsksItsAttackWhetherItCanAttackTheContextItIsGiven() {
        final Skeleton skeleton = new Skeleton(SKELETON_TILE, SKELETON_TILE);

        assertTrue(skeleton.canAttack(contextWithPlayerAt(ADJACENT_TILE)));
        assertFalse(skeleton.canAttack(contextWithPlayerAt(FAR_TILE)));
    }
}
