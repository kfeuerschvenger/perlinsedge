package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.strategies.EnemyAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.EnemyMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SkeletonAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SkeletonMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SpawnSeed;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * Represents a Skeleton enemy with ranged attack capabilities.
 * Skeletons can shoot arrows at players from a distance, implementing unique combat behavior.
 */
public class Skeleton extends Enemy {
    // ==================================================================
    //  Default Configuration Constants
    // ==================================================================
    private static final double MAX_HEALTH = 30.0;
    private static final double ATTACK_DAMAGE = 8.0;
    private static final double ATTACK_COOLDOWN = 3.0;
    private static final Rgba BONE_COLOR = Rgba.rgb(226, 220, 200);

    // ==================================================================
    //  Constructors
    // ==================================================================

    /**
     * Creates a skeleton with the default melee behaviour: it chases the player when near and strikes when adjacent.
     *
     * @param startX Initial x-coordinate (tile position)
     * @param startY Initial y-coordinate (tile position)
     */
    public Skeleton(int startX, int startY) {
        this(startX, startY, new SkeletonMovementStrategy(SpawnSeed.of(startX, startY)), new SkeletonAttackStrategy());
    }

    /**
     * Creates a skeleton with default attributes using custom strategies.
     *
     * @param startX            Initial x-coordinate (tile position)
     * @param startY            Initial y-coordinate (tile position)
     * @param movementStrategy  Custom movement behavior implementation
     * @param attackStrategy    Custom attack behavior implementation
     */
    public Skeleton(int startX, int startY,
                    EnemyMovementStrategy movementStrategy,
                    EnemyAttackStrategy attackStrategy) {
        this(startX, startY, MAX_HEALTH, ATTACK_DAMAGE,
                ATTACK_COOLDOWN, movementStrategy, attackStrategy);
    }

    /**
     * Creates a fully customizable skeleton instance.
     *
     * @param startX            Initial x-coordinate (tile position)
     * @param startY            Initial y-coordinate (tile position)
     * @param maxHealth         Maximum health points
     * @param attackDamage      Base damage per attack
     * @param attackCooldown    Time between attacks (seconds)
     * @param movementStrategy  Movement behavior implementation
     * @param attackStrategy    Attack behavior implementation
     */
    public Skeleton(int startX, int startY, double maxHealth, double attackDamage, double attackCooldown,
                    EnemyMovementStrategy movementStrategy, EnemyAttackStrategy attackStrategy) {
        super(startX, startY, maxHealth, attackDamage, attackCooldown, movementStrategy, attackStrategy);
        setColor(BONE_COLOR);
    }

    // ==================================================================
    //  Core Game Loop Methods
    // ==================================================================

    /**
     * A hit makes the skeleton chase the attacker even from outside its aggro range. A skeleton built with another
     * movement strategy has no chase to start and only takes the damage.
     *
     * @param amount Damage amount to apply
     */
    @Override
    public void takeDamage(double amount) {
        if (!isDead && getMovementStrategy() instanceof SkeletonMovementStrategy skeletonMovement) {
            skeletonMovement.alert();
        }
        super.takeDamage(amount);
    }

    // ==================================================================
    //  Rendering Properties
    // ==================================================================

    @Override
    public double getRenderOffsetY() {
        return 0; // Skeletons render at standard height
    }
}
