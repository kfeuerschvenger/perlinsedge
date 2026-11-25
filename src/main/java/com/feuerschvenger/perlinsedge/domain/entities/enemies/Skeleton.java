package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Species;
import com.feuerschvenger.perlinsedge.domain.strategies.CreatureAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.CreatureMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SkeletonAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SkeletonMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SpawnSeed;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * Represents a Skeleton enemy that fights in melee. A skeleton stands still until the player comes within its aggro
 * range or hurts it, then walks to the player and strikes from within
 * {@link SkeletonAttackStrategy#ATTACK_RANGE_TILES}. It shoots no arrows.
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
                    CreatureMovementStrategy movementStrategy,
                    CreatureAttackStrategy attackStrategy) {
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
                    CreatureMovementStrategy movementStrategy, CreatureAttackStrategy attackStrategy) {
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
     * @param source who dealt the damage, or null when the source is not known
     */
    @Override
    protected void onHurt(Entity source) {
        if (getMovementStrategy() instanceof SkeletonMovementStrategy skeletonMovement) {
            skeletonMovement.alert();
        }
    }

    /**
     * Gives the species of a skeleton.
     *
     * @return {@link Species#SKELETON}
     */
    @Override
    public Species species() {
        return Species.SKELETON;
    }

    // ==================================================================
    //  Rendering Properties
    // ==================================================================

    @Override
    public double getRenderOffsetY() {
        return 0; // Skeletons render at standard height
    }
}
