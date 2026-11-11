package com.feuerschvenger.perlinsedge.domain.entities;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerHurtTest {

    private static final double STEP = 1.0 / 60.0;

    @Test
    void aPlayerThatWasNeverHurtHasNoHurtTime() {
        assertEquals(Double.POSITIVE_INFINITY, new Player(3, 3).getSecondsSinceHurt());
    }

    @Test
    void takingDamageRestartsTheHurtClockAndTimePassesAfterIt() {
        Player player = new Player(3, 3);

        player.takeDamage(5);
        assertEquals(0.0, player.getSecondsSinceHurt());

        player.update(0.1);
        player.update(0.05);
        assertEquals(0.15, player.getSecondsSinceHurt(), 1e-9);

        player.takeDamage(1);
        assertEquals(0.0, player.getSecondsSinceHurt());
    }

    @Test
    void noDamageLeavesTheClockAlone() {
        Player player = new Player(3, 3);
        player.takeDamage(0);
        player.takeDamage(-4);
        assertEquals(Double.POSITIVE_INFINITY, player.getSecondsSinceHurt());
    }

    @Test
    void aDeadPlayerIsNotHurtAgain() {
        Player player = new Player(3, 3);
        player.takeDamage(1_000_000);
        player.update(1.0);
        double since = player.getSecondsSinceHurt();

        player.takeDamage(5);

        assertEquals(since, player.getSecondsSinceHurt());
    }

    @Test
    void respawningForgetsTheLastHit() {
        Player player = new Player(3, 3);
        player.takeDamage(1_000_000);

        player.revive(5, 5);

        assertEquals(Double.POSITIVE_INFINITY, player.getSecondsSinceHurt());
    }

    @Test
    void anEnemyStrikeHurtsThePlayerThroughTheRealDamagePath() {
        TileMap map = new TileMap(40, 40);
        for (int x = 0; x < 40; x++) {
            for (int y = 0; y < 40; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        Skeleton skeleton = new Skeleton(5, 5);
        Player player = new Player(6, 5);
        double before = player.getHealth();

        skeleton.update(STEP, map, player);

        assertTrue(player.getHealth() < before);
        assertEquals(0.0, player.getSecondsSinceHurt());
    }
}
