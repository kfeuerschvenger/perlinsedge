package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.pathfinding.EnemyOccupancy;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The motion of a slime hop: continuous in position and phase, bounded per tick, and never landing on the player. */
class SlimeHopTest {

    private static final double STEP = 1.0 / 60.0;
    /** Duration of a hop along a path to the player, in seconds. */
    private static final double HOP_SECONDS = 0.5;
    /** Largest distance a tick may move the slime: the peak of a smoothstep over a diagonal hop is 1.5 times its mean. */
    private static final double MAX_TICK_DISTANCE = 0.075;
    private static final int TAKE_OFF_X = 10;
    private static final int TAKE_OFF_Y = 10;

    /** A chasing slime with the player a few tiles away, on open grass. */
    private static final class Scene {
        final TileMap map = new TileMap(60, 20);
        final Slime slime = new Slime(TAKE_OFF_X, TAKE_OFF_Y);
        final Player player = new Player(16, 10);
        final EnemyOccupancy occupancy = new EnemyOccupancy();
        final SlimeMovementStrategy movement = (SlimeMovementStrategy) slime.getMovementStrategy();
        double maxTick;
        double maxPhaseTick;

        Scene() {
            for (int x = 0; x < 60; x++) {
                for (int y = 0; y < 20; y++) {
                    map.setTile(x, y, new Tile(x, y, TileType.GRASS));
                }
            }
        }

        /** Advances one update and records the largest position and phase change seen so far. */
        void tick() {
            final double x = slime.getPosX();
            final double y = slime.getPosY();
            final double phase = movement.getJumpPhase();
            slime.update(STEP, map, player, occupancy);
            maxTick = Math.max(maxTick, Math.hypot(slime.getPosX() - x, slime.getPosY() - y));
            // The landing resets the phase to the ground value, which is not a step of the arc
            if (movement.isJumping()) {
                maxPhaseTick = Math.max(maxPhaseTick, Math.abs(movement.getJumpPhase() - phase));
            }
        }

        /** Advances until the slime is in the air, on the update that takes off, before the position moves. */
        void takeOff() {
            for (int i = 0; i < 600 && !movement.isJumping(); i++) {
                tick();
            }
            assertTrue(movement.isJumping(), "precondition: in the air");
        }

        void tick(int count) {
            for (int i = 0; i < count; i++) {
                tick();
            }
        }

        /** Advances until the slime lands, and fails when it never does. */
        void land() {
            for (int i = 0; i < 300 && movement.isJumping(); i++) {
                tick();
            }
            assertFalse(movement.isJumping(), "the slime did not land");
        }

        boolean onPlayerTile() {
            return slime.getCurrentTileX() == player.getCurrentTileX() && slime.getCurrentTileY() == player.getCurrentTileY();
        }

        void assertLandedCleanly(int playerX, int playerY) {
            assertFalse(slime.getCurrentTileX() == playerX && slime.getCurrentTileY() == playerY,
                    "landed on the tile of the player");
            assertTrue(Math.abs(slime.getCurrentTileX() - TAKE_OFF_X) <= 1
                    && Math.abs(slime.getCurrentTileY() - TAKE_OFF_Y) <= 1, "landed more than a tile from take-off");
            assertTrue(map.getTile(slime.getCurrentTileX(), slime.getCurrentTileY()).isWalkable());
            assertEquals(slime.getCurrentTileX(), slime.getPosX(), 1e-9);
            assertEquals(slime.getCurrentTileY(), slime.getPosY(), 1e-9);
        }
    }

    @Test
    void aNormalHopMovesSmoothlyForItsWholeDurationAndLandsOnItsTarget() {
        Scene scene = new Scene();
        scene.takeOff();
        final int targetX = scene.slime.getTargetTileX();
        final int targetY = scene.slime.getTargetTileY();
        final double length = Math.hypot(targetX - TAKE_OFF_X, targetY - TAKE_OFF_Y);
        double previousToTarget = length;
        int ticks = 0;
        double halfwayRemaining = -1;

        while (scene.movement.isJumping() && ticks < 300) {
            scene.tick();
            ticks++;
            final double toTarget = Math.hypot(targetX - scene.slime.getPosX(), targetY - scene.slime.getPosY());
            assertTrue(toTarget <= previousToTarget + 1e-9, "moved away from the target");
            previousToTarget = toTarget;
            if (ticks == Math.round(HOP_SECONDS / STEP / 2)) {
                halfwayRemaining = toTarget;
            }
        }

        assertEquals(HOP_SECONDS / STEP, ticks, 2, "ticks in the air");
        assertEquals(targetX, scene.slime.getCurrentTileX());
        assertEquals(targetY, scene.slime.getCurrentTileY());
        assertTrue(scene.maxTick <= 2 * length / HOP_SECONDS * STEP, "largest step " + scene.maxTick);
        assertTrue(scene.maxTick <= MAX_TICK_DISTANCE, "largest step " + scene.maxTick);
        // A smoothstep is at the middle of the way at half the time, and does not wait at the target
        assertEquals(length / 2, halfwayRemaining, 0.05 * length);
    }

    @Test
    void aTargetTakenByThePlayerAtAnyMomentOfTheHopBendsWithoutAJumpAndNeverLandsOnThePlayer() {
        for (double fraction : new double[]{0.1, 0.4, 0.7, 0.95}) {
            Scene scene = new Scene();
            scene.takeOff();
            final int targetX = scene.slime.getTargetTileX();
            final int targetY = scene.slime.getTargetTileY();
            scene.tick((int) Math.round(fraction * HOP_SECONDS / STEP));
            assertTrue(scene.movement.isJumping(), "precondition: still in the air at " + fraction);

            scene.player.setPosition(targetX, targetY);
            scene.land();

            assertTrue(scene.maxTick <= MAX_TICK_DISTANCE, "largest step " + scene.maxTick + " at " + fraction);
            assertTrue(scene.maxPhaseTick <= 0.12, "largest phase change " + scene.maxPhaseTick + " at " + fraction);
            scene.assertLandedCleanly(targetX, targetY);
        }
    }

    @Test
    void theHopAfterABendStartsAdjacentToTheLandingTile() {
        for (double fraction : new double[]{0.1, 0.4, 0.7, 0.95}) {
            Scene scene = new Scene();
            scene.takeOff();
            final int targetX = scene.slime.getTargetTileX();
            final int targetY = scene.slime.getTargetTileY();
            scene.tick((int) Math.round(fraction * HOP_SECONDS / STEP));
            scene.player.setPosition(targetX, targetY);
            scene.tick();
            scene.player.setPosition(16, 10);
            scene.land();

            for (int i = 0; i < 300 && !scene.movement.isJumping(); i++) {
                scene.tick();
            }

            assertTrue(scene.movement.isJumping(), "the chase goes on at " + fraction);
            assertTrue(Math.abs(scene.slime.getTargetTileX() - scene.slime.getCurrentTileX()) <= 1
                            && Math.abs(scene.slime.getTargetTileY() - scene.slime.getCurrentTileY()) <= 1,
                    "the next hop is one tile long at " + fraction);
        }
    }

    /** Water on every tile within two of the take-off tile except the take-off tile and the given one. */
    private static void floodAroundTakeOff(TileMap map, int keepX, int keepY) {
        for (int x = TAKE_OFF_X - 2; x <= TAKE_OFF_X + 2; x++) {
            for (int y = TAKE_OFF_Y - 2; y <= TAKE_OFF_Y + 2; y++) {
                final boolean takeOff = x == TAKE_OFF_X && y == TAKE_OFF_Y;
                if (!takeOff && !(x == keepX && y == keepY)) {
                    map.setTile(x, y, new Tile(x, y, TileType.DEEP_WATER));
                }
            }
        }
    }

    @Test
    void aReturnHopNeverLandsOnThePlayerWhoStepsOntoTheTakeOffTile() {
        Scene scene = new Scene();
        scene.takeOff();
        final int targetX = scene.slime.getTargetTileX();
        final int targetY = scene.slime.getTargetTileY();
        floodAroundTakeOff(scene.map, targetX, targetY);
        scene.player.setPosition(targetX, targetY);
        scene.tick(3);
        assertEquals(TAKE_OFF_X, scene.slime.getTargetTileX(), "the only free tile is the take-off tile");
        assertEquals(TAKE_OFF_Y, scene.slime.getTargetTileY());

        scene.player.setPosition(TAKE_OFF_X, TAKE_OFF_Y);
        scene.tick(9);
        assertTrue(scene.movement.isJumping());
        scene.land();

        assertFalse(scene.onPlayerTile(), "landed on the player");
        assertEquals(targetX, scene.slime.getCurrentTileX());
        assertEquals(targetY, scene.slime.getCurrentTileY());
        assertTrue(scene.maxTick <= MAX_TICK_DISTANCE, "largest step " + scene.maxTick);
    }

    @Test
    void withNoFreeTileTheSlimeHangsInTheAirAndLandsWhenOneFreesUp() {
        Scene scene = new Scene();
        scene.takeOff();
        final int targetX = scene.slime.getTargetTileX();
        final int targetY = scene.slime.getTargetTileY();
        floodAroundTakeOff(scene.map, targetX, targetY);
        scene.tick(10);

        scene.player.setPosition(targetX, targetY);
        scene.player.setTargetTileX(TAKE_OFF_X);
        scene.player.setTargetTileY(TAKE_OFF_Y);
        scene.tick(2);
        assertTrue(scene.movement.isHovering());
        final double x = scene.slime.getPosX();
        final double y = scene.slime.getPosY();
        final double phase = scene.movement.getJumpPhase();
        scene.tick(120);

        assertTrue(scene.movement.isJumping(), "it does not land on a tile of the player");
        assertEquals(x, scene.slime.getPosX(), 1e-12);
        assertEquals(y, scene.slime.getPosY(), 1e-12);
        assertEquals(phase, scene.movement.getJumpPhase(), 1e-12);

        scene.player.setPosition(16, 10);
        scene.land();

        assertFalse(scene.movement.isHovering());
        assertTrue(scene.maxTick <= MAX_TICK_DISTANCE, "largest step " + scene.maxTick);
        scene.assertLandedCleanly(16, 10);
    }

    @Test
    void aTakeOffTileBuiltOverInTheAirIsNotLandedOn() {
        Scene scene = new Scene();
        scene.takeOff();
        final int targetX = scene.slime.getTargetTileX();
        final int targetY = scene.slime.getTargetTileY();
        scene.tick(8);

        scene.map.getTile(TAKE_OFF_X, TAKE_OFF_Y).setWorldResource(new Resource(ResourceType.STONE, 1));
        scene.player.setPosition(targetX, targetY);
        scene.land();

        assertTrue(scene.maxTick <= MAX_TICK_DISTANCE, "largest step " + scene.maxTick);
        assertFalse(scene.slime.getCurrentTileX() == TAKE_OFF_X && scene.slime.getCurrentTileY() == TAKE_OFF_Y,
                "landed on the built tile");
        scene.assertLandedCleanly(targetX, targetY);
    }

    @Test
    void losingTheChaseInTheAirDoesNotChangeTheDurationOfTheHop() {
        Scene scene = new Scene();
        scene.takeOff();
        assertEquals(HOP_SECONDS, scene.movement.hopDuration());
        scene.tick(5);

        scene.player.setPosition(59, 19);
        int airborne = 5;
        while (scene.movement.isJumping() && airborne < 300) {
            scene.tick();
            airborne++;
        }

        assertFalse(scene.movement.isChasing(), "precondition: the chase is lost");
        assertEquals(HOP_SECONDS, scene.movement.hopDuration());
        assertEquals(HOP_SECONDS / STEP, airborne, 2, "ticks in the air");
    }

    @Test
    void noPathIsSearchedWhileTheSlimeIsInTheAir() {
        Scene scene = new Scene();
        scene.takeOff();
        final int targetX = scene.slime.getTargetTileX();
        final int targetY = scene.slime.getTargetTileY();
        // A bend late in the hop sends the slime back across most of a tile and drops the path, so the long stretch of
        // air that follows has an empty path that a search would be tempted to fill
        scene.tick(25);
        scene.player.setPosition(targetX, targetY);
        scene.tick();
        scene.player.setPosition(16, 10);
        final int searches = scene.movement.pathSearchCount();

        int airborne = 0;
        while (scene.movement.isJumping() && airborne < 300) {
            scene.tick();
            if (scene.movement.isJumping()) {
                assertEquals(searches, scene.movement.pathSearchCount(), "searched in the air at tick " + airborne);
            }
            airborne++;
        }
        assertTrue(airborne >= 20, "the hop lasted " + airborne + " ticks");

        scene.tick(5);
        assertTrue(scene.movement.pathSearchCount() > searches, "on the ground the empty path is searched again");
    }

    @Test
    void thePathIsKeptBetweenHopsWhileThePlayerStaysPut() {
        Scene scene = new Scene();

        for (int i = 0; i < 60 * 3; i++) {
            scene.tick();
        }

        assertTrue(scene.slime.getCurrentTileX() >= TAKE_OFF_X + 3, "the slime made its way along the path");
        assertEquals(1, scene.movement.pathSearchCount(), "one search served every hop");
    }
}
