package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.pathfinding.OccupiedTiles;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The walking chase follows a moving player and recovers when the tile it walks to becomes blocked. */
class WalkingChaserTest {

    private static final double STEP = 1.0 / 60.0;

    private static TileMap grass(int width, int height) {
        TileMap map = new TileMap(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    private static void run(Skeleton enemy, TileMap map, Player player, double seconds) {
        for (int i = 0; i < Math.round(seconds / STEP); i++) {
            enemy.update(STEP, map, player);
        }
    }

    private static boolean isNextTo(Skeleton enemy, Player player) {
        return Math.max(Math.abs(enemy.getCurrentTileX() - player.getCurrentTileX()),
                Math.abs(enemy.getCurrentTileY() - player.getCurrentTileY())) <= 1;
    }

    @Test
    void aTileBlockedUnderAStepInProgressSendsTheEnemyBackAndTheChaseResumes() {
        TileMap map = grass(30, 20);
        Skeleton skeleton = new Skeleton(5, 5);
        Player player = new Player(13, 5);
        run(skeleton, map, player, 0.1);
        assertTrue(skeleton.getPosX() > 5.0 && skeleton.getPosX() < 6.0, "precondition: between two tiles");
        assertEquals(6, skeleton.getTargetTileX(), "precondition: walking to the tile that gets blocked");

        map.getTile(6, 5).setWorldResource(new Resource(ResourceType.STONE, 1));
        skeleton.update(STEP, map, player);

        assertFalse(OccupiedTiles.of(List.of(skeleton)).contains(new PathNode(6, 5)),
                "the blocked tile is no longer reported as the one it steps to");

        boolean wasOnATileCentre = false;
        for (int i = 0; i < 60 * 5; i++) {
            skeleton.update(STEP, map, player);
            assertFalse(Math.round(skeleton.getPosX()) == 6 && Math.round(skeleton.getPosY()) == 5,
                    "drawn over the blocked tile");
            wasOnATileCentre |= skeleton.getPosX() == skeleton.getCurrentTileX()
                    && skeleton.getPosY() == skeleton.getCurrentTileY();
        }
        assertTrue(wasOnATileCentre, "it came back to a tile centre");
        assertTrue(skeleton.getCurrentTileX() >= 8, "and went on towards the player: " + skeleton.getCurrentTileX());
    }

    @Test
    void aTileBlockedAheadOfAnEnemyOnATileCentreSendsItAroundIt() {
        TileMap map = grass(30, 20);
        Skeleton skeleton = new Skeleton(5, 5);
        Player player = new Player(13, 5);
        map.getTile(6, 5).setWorldResource(new Resource(ResourceType.STONE, 1));

        run(skeleton, map, player, 4.0);

        assertTrue(skeleton.getCurrentTileX() >= 8);
    }

    @Test
    void anEnemyNeverEntersTheTileThePlayerIsSteppingTo() {
        TileMap map = grass(30, 20);
        Skeleton skeleton = new Skeleton(8, 10);
        Player player = new Player(10, 10);
        assertTrue(player.walkAlong(List.of(new PathNode(9, 10))), "precondition: the player steps toward the enemy");

        for (int i = 0; i < 60; i++) {
            skeleton.update(STEP, map, player);
            assertEquals(8.0, skeleton.getPosX(), 1e-9, "stepped toward the player's tile at update " + i);
            assertEquals(8, skeleton.getTargetTileX());
        }
    }

    @Test
    void aPlayerChangingTileSoonAfterASearchDoesNotStartAnotherOneBeforeTheSpacingHasPassed() {
        TileMap map = grass(40, 30);
        Skeleton skeleton = new Skeleton(10, 10);
        Player player = new Player(20, 10);
        run(skeleton, map, player, 0.2);

        player.setPosition(10, 17);
        run(skeleton, map, player, 0.15);
        assertEquals(10, skeleton.getTargetTileY(), "still on the path to the old tile, no new search yet");

        run(skeleton, map, player, 1.0);
        assertTrue(skeleton.getTargetTileY() > 10, "the new position is followed once the spacing has passed");
    }

    @Test
    void aPlayerThatMovesMidChaseIsFollowedWithoutWaitingForTheOldPathToEnd() {
        TileMap map = grass(40, 30);
        Skeleton skeleton = new Skeleton(10, 10);
        Player player = new Player(10, 3);
        run(skeleton, map, player, 0.4);

        player.setPosition(17, 10);
        run(skeleton, map, player, 3.0);

        assertTrue(isNextTo(skeleton, player),
                "the skeleton is at " + skeleton.getCurrentTileX() + "," + skeleton.getCurrentTileY());
    }
}
