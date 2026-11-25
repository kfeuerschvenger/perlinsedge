package com.feuerschvenger.perlinsedge.domain.entities.creatures;

import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.strategies.CreatureAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.CreatureMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * Abstract base of every living thing that moves about the world on its own, monsters and animals alike. It holds the
 * state they share, the attack cooldown and the colour, and the lifecycle they share: one final update that runs the
 * same steps in the same order for every creature. What differs is plugged in: movement and attack are strategies, a
 * hurt creature reacts through {@link #onHurt(Entity)}, and the branches say whether they are hostile.
 */
public abstract class Creature extends Entity {
    private static final Rgba DEFAULT_COLOR = Rgba.MAGENTA;

    protected Rgba color;
    protected double attackDamage;
    protected double attackCooldown;
    protected double attackTimer;
    protected final CreatureMovementStrategy movementStrategy;
    protected final CreatureAttackStrategy attackStrategy;

    /**
     * Creates a creature.
     *
     * @param startX           initial x-coordinate (tile position)
     * @param startY           initial y-coordinate (tile position)
     * @param maxHealth        maximum health points
     * @param attackDamage     base damage per attack
     * @param attackCooldown   time between attacks (seconds)
     * @param movementStrategy movement behaviour
     * @param attackStrategy   attack behaviour
     */
    protected Creature(int startX, int startY, double maxHealth, double attackDamage, double attackCooldown,
                       CreatureMovementStrategy movementStrategy, CreatureAttackStrategy attackStrategy) {
        super(startX, startY, maxHealth);
        this.attackDamage = attackDamage;
        this.attackCooldown = attackCooldown;
        this.attackTimer = 0.0;
        this.movementStrategy = movementStrategy;
        this.attackStrategy = attackStrategy;
        this.color = DEFAULT_COLOR;
    }

    /**
     * Gives the kind of creature this is, which a factory builds and a table keyed by species reads.
     *
     * @return the species
     */
    public abstract Species species();

    /**
     * Tells whether this creature is a threat to the player, which decides how the rest of the game treats it.
     *
     * @return true for monsters, false for animals
     */
    public abstract boolean isHostile();

    /**
     * Updates the creature among the others: it moves only to tiles no other creature holds, and the occupancy is
     * brought in line with its tiles before and after the move. This is the single update of every creature and it
     * is final, so subclasses add behaviour through {@link #afterUpdate(Player)} and every creature runs in the
     * same order: cooldown, movement, attack, hook.
     *
     * @param deltaTime time since last update (seconds)
     * @param context   the map, the player and the occupancy shared by every creature of the map
     */
    public final void update(double deltaTime, CreatureContext context) {
        final var occupancy = context.occupancy();
        updateAttackCooldown(deltaTime);
        occupancy.sync(this);
        movementStrategy.updateMovement(this, deltaTime, context);
        occupancy.sync(this);
        attackStrategy.updateAttack(this, deltaTime, context);
        afterUpdate(context.player());
    }

    /**
     * Hook that runs at the end of every update, after the movement and the attack. It does nothing by default.
     *
     * @param player player instance
     */
    protected void afterUpdate(Player player) {
        // No behaviour beyond the strategies by default
    }

    /**
     * Hook that runs when a living creature is about to take damage, before the damage is applied. By default the
     * movement strategy reacts, which is how a creature wakes up, flees or fights back; a branch can override it to
     * add a reaction of its own.
     *
     * @param source who dealt the damage, or null when the source is not known
     */
    protected void onHurt(Entity source) {
        movementStrategy.onHurt(this, source);
    }

    /**
     * Applies damage from a source: a creature that is alive reacts first through {@link #onHurt(Entity)}, then the
     * damage is applied, which can kill it. It is final, so no subclass can skip the reaction.
     *
     * @param amount damage amount to apply
     * @param source who dealt the damage, or null when the source is not known
     */
    public final void takeDamage(double amount, Entity source) {
        if (!isDead) {
            onHurt(source);
        }
        super.takeDamage(amount);
    }

    /**
     * Applies damage from an unknown source, see {@link #takeDamage(double, Entity)}. It is final and goes through
     * the same path as a damage with a source.
     *
     * @param amount damage amount to apply
     */
    @Override
    public final void takeDamage(double amount) {
        takeDamage(amount, null);
    }

    /**
     * Updates the attack cooldown timer.
     *
     * @param deltaTime time since last update (seconds)
     */
    public void updateAttackCooldown(double deltaTime) {
        attackTimer = Math.max(0, attackTimer - deltaTime);
    }

    /**
     * Checks if attack cooldown has expired.
     *
     * @return true if the creature can perform an attack
     */
    public boolean isReadyToAttack() {
        return attackTimer <= 0;
    }

    /**
     * Resets attack cooldown to maximum value.
     */
    public void resetAttackCooldown() {
        attackTimer = attackCooldown;
    }

    /** @return visual representation color */
    public Rgba getColor() {
        return color;
    }

    /** @return base damage per attack */
    public double getAttackDamage() {
        return attackDamage;
    }

    /** @return time between attacks (seconds) */
    public double getAttackCooldown() {
        return attackCooldown;
    }

    /** @return remaining attack cooldown time (seconds) */
    public double getAttackTimer() {
        return attackTimer;
    }

    /** @return configured movement strategy */
    public CreatureMovementStrategy getMovementStrategy() {
        return movementStrategy;
    }

    /** @return configured attack strategy */
    public CreatureAttackStrategy getAttackStrategy() {
        return attackStrategy;
    }

    /**
     * Sets the visual representation color.
     *
     * @param color new color value
     */
    public final void setColor(Rgba color) {
        this.color = color;
    }

    /**
     * Sets base damage per attack.
     *
     * @param attackDamage new damage value
     */
    public void setAttackDamage(double attackDamage) {
        this.attackDamage = attackDamage;
    }

    /**
     * Sets time between attacks.
     *
     * @param attackCooldown new cooldown duration (seconds)
     */
    public void setAttackCooldown(double attackCooldown) {
        this.attackCooldown = attackCooldown;
    }

    /**
     * Directly sets attack cooldown timer.
     *
     * @param attackTimer new cooldown value (seconds)
     */
    protected void setAttackTimer(double attackTimer) {
        this.attackTimer = attackTimer;
    }
}
