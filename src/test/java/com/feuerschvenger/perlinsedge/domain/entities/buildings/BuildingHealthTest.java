package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingHealthTest {

    @Test
    void everyTypeHasAPositiveMaxHealthAndANewBuildingStartsAtIt() {
        for (BuildingType type : BuildingType.values()) {
            assertTrue(BuildingStats.maxHealth(type) > 0, type.name());
        }
        Fence wall = new Fence(1, 1);
        assertEquals(BuildingStats.maxHealth(BuildingType.FENCE), wall.getHealth());
        assertEquals(wall.getMaxHealth(), wall.getHealth());
        assertFalse(wall.isDamaged());
        assertEquals(1.0, wall.getHealthFraction(), 1e-9);
    }

    @Test
    void damageLowersTheHealthAndNeverGoesBelowZero() {
        Fence wall = new Fence(1, 1);

        wall.damage(30);
        assertEquals(wall.getMaxHealth() - 30, wall.getHealth());
        assertTrue(wall.isDamaged());
        assertFalse(wall.isDestroyed());

        wall.damage(10_000);
        assertEquals(0, wall.getHealth());
        assertTrue(wall.isDestroyed());
    }

    @Test
    void aNegativeDamageChangesNothing() {
        Fence wall = new Fence(1, 1);
        wall.damage(-5);
        assertEquals(wall.getMaxHealth(), wall.getHealth());
    }

    @Test
    void theRefundIsTheShareOfTheQuantityThatMatchesTheHealthRoundedDown() {
        Fence wall = new Fence(1, 1);
        assertEquals(7, wall.refundOf(7));

        wall.damage(wall.getMaxHealth() / 2);
        assertEquals(3, wall.refundOf(7));
        assertEquals(0, wall.refundOf(1));

        wall.damage(10_000);
        assertEquals(0, wall.refundOf(7));
    }
}
