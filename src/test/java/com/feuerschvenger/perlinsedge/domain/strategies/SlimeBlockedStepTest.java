package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;
import com.feuerschvenger.perlinsedge.domain.pathfinding.CreatureOccupancy;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A slime whose next step is held by another enemy waits for it with its path kept, and backs off while it stays held. */
class SlimeBlockedStepTest {

    private static final double STEP = 1.0 / 60.0;

    /** A corridor one tile wide, a chasing slime at its start, a motionless skeleton right ahead and the player far on. */
    private static final class Corridor {
        final TileMap map = new TileMap(30, 1);
        final CreatureOccupancy occupancy = new CreatureOccupancy();
        final Slime slime = new Slime(5, 0);
        final Skeleton blocker = new Skeleton(6, 0);
        final Player player = new Player(13, 0);
        final SlimeMovementStrategy movement = (SlimeMovementStrategy) slime.getMovementStrategy();

        Corridor() {
            for (int x = 0; x < 30; x++) {
                map.setTile(x, 0, new Tile(x, 0, TileType.GRASS));
            }
            occupancy.sync(slime);
            occupancy.sync(blocker);
            movement.alert();
        }

        void run(double seconds) {
            for (int i = 0; i < Math.round(seconds / STEP); i++) {
                player.heal(1_000);
                slime.update(STEP, new CreatureContext(map, player, occupancy));
            }
        }
    }

    @Test
    void aHeldStepKeepsThePathAndIsNotSearchedAroundWhileTheFirstWaitRuns() {
        final Corridor corridor = new Corridor();

        corridor.run(0.1);

        assertEquals(1, corridor.movement.pathSearchCount(), "one search found the path");
        assertTrue(corridor.movement.pathLength() > 0, "the path is kept while the step is held");
    }

    @Test
    void aStepThatStaysHeldIsSearchedAroundFarLessOftenThanOncePerShortestWait() {
        final Corridor corridor = new Corridor();

        corridor.run(10);

        assertEquals(5, corridor.slime.getCurrentTileX(), "the held tile is never entered");
        // Waits of 0.15 s to 0.45 s without a backoff would search about three times a second
        assertTrue(corridor.movement.pathSearchCount() <= 10,
                "searched " + corridor.movement.pathSearchCount() + " times in ten seconds");
    }

    @Test
    void theChaseResumesOnceTheHeldStepIsLetGo() {
        final Corridor corridor = new Corridor();
        corridor.run(3);
        assertEquals(5, corridor.slime.getCurrentTileX(), "precondition: held back");

        corridor.occupancy.release(corridor.blocker);
        corridor.run(10);

        assertTrue(corridor.slime.getCurrentTileX() > 5, "the slime went on: " + corridor.slime.getCurrentTileX());
    }
}
