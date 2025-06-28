package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import javafx.scene.paint.Color;

/**
 * Represents an enemy in the game. Extends Entity and uses Strategy pattern for movement and attack.
 */
public abstract class Enemy extends Entity {

    // Specific properties for Enemy visual
    protected Color color;

    protected double attackDamage;
    protected double attackCooldown;
    protected double attackTimer;

    protected EnemyMovementStrategy movementStrategy;
    protected EnemyAttackStrategy attackStrategy;

    public Enemy(int startX, int startY, double maxHealth, double attackDamage, double attackCooldown,
                 EnemyMovementStrategy movementStrategy, EnemyAttackStrategy attackStrategy) {
        super(startX, startY, maxHealth);
        this.attackDamage = attackDamage;
        this.attackCooldown = attackCooldown;
        this.attackTimer = 0.0;
        this.movementStrategy = movementStrategy;
        this.attackStrategy = attackStrategy;
        this.color = Color.MAGENTA;
    }

    /**
     * Updates the enemy's state. Delegates movement and attack logic to strategies.
     * @param deltaTime Time since last update in seconds.
     * @param map The game map.
     * @param player The player character.
     */
    public void update(double deltaTime, TileMap map, Player player) {
        // Update common cooldowns
        attackTimer = Math.max(0, attackTimer - deltaTime);

        // Delegate to strategies
        movementStrategy.updateMovement(this, deltaTime, map, player);
        attackStrategy.updateAttack(this, deltaTime, player);
    }

    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public double getAttackDamage() { return attackDamage; }
    public void setAttackDamage(double attackDamage) { this.attackDamage = attackDamage; }

    public double getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(double attackCooldown) { this.attackCooldown = attackCooldown; }

    public double getAttackTimer() { return attackTimer; }
    public void setAttackTimer(double attackTimer) { this.attackTimer = attackTimer; }

    public EnemyMovementStrategy getMovementStrategy() { return movementStrategy; }

    public EnemyAttackStrategy getAttackStrategy() { return attackStrategy; }

}