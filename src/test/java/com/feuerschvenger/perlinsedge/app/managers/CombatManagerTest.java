package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatManagerTest {

    private GameStateManager state;
    private CombatManager combat;
    private Player player;
    private Slime slime;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        combat = new CombatManager(state);
        player = new Player(5, 5);
        slime = new Slime(5, 5);
        state.setPlayer(player);
        state.getEnemies().add(slime);
        state.setMouseOverTile(6, 5);
    }

    @Test
    void firstAttackDamagesAnEnemyInRange() {
        double before = slime.getHealth();

        combat.handlePlayerAttack();

        assertEquals(before - player.getAttackDamage(), slime.getHealth(), 1e-9);
    }

    @Test
    void attackingAgainDuringTheCooldownDealsNoDamage() {
        combat.handlePlayerAttack();
        double afterFirst = slime.getHealth();

        combat.handlePlayerAttack();
        combat.handlePlayerAttack();

        assertEquals(afterFirst, slime.getHealth(), 1e-9);
    }

    @Test
    void attackWorksAgainOnceTheCooldownHasPassed() {
        combat.handlePlayerAttack();
        double afterFirst = slime.getHealth();

        for (int i = 0; i < 60; i++) {
            player.update(1.0 / 60.0);
        }
        combat.handlePlayerAttack();

        assertTrue(slime.getHealth() < afterFirst);
    }

    @Test
    void noAttackWhilePaused() {
        state.setGamePaused(true);
        double before = slime.getHealth();

        combat.handlePlayerAttack();

        assertEquals(before, slime.getHealth(), 1e-9);
        assertFalse(player.isAttacking());
    }
}
