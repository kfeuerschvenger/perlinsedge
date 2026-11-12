package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A slime next to the player holds its ground, squashes, strikes and waits; out of reach it hops after the player. */
class SlimeCombatTest {

    private static final double STEP = 1.0 / 60.0;

    private static TileMap grass(int size) {
        TileMap map = new TileMap(size, size);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    private static void run(Slime slime, TileMap map, Player player, double seconds) {
        for (int i = 0; i < Math.round(seconds / STEP); i++) {
            slime.update(STEP, map, player);
        }
    }

    @Test
    void aSlimeNextToThePlayerNeverHops() {
        TileMap map = grass(20);
        Slime slime = new Slime(5, 5);
        Player player = new Player(6, 5);

        for (int i = 0; i < 60 * 8; i++) {
            slime.update(STEP, map, player);
            assertEquals(5.0, slime.getPosX(), 1e-9, "moved at step " + i);
            assertEquals(5.0, slime.getPosY(), 1e-9, "moved at step " + i);
        }
    }

    @Test
    void aSlimeOnTheDiagonalCountsAsNextToThePlayer() {
        TileMap map = grass(20);
        Slime slime = new Slime(5, 5);
        Player player = new Player(6, 6);

        run(slime, map, player, 5);

        assertEquals(5.0, slime.getPosX(), 1e-9);
        assertEquals(5.0, slime.getPosY(), 1e-9);
        assertTrue(player.getHealth() < player.getMaxHealth(), "it strikes from the diagonal");
    }

    @Test
    void theStrikeComesAfterTheSquashAndNotBefore() {
        TileMap map = grass(20);
        Slime slime = new Slime(5, 5);
        Player player = new Player(6, 5);

        run(slime, map, player, 0.5);
        assertEquals(player.getMaxHealth(), player.getHealth(), "still squashing at half a second");
        assertTrue(slime.getWindupProgress() > 0.5, "the squash is well under way: " + slime.getWindupProgress());

        run(slime, map, player, 0.2);
        assertEquals(player.getMaxHealth() - slime.getAttackDamage(), player.getHealth(), 1e-9);
        assertEquals(0.0, slime.getWindupProgress(), "no squash while it waits");
    }

    @Test
    void theSecondStrikeComesAfterTheWaitAndAnotherSquash() {
        TileMap map = grass(20);
        Slime slime = new Slime(5, 5);
        Player player = new Player(6, 5);

        run(slime, map, player, 2.3);
        assertEquals(player.getMaxHealth() - slime.getAttackDamage(), player.getHealth(), 1e-9,
                "one strike after 2.3 seconds");

        run(slime, map, player, 0.6);
        assertEquals(player.getMaxHealth() - 2 * slime.getAttackDamage(), player.getHealth(), 1e-9,
                "the second strike lands about 2.1 seconds after the first");
    }

    @Test
    void aSlimeThatIsFarAwayHopsToTheTileNextToThePlayerAndStopsThere() {
        TileMap map = grass(30);
        Slime slime = new Slime(5, 5);
        Player player = new Player(11, 5);

        run(slime, map, player, 12);

        int distance = Math.max(Math.abs(slime.getCurrentTileX() - 11), Math.abs(slime.getCurrentTileY() - 5));
        assertEquals(1, distance, "it stops next to the player, not on the player's tile");
    }

    @Test
    void aPlayerThatWalksAwayIsChasedAgain() {
        TileMap map = grass(30);
        Slime slime = new Slime(5, 5);
        Player player = new Player(6, 5);
        run(slime, map, player, 1);
        assertEquals(5.0, slime.getPosX(), 1e-9);

        player.setPosition(11, 5);
        run(slime, map, player, 3);

        assertTrue(slime.getCurrentTileX() > 5 || slime.getPosX() > 5.0, "the slime hops after the player");
    }

    @Test
    void aPlayerThatLeavesDuringTheSquashCancelsTheStrike() {
        TileMap map = grass(30);
        Slime slime = new Slime(5, 5);
        Player player = new Player(6, 5);
        run(slime, map, player, 0.4);
        assertTrue(slime.getWindupProgress() > 0);

        player.setPosition(12, 5);
        slime.update(STEP, map, player);

        assertEquals(0.0, slime.getWindupProgress());
        assertFalse(player.getHealth() < player.getMaxHealth());
    }

    @Test
    void aDeadPlayerIsNotStruck() {
        TileMap map = grass(20);
        Slime slime = new Slime(5, 5);
        Player player = new Player(6, 5);
        player.takeDamage(player.getMaxHealth() * 2);

        run(slime, map, player, 5);

        assertEquals(0.0, slime.getWindupProgress());
    }
}
