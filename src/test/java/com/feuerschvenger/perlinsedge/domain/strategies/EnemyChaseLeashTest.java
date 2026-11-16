package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.AppConfig.EnemyConfig.ChaseRange;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every enemy type chases inside its aggro range, gives up beyond its leash, and only ever walks on free tiles. */
class EnemyChaseLeashTest {

    private static final double STEP = 1.0 / 60.0;
    private static final ChaseRange RANGE = new ChaseRange(10, 15, 4);

    private static TileMap grass(int width, int height) {
        TileMap map = new TileMap(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    private static void run(Enemy enemy, TileMap map, Player player, double seconds) {
        for (int i = 0; i < Math.round(seconds / STEP); i++) {
            enemy.update(STEP, map, player);
        }
    }

    private static double distance(Enemy enemy, Player player) {
        return Math.hypot(player.getCurrentTileX() - enemy.getCurrentTileX(),
                player.getCurrentTileY() - enemy.getCurrentTileY());
    }

    // ------------------------------------------------------------------ the shared rules

    @Test
    void aChaseStartsOnlyWithinTheAggroRange() {
        ChaseLeash leash = new ChaseLeash(RANGE);

        assertFalse(leash.update(12, false, STEP), "between aggro and leash a calm enemy stays calm");
        assertTrue(leash.update(10, false, STEP));
    }

    @Test
    void aChaseSurvivesTheAggroEdgeUntilTheLeashOrTheLoseTime() {
        ChaseLeash leash = new ChaseLeash(RANGE);
        leash.update(5, false, STEP);

        assertTrue(leash.update(12, false, STEP), "hysteresis: outside aggro but inside the leash");
        assertFalse(leash.update(15.5, false, STEP), "beyond the leash it gives up at once");
    }

    @Test
    void aPlayerLingeringOutsideTheAggroRangeEndsTheChaseAfterTheLoseTime() {
        ChaseLeash leash = new ChaseLeash(RANGE);
        leash.update(5, false, STEP);

        assertTrue(leash.update(12, false, 3.9));
        assertFalse(leash.update(12, false, 0.2));
    }

    @Test
    void comingBackWithinTheAggroRangeResetsTheLoseTime() {
        ChaseLeash leash = new ChaseLeash(RANGE);
        leash.update(5, false, STEP);
        leash.update(12, false, 3.9);

        assertTrue(leash.update(9, false, STEP));
        assertTrue(leash.update(12, false, 3.9), "the clock started again");
    }

    @Test
    void aDeadPlayerEndsTheChase() {
        ChaseLeash leash = new ChaseLeash(RANGE);
        leash.update(5, false, STEP);

        assertFalse(leash.update(5, true, STEP));
        assertFalse(leash.isChasing());
    }

    @Test
    void theLeashMustEncloseTheAggroRange() {
        assertThrows(IllegalArgumentException.class, () -> new ChaseRange(10, 10, 1));
    }

    @Test
    void everyEnemyTypeHasALeashLargerThanItsAggroRange() {
        AppConfig.EnemyConfig config = AppConfig.getInstance().enemies();

        for (ChaseRange range : new ChaseRange[]{config.slime(), config.skeleton(), config.mimic()}) {
            assertTrue(range.leashTiles() > range.aggroTiles());
        }
    }

    // ------------------------------------------------------------------ leash, per type

    private static void givesUpWhenThePlayerIsBeyondTheLeash(Function<Integer, Enemy> spawn,
                                                             Predicate<Enemy> chasing, int startDistance) {
        TileMap map = grass(120, 20);
        Enemy enemy = spawn.apply(10);
        Player player = new Player(10 + startDistance, 10);
        run(enemy, map, player, 1.0);
        assertTrue(chasing.test(enemy), "precondition: chasing a near player");

        player.setPosition(110, 10);
        run(enemy, map, player, 3.0);

        assertFalse(chasing.test(enemy), "the player is far beyond the leash");
        double atGiveUp = distance(enemy, player);
        run(enemy, map, player, 10.0);
        assertTrue(distance(enemy, player) >= atGiveUp - 4, "it wanders at most, it does not follow");
    }

    private static void takesUpTheChaseAgainWhenThePlayerComesBack(Function<Integer, Enemy> spawn,
                                                                   Predicate<Enemy> chasing, int startDistance) {
        TileMap map = grass(120, 20);
        Enemy enemy = spawn.apply(10);
        Player player = new Player(10 + startDistance, 10);
        run(enemy, map, player, 1.0);
        player.setPosition(110, 10);
        run(enemy, map, player, 3.0);
        assertFalse(chasing.test(enemy), "precondition: gave up");

        player.setPosition(enemy.getCurrentTileX() + startDistance, enemy.getCurrentTileY());
        run(enemy, map, player, 0.2);

        assertTrue(chasing.test(enemy), "the player is within the aggro range again");
    }

    @Test
    void aSkeletonGivesUpBeyondTheLeashAndStopsOnATileCentre() {
        givesUpWhenThePlayerIsBeyondTheLeash(x -> new Skeleton(x, 10), SkeletonChase::chasing, 6);

        TileMap map = grass(120, 20);
        Skeleton skeleton = new Skeleton(10, 10);
        Player player = new Player(16, 10);
        run(skeleton, map, player, 0.3);
        player.setPosition(110, 10);
        run(skeleton, map, player, 2.0);
        assertEquals(Math.rint(skeleton.getPosX()), skeleton.getPosX(), 1e-9);
        assertEquals(Math.rint(skeleton.getPosY()), skeleton.getPosY(), 1e-9);
    }

    @Test
    void aSlimeGivesUpBeyondTheLeash() {
        givesUpWhenThePlayerIsBeyondTheLeash(x -> new Slime(x, 10), SlimeChase::chasing, 6);
    }

    @Test
    void aMimicGivesUpBeyondTheLeash() {
        givesUpWhenThePlayerIsBeyondTheLeash(x -> new Mimic(x, 10), MimicChase::chasing, 2);
    }

    @Test
    void aSkeletonTakesUpTheChaseAgain() {
        takesUpTheChaseAgainWhenThePlayerComesBack(x -> new Skeleton(x, 10), SkeletonChase::chasing, 6);
    }

    @Test
    void aSlimeTakesUpTheChaseAgain() {
        takesUpTheChaseAgainWhenThePlayerComesBack(x -> new Slime(x, 10), SlimeChase::chasing, 6);
    }

    @Test
    void aMimicTakesUpTheChaseAgain() {
        takesUpTheChaseAgainWhenThePlayerComesBack(x -> new Mimic(x, 10), MimicChase::chasing, 2);
    }

    @Test
    void aMimicHurtFromAfarWakesAndHoldsTheChaseForTheLoseTimeThenGivesUpBeyondTheLeash() {
        TileMap map = grass(120, 20);
        Mimic mimic = new Mimic(10, 10);
        Player player = new Player(16, 10);

        mimic.takeDamage(1);
        run(mimic, map, player, 0.2);
        assertTrue(MimicChase.chasing(mimic), "6 tiles is outside the wake range but inside the leash");

        player.setPosition(60, 10);
        run(mimic, map, player, 0.2);
        assertTrue(MimicChase.chasing(mimic), "the alert holds whatever the distance");

        run(mimic, map, player, AppConfig.getInstance().enemies().mimic().loseSeconds() + 0.2);
        assertFalse(MimicChase.chasing(mimic), "the player is beyond the leash once the hold is over");
    }

    @Test
    void aSlimeHurtFromAfarChasesForTheLoseTimeAndAMimicHitAgainHoldsLonger() {
        TileMap map = grass(120, 20);
        Slime slime = new Slime(10, 10);
        Player player = new Player(20, 10);

        slime.takeDamage(1);
        run(slime, map, player, 0.2);
        assertTrue(SlimeChase.chasing(slime), "10 tiles is outside the aggro range but the hit alerts it");

        player.setPosition(100, 10);
        run(slime, map, player, 0.2);
        assertTrue(SlimeChase.chasing(slime), "the alert holds whatever the distance");
        run(slime, map, player, AppConfig.getInstance().enemies().slime().loseSeconds() + 0.2);
        assertFalse(SlimeChase.chasing(slime));

        Mimic mimic = new Mimic(10, 10);
        player.setPosition(16, 10);
        mimic.takeDamage(1);
        run(mimic, map, player, 2.0);
        mimic.takeDamage(1);
        player.setPosition(100, 10);
        run(mimic, map, player, 2.0);
        assertTrue(MimicChase.chasing(mimic), "the second hit renewed the hold");
    }

    @Test
    void aSkeletonHurtFromAfarChasesAndHoldsTheChaseForTheLoseTimeThenGivesUpBeyondTheLeash() {
        TileMap map = grass(120, 20);
        Skeleton skeleton = new Skeleton(10, 10);
        Player player = new Player(22, 10);

        run(skeleton, map, player, 0.2);
        assertFalse(SkeletonChase.chasing(skeleton), "12 tiles is outside the aggro range");

        skeleton.takeDamage(1);
        run(skeleton, map, player, 0.2);
        assertTrue(SkeletonChase.chasing(skeleton), "the hit alerts it");

        player.setPosition(100, 10);
        run(skeleton, map, player, 0.2);
        assertTrue(SkeletonChase.chasing(skeleton), "the alert holds whatever the distance");

        run(skeleton, map, player, AppConfig.getInstance().enemies().skeleton().loseSeconds() + 0.2);
        assertFalse(SkeletonChase.chasing(skeleton));
    }

    @Test
    void theAlertHoldOfASlimeRunsOutWhileThePlayerIsInReach() {
        TileMap map = grass(120, 20);
        Slime slime = new Slime(10, 10);
        Player player = new Player(11, 10);
        slime.takeDamage(1);

        run(slime, map, player, AppConfig.getInstance().enemies().slime().loseSeconds() + 0.5);
        player.setPosition(100, 10);
        run(slime, map, player, 0.1);

        assertFalse(SlimeChase.chasing(slime), "the hold was spent next to the player, so the leash applies at once");
    }

    @Test
    void aSlimeNeverLandsOnTheTileThePlayerStandsOnOrIsSteppingTo() {
        TileMap map = grass(60, 20);
        Slime slime = new Slime(5, 10);
        Player player = new Player(30, 10);
        slime.takeDamage(1);
        SlimeMovementStrategy movement = (SlimeMovementStrategy) slime.getMovementStrategy();
        for (int i = 0; i < 600 && !movement.isJumping(); i++) {
            slime.update(STEP, map, player);
        }
        assertTrue(movement.isJumping(), "the alerted slime hops after the player");
        final int targetX = slime.getTargetTileX();
        final int targetY = slime.getTargetTileY();

        // The player steps onto the tile the slime is hopping to
        player.setPosition(targetX, targetY);
        for (int i = 0; i < 120 && movement.isJumping(); i++) {
            slime.update(STEP, map, player);
        }

        assertFalse(movement.isJumping());
        assertFalse(slime.getCurrentTileX() == targetX && slime.getCurrentTileY() == targetY,
                "it came down on another tile");
        assertEquals(slime.getCurrentTileX(), slime.getPosX(), 1e-9);
    }

    @Test
    void aSlimeBlockedByThePlayersStepDoesNotSearchOnEveryUpdate() {
        TileMap map = grass(60, 20);
        Slime slime = new Slime(5, 10);
        Player player = new Player(7, 10);
        player.setTargetTileX(6);
        player.setTargetTileY(10);
        slime.takeDamage(1);
        SlimeMovementStrategy movement = (SlimeMovementStrategy) slime.getMovementStrategy();

        for (int i = 0; i < 18; i++) {
            slime.update(STEP, map, player);
        }

        assertFalse(movement.isJumping());
        assertTrue(movement.pathSearchCount() <= 3, "searches: " + movement.pathSearchCount());
    }

    @Test
    void anAlertHoldsTheChaseForAtLeastTheLoseTimeWhateverTheDistance() {
        ChaseLeash leash = new ChaseLeash(RANGE);
        leash.alert();

        assertTrue(leash.update(50, false, 3.9), "beyond the leash at the moment of the alert");
        assertTrue(leash.update(50, false, 0.2), "the hold ends with this update");
        assertFalse(leash.update(50, false, STEP), "the normal leash rules apply afterwards");
    }

    @Test
    void anAlertEndsWithADeadPlayerAndAChaseStartedByAggroGetsNoHold() {
        ChaseLeash alerted = new ChaseLeash(RANGE);
        alerted.alert();
        assertFalse(alerted.update(5, true, STEP));

        ChaseLeash aggro = new ChaseLeash(RANGE);
        assertTrue(aggro.update(5, false, STEP));
        assertFalse(aggro.update(50, false, STEP), "beyond the leash it gives up at once");
    }

    // ------------------------------------------------------------------ never on a blocked tile

    /** A wall of stone at x = 8 from y = 0 to wallTop, so the way round is through the gap below it. */
    private static TileMap mapWithStoneWall(int wallTop) {
        TileMap map = grass(30, 20);
        for (int y = 0; y <= wallTop; y++) {
            map.getTile(8, y).setWorldResource(new Resource(ResourceType.STONE, 1));
        }
        return map;
    }

    private static void walksAroundTheStoneWithoutTouchingIt(Enemy enemy, Player player, double seconds, int wallTop) {
        TileMap map = mapWithStoneWall(wallTop);
        boolean crossed = false;
        for (int i = 0; i < Math.round(seconds / STEP); i++) {
            enemy.update(STEP, map, player);
            int tileX = enemy.getCurrentTileX();
            int tileY = enemy.getCurrentTileY();
            assertTrue(map.getTile(tileX, tileY).isWalkable(), "stands on a blocked tile " + tileX + "," + tileY);
            int nearX = (int) Math.round(enemy.getPosX());
            int nearY = (int) Math.round(enemy.getPosY());
            assertTrue(map.getTile(nearX, nearY).isWalkable(), "drawn over a blocked tile " + nearX + "," + nearY);
            crossed |= enemy.getCurrentTileX() > 8;
        }
        assertTrue(crossed, "it found the way round the wall");
    }

    @Test
    void aSkeletonChasingAroundAWallNeverEntersAResourceTile() {
        walksAroundTheStoneWithoutTouchingIt(new Skeleton(5, 6), new Player(11, 6), 20, 12);
    }

    @Test
    void aSlimeChasingAroundAWallNeverEntersAResourceTile() {
        walksAroundTheStoneWithoutTouchingIt(new Slime(5, 6), new Player(11, 6), 40, 12);
    }

    @Test
    void aMimicChasingAroundAWallNeverEntersAResourceTile() {
        Mimic mimic = new Mimic(5, 6);
        ((MimicMovementStrategy) mimic.getMovementStrategy()).setAggroed(true);
        // A mimic gives up after the lose time, so its detour is short
        walksAroundTheStoneWithoutTouchingIt(mimic, new Player(11, 6), 20, 6);
    }

    @Test
    void aMimicNeverCrossesWaterWhileChasing() {
        TileMap map = grass(30, 20);
        for (int y = 0; y <= 12; y++) {
            map.setTile(8, y, new Tile(8, y, TileType.DEEP_WATER));
        }
        Mimic mimic = new Mimic(5, 6);
        ((MimicMovementStrategy) mimic.getMovementStrategy()).setAggroed(true);
        Player player = new Player(11, 6);

        for (int i = 0; i < 60 * 20; i++) {
            mimic.update(STEP, map, player);
            assertTrue(map.getTile(mimic.getCurrentTileX(), mimic.getCurrentTileY()).isWalkable());
            assertTrue(mimic.getPosX() < 7.5 || mimic.getPosX() > 8.5 || mimic.getPosY() > 12.5,
                    "inside the water column at step " + i);
        }
    }

    // ------------------------------------------------------------------ accessors for the tests

    private static final class SkeletonChase {
        static boolean chasing(Enemy enemy) {
            return ((SkeletonMovementStrategy) enemy.getMovementStrategy()).isChasing();
        }
    }

    private static final class SlimeChase {
        static boolean chasing(Enemy enemy) {
            return ((SlimeMovementStrategy) enemy.getMovementStrategy()).isChasing();
        }
    }

    private static final class MimicChase {
        static boolean chasing(Enemy enemy) {
            return ((MimicMovementStrategy) enemy.getMovementStrategy()).isAggroed();
        }
    }
}
