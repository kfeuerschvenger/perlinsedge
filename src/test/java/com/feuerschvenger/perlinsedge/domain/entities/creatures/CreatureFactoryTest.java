package com.feuerschvenger.perlinsedge.domain.entities.creatures;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The factory builds the class that goes with each species and wires its strategies. */
class CreatureFactoryTest {
    private static final int TILE_X = 4;
    private static final int TILE_Y = 7;

    private final CreatureFactory factory = new CreatureFactory();

    @ParameterizedTest
    @EnumSource(Species.class)
    void everySpeciesIsBuiltOnTheTileAskedForAndReportsItsOwnSpecies(Species species) {
        final Creature creature = factory.create(species, TILE_X, TILE_Y);

        assertEquals(species, creature.species());
        assertEquals(TILE_X, creature.getCurrentTileX());
        assertEquals(TILE_Y, creature.getCurrentTileY());
    }

    @ParameterizedTest
    @EnumSource(Species.class)
    void everySpeciesComesWithBothItsStrategies(Species species) {
        final Creature creature = factory.create(species, TILE_X, TILE_Y);

        assertNotNull(creature.getMovementStrategy());
        assertNotNull(creature.getAttackStrategy());
    }

    @Test
    void eachSpeciesIsBuiltAsItsOwnClassOfTheMonsterBranch() {
        assertInstanceOf(Slime.class, factory.create(Species.SLIME, TILE_X, TILE_Y));
        assertInstanceOf(Skeleton.class, factory.create(Species.SKELETON, TILE_X, TILE_Y));
        assertInstanceOf(Mimic.class, factory.create(Species.MIMIC, TILE_X, TILE_Y));
    }

    @ParameterizedTest
    @EnumSource(Species.class)
    void theMonstersOfTheFactoryAreHostile(Species species) {
        final Creature creature = factory.create(species, TILE_X, TILE_Y);

        assertInstanceOf(Enemy.class, creature);
        assertTrue(creature.isHostile());
    }

    @Test
    void everyCallBuildsANewCreature() {
        final Creature first = factory.create(Species.SKELETON, TILE_X, TILE_Y);
        final Creature second = factory.create(Species.SKELETON, TILE_X, TILE_Y);

        assertNotSame(first, second);
    }
}
