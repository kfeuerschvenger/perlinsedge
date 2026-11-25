package com.feuerschvenger.perlinsedge.domain.entities.creatures;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.strategies.MimicAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.MimicMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SkeletonAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SkeletonMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SlimeAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SlimeMovementStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The factory builds the class that goes with each species and wires its strategies. */
class CreatureFactoryTest {
    private static final int TILE_X = 4;
    private static final int TILE_Y = 7;

    @ParameterizedTest
    @EnumSource(Species.class)
    void everySpeciesIsBuiltOnTheTileAskedForAndReportsItsOwnSpecies(Species species) {
        final Creature creature = CreatureFactory.create(species, TILE_X, TILE_Y);

        assertEquals(species, creature.species());
        assertEquals(TILE_X, creature.getCurrentTileX());
        assertEquals(TILE_Y, creature.getCurrentTileY());
    }

    @Test
    void eachSpeciesIsWiredWithTheStrategiesOfItsOwnKind() {
        final Creature slime = CreatureFactory.create(Species.SLIME, TILE_X, TILE_Y);
        final Creature skeleton = CreatureFactory.create(Species.SKELETON, TILE_X, TILE_Y);
        final Creature mimic = CreatureFactory.create(Species.MIMIC, TILE_X, TILE_Y);

        assertInstanceOf(SlimeMovementStrategy.class, slime.getMovementStrategy());
        assertInstanceOf(SlimeAttackStrategy.class, slime.getAttackStrategy());
        assertInstanceOf(SkeletonMovementStrategy.class, skeleton.getMovementStrategy());
        assertInstanceOf(SkeletonAttackStrategy.class, skeleton.getAttackStrategy());
        assertInstanceOf(MimicMovementStrategy.class, mimic.getMovementStrategy());
        assertInstanceOf(MimicAttackStrategy.class, mimic.getAttackStrategy());
    }

    @Test
    void eachSpeciesIsBuiltAsItsOwnClassOfTheMonsterBranch() {
        assertInstanceOf(Slime.class, CreatureFactory.create(Species.SLIME, TILE_X, TILE_Y));
        assertInstanceOf(Skeleton.class, CreatureFactory.create(Species.SKELETON, TILE_X, TILE_Y));
        assertInstanceOf(Mimic.class, CreatureFactory.create(Species.MIMIC, TILE_X, TILE_Y));
    }

    @ParameterizedTest
    @EnumSource(Species.class)
    void theMonstersOfTheFactoryAreHostile(Species species) {
        final Creature creature = CreatureFactory.create(species, TILE_X, TILE_Y);

        assertInstanceOf(Enemy.class, creature);
        assertTrue(creature.isHostile());
    }

    @Test
    void everyCallBuildsANewCreatureWithStrategiesOfItsOwn() {
        final Creature first = CreatureFactory.create(Species.SKELETON, TILE_X, TILE_Y);
        final Creature second = CreatureFactory.create(Species.SKELETON, TILE_X, TILE_Y);

        assertNotSame(first, second);
        assertNotSame(first.getMovementStrategy(), second.getMovementStrategy());
    }
}
