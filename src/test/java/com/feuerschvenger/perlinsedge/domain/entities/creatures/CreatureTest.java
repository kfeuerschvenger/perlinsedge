package com.feuerschvenger.perlinsedge.domain.entities.creatures;

import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.pathfinding.CreatureOccupancy;
import com.feuerschvenger.perlinsedge.domain.strategies.CreatureAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.CreatureMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.NoAttack;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The lifecycle and the hurt reaction that every creature shares, whatever its branch. */
class CreatureTest {
    private static final int MAP_SIDE = 10;
    private static final double STEP = 1.0 / 60.0;
    private static final double MAX_HEALTH = 20.0;
    private static final double COOLDOWN = 1.5;
    private static final double SMALL_HIT = 5.0;
    private static final double KILLING_HIT = 100.0;
    private static final double EPSILON = 1e-9;
    private static final int START_TILE = 3;
    private static final int PLAYER_TILE = 8;
    private static final int NEXT_TO_ANIMAL = 4;

    /** A monster with no species, with a hook that records that it ran. */
    private static final class TestEnemy extends Enemy {
        private final List<String> log;

        private TestEnemy(List<String> log, CreatureMovementStrategy movement, CreatureAttackStrategy attack) {
            super(START_TILE, START_TILE, MAX_HEALTH, SMALL_HIT, COOLDOWN, movement, attack);
            this.log = log;
        }

        @Override
        protected void afterUpdate(Player player) {
            log.add("hook");
        }

        @Override
        public Species species() {
            throw new UnsupportedOperationException("a test creature is not a species the game builds");
        }

        @Override
        public double getRenderOffsetY() {
            return 0;
        }
    }

    /** An animal with no species, because the real animal species come with their own unit. */
    private static final class TestAnimal extends Animal {
        private TestAnimal(CreatureMovementStrategy movement) {
            super(START_TILE, START_TILE, MAX_HEALTH, movement);
        }

        @Override
        public Species species() {
            throw new UnsupportedOperationException("a test creature is not a species the game builds");
        }

        @Override
        public double getRenderOffsetY() {
            return 0;
        }
    }

    /** A movement that records that it ran and every hurt it was told about, with the health at that moment. */
    private static final class RecordingMovement implements CreatureMovementStrategy {
        private final List<String> log;
        private final List<Entity> hurtBy = new ArrayList<>();
        private Creature hurtCreature;
        private double healthWhenHurt = Double.NaN;

        private RecordingMovement(List<String> log) {
            this.log = log;
        }

        @Override
        public void updateMovement(Creature self, double deltaTime, CreatureContext context) {
            log.add("move");
        }

        @Override
        public void onHurt(Creature self, Entity source) {
            log.add("hurt");
            hurtBy.add(source);
            hurtCreature = self;
            healthWhenHurt = self.getHealth();
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
        public boolean canAttack(Creature self, CreatureContext context) {
            return false;
        }
    }

    private final List<String> log = new ArrayList<>();
    private final RecordingAttack attack = new RecordingAttack(log);
    private final RecordingMovement movement = new RecordingMovement(log);
    private final TestEnemy enemy = new TestEnemy(log, movement, attack);

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
        enemy.update(STEP, context());

        assertEquals(List.of("move", "attack", "hook"), log);
    }

    @Test
    void theAttackStrategyIsHandedTheContextOfTheUpdate() {
        final CreatureContext context = context();

        enemy.update(STEP, context);

        assertSame(context, attack.seen);
    }

    @Test
    void anUpdateKeepsTheOccupancyInStepWithTheCreature() {
        final CreatureContext context = context();

        enemy.update(STEP, context);

        assertSame(enemy, context.occupancy().ownerOf(START_TILE, START_TILE));
    }

    @Test
    void theAttackCooldownCountsDownWithEveryUpdateAndStopsAtZero() {
        enemy.resetAttackCooldown();
        assertFalse(enemy.isReadyToAttack());

        enemy.update(COOLDOWN / 2, context());
        assertEquals(COOLDOWN / 2, enemy.getAttackTimer(), EPSILON);

        enemy.update(COOLDOWN, context());
        assertEquals(0.0, enemy.getAttackTimer(), 0.0);
        assertTrue(enemy.isReadyToAttack());
    }

    @Test
    void aLivingCreatureIsToldItsMovementWasHurtBeforeTheDamageLandsAndByWhom() {
        final Player player = new Player(PLAYER_TILE, PLAYER_TILE);

        enemy.takeDamage(SMALL_HIT, player);

        assertEquals(List.of("hurt"), log);
        assertSame(player, movement.hurtBy.get(0));
        assertSame(enemy, movement.hurtCreature);
        assertEquals(MAX_HEALTH, movement.healthWhenHurt, 0.0);
        assertEquals(MAX_HEALTH - SMALL_HIT, enemy.getHealth(), 0.0);
    }

    @Test
    void aDamageFromNoKnownSourceHurtsWithANullSource() {
        enemy.takeDamage(SMALL_HIT);

        assertEquals(1, movement.hurtBy.size());
        assertNull(movement.hurtBy.get(0));
        assertEquals(MAX_HEALTH - SMALL_HIT, enemy.getHealth(), 0.0);
    }

    @Test
    void aHitThatKillsStillReactsOnceAndADeadCreatureIsNotHurtAgain() {
        enemy.takeDamage(KILLING_HIT);
        enemy.takeDamage(SMALL_HIT);

        assertTrue(enemy.isDead());
        assertEquals(List.of("hurt"), log);
    }

    @Test
    void aCreatureThatIsAlreadyDeadDoesNotReactToAHit() {
        enemy.handleDeath();

        enemy.takeDamage(SMALL_HIT, new Player(PLAYER_TILE, PLAYER_TILE));

        assertTrue(log.isEmpty());
    }

    @Test
    void noSubclassCanSkipTheReactionBecauseBothWaysOfTakingDamageAreFinal() throws NoSuchMethodException {
        assertTrue(Modifier.isFinal(
                Creature.class.getMethod("takeDamage", double.class, Entity.class).getModifiers()));
        assertTrue(Modifier.isFinal(Creature.class.getMethod("takeDamage", double.class).getModifiers()));
    }

    @Test
    void aMonsterIsHostileAndAnAnimalIsNot() {
        assertTrue(new Slime(START_TILE, START_TILE).isHostile());
        assertFalse(new TestAnimal(movement).isHostile());
    }

    @Test
    void anAnimalHasNoAttackNumbersAndNeverAttacksEvenNextToThePlayer() {
        final TestAnimal animal = new TestAnimal(movement);
        final Player player = new Player(NEXT_TO_ANIMAL, NEXT_TO_ANIMAL);
        final CreatureContext context = new CreatureContext(grass(), player, new CreatureOccupancy());
        final double healthBefore = player.getHealth();

        animal.update(STEP, context);

        assertSame(NoAttack.INSTANCE, animal.getAttackStrategy());
        assertEquals(0.0, animal.getAttackDamage(), 0.0);
        assertEquals(0.0, animal.getAttackCooldown(), 0.0);
        assertFalse(animal.canAttack(context));
        assertEquals(healthBefore, player.getHealth(), 0.0);
    }

    @Test
    void anAnimalReactsToAHitThroughItsMovementToo() {
        final TestAnimal animal = new TestAnimal(movement);
        final Player player = new Player(PLAYER_TILE, PLAYER_TILE);

        animal.takeDamage(SMALL_HIT, player);

        assertEquals(List.of("hurt"), log);
        assertSame(player, movement.hurtBy.get(0));
        assertSame(animal, movement.hurtCreature);
    }
}
