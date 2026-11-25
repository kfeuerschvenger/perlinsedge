package com.feuerschvenger.perlinsedge.domain.entities.creatures;

import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.pathfinding.CreatureOccupancy;
import com.feuerschvenger.perlinsedge.domain.strategies.CreatureAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.CreatureMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The lifecycle and the hurt hook that every creature shares, whatever its branch. */
class CreatureTest {
    private static final int MAP_SIDE = 10;
    private static final double STEP = 1.0 / 60.0;
    private static final double MAX_HEALTH = 20.0;
    private static final double COOLDOWN = 1.5;
    private static final double SMALL_HIT = 5.0;
    private static final double KILLING_HIT = 100.0;
    private static final int START_TILE = 3;
    private static final int PLAYER_TILE = 8;

    /** A creature of the animal branch, which has no species of its own yet, with hooks that record what runs. */
    private static final class TestAnimal extends Animal {
        private final List<String> log;
        private final List<Entity> hurtBy = new ArrayList<>();

        private TestAnimal(List<String> log, CreatureMovementStrategy movement, CreatureAttackStrategy attack) {
            super(START_TILE, START_TILE, MAX_HEALTH, SMALL_HIT, COOLDOWN, movement, attack);
            this.log = log;
        }

        @Override
        protected void afterUpdate(Player player) {
            log.add("hook");
        }

        @Override
        protected void onHurt(Entity source) {
            log.add("hurt");
            hurtBy.add(source);
        }

        @Override
        public Species species() {
            return Species.SLIME;
        }

        @Override
        public double getRenderOffsetY() {
            return 0;
        }
    }

    /** An attack that records the context it is given and never attacks. */
    private static final class RecordingAttack implements CreatureAttackStrategy {
        private final List<String> log;
        private CreatureContext seen;

        private RecordingAttack(List<String> log) {
            this.log = log;
        }

        @Override
        public void updateAttack(Creature self, double deltaTime, CreatureContext context) {
            log.add("attack");
            seen = context;
        }

        @Override
        public boolean canAttack(Creature creature, Player player) {
            return false;
        }
    }

    private final List<String> log = new ArrayList<>();
    private final RecordingAttack attack = new RecordingAttack(log);
    private final TestAnimal animal = new TestAnimal(log, (creature, dt, map, player, occupancy) -> log.add("move"),
            attack);

    private static TileMap grass() {
        final TileMap map = new TileMap(MAP_SIDE, MAP_SIDE);
        for (int x = 0; x < MAP_SIDE; x++) {
            for (int y = 0; y < MAP_SIDE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    private static CreatureContext context() {
        return new CreatureContext(grass(), new Player(PLAYER_TILE, PLAYER_TILE), new CreatureOccupancy());
    }

    @Test
    void anUpdateRunsTheMovementThenTheAttackThenTheHook() {
        animal.update(STEP, context());

        assertEquals(List.of("move", "attack", "hook"), log);
    }

    @Test
    void theAttackStrategyIsHandedTheContextOfTheUpdate() {
        final CreatureContext context = context();

        animal.update(STEP, context);

        assertSame(context, attack.seen);
    }

    @Test
    void anUpdateKeepsTheOccupancyInStepWithTheCreature() {
        final CreatureContext context = context();

        animal.update(STEP, context);

        assertSame(animal, context.occupancy().ownerOf(START_TILE, START_TILE));
    }

    @Test
    void theAttackCooldownCountsDownWithEveryUpdateAndStopsAtZero() {
        animal.resetAttackCooldown();
        assertFalse(animal.isReadyToAttack());

        animal.update(COOLDOWN / 2, context());
        assertEquals(COOLDOWN / 2, animal.getAttackTimer(), 1e-9);

        animal.update(COOLDOWN, context());
        assertEquals(0.0, animal.getAttackTimer(), 0.0);
        assertTrue(animal.isReadyToAttack());
    }

    @Test
    void aLivingCreatureIsToldItWasHurtBeforeTheDamageLandsAndWho() {
        final Player player = new Player(PLAYER_TILE, PLAYER_TILE);

        animal.takeDamage(SMALL_HIT, player);

        assertEquals(List.of("hurt"), log);
        assertSame(player, animal.hurtBy.get(0));
        assertEquals(MAX_HEALTH - SMALL_HIT, animal.getHealth(), 0.0);
    }

    @Test
    void aDamageFromNoKnownSourceHurtsWithANullSource() {
        animal.takeDamage(SMALL_HIT);

        assertEquals(1, animal.hurtBy.size());
        assertNull(animal.hurtBy.get(0));
    }

    @Test
    void aHitThatKillsStillCallsTheHookOnceAndADeadCreatureIsNotHurtAgain() {
        animal.takeDamage(KILLING_HIT);
        animal.takeDamage(SMALL_HIT);

        assertTrue(animal.isDead());
        assertEquals(List.of("hurt"), log);
    }

    @Test
    void aMonsterIsHostileAndAnAnimalIsNot() {
        assertTrue(new Slime(START_TILE, START_TILE).isHostile());
        assertFalse(animal.isHostile());
    }
}
