package com.feuerschvenger.perlinsedge.domain.entities.creatures;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.strategies.MimicMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SkeletonMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SlimeMovementStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A monster reacts to a hit through its movement strategy, with the attacker as the source and the creature as it
 * was before the damage, and a creature that is already dead does not react.
 */
class CreatureHurtReactionTest {
    private static final int TILE = 10;
    private static final int FAR_TILE = 60;
    private static final double HIT = 1.0;

    private final Player attacker = new Player(FAR_TILE, FAR_TILE);

    @Test
    void aSlimeHitFromFarAwayStartsToChaseTheAttacker() {
        final Slime slime = new Slime(TILE, TILE);
        final SlimeMovementStrategy movement = slime.getMovementStrategy();
        assertFalse(movement.isChasing(), "precondition: calm before the hit");

        slime.takeDamage(HIT, attacker);

        assertTrue(movement.isChasing());
        assertEquals(slime.getMaxHealth() - HIT, slime.getHealth(), 0.0);
    }

    @Test
    void aSkeletonHitFromFarAwayStartsToChaseTheAttacker() {
        final Skeleton skeleton = new Skeleton(TILE, TILE);
        final SkeletonMovementStrategy movement = (SkeletonMovementStrategy) skeleton.getMovementStrategy();
        assertFalse(movement.isChasing(), "precondition: still before the hit");

        skeleton.takeDamage(HIT, attacker);

        assertTrue(movement.isChasing());
        assertEquals(skeleton.getMaxHealth() - HIT, skeleton.getHealth(), 0.0);
    }

    @Test
    void aMimicHitFromFarAwayWakesAndAggroesAgainstTheAttacker() {
        final Mimic mimic = new Mimic(TILE, TILE);
        final MimicMovementStrategy movement = (MimicMovementStrategy) mimic.getMovementStrategy();
        assertFalse(movement.isAggroed(), "precondition: a chest before the hit");

        mimic.takeDamage(HIT, attacker);

        assertTrue(movement.isAggroed());
        assertEquals(mimic.getMaxHealth() - HIT, mimic.getHealth(), 0.0);
    }

    @Test
    void aDeadSlimeDoesNotReactToAHit() {
        final Slime slime = new Slime(TILE, TILE);
        slime.handleDeath();

        slime.takeDamage(HIT, attacker);

        assertFalse(slime.getMovementStrategy().isChasing());
    }

    @Test
    void aDeadSkeletonDoesNotReactToAHit() {
        final Skeleton skeleton = new Skeleton(TILE, TILE);
        skeleton.handleDeath();

        skeleton.takeDamage(HIT, attacker);

        assertFalse(((SkeletonMovementStrategy) skeleton.getMovementStrategy()).isChasing());
    }

    @Test
    void aDeadMimicDoesNotReactToAHit() {
        final Mimic mimic = new Mimic(TILE, TILE);
        mimic.handleDeath();

        mimic.takeDamage(HIT, attacker);

        assertFalse(((MimicMovementStrategy) mimic.getMovementStrategy()).isAggroed());
    }
}
