package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.entities.creatures.Species;
import com.feuerschvenger.perlinsedge.domain.strategies.MimicAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.MimicMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SpawnSeed;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * Represents a Mimic enemy that disguises itself as a chest until activated.
 * Mimics remain passive until triggered, then become aggressive toward the player. A hit wakes a mimic through its
 * movement strategy, which reacts to being hurt.
 */
public class Mimic extends Enemy {
    // ==================================================================
    //  Configuration Constants
    // ==================================================================
    private static final double MAX_HEALTH = 50.0;
    private static final double ATTACK_DAMAGE = 20.0;
    private static final double ATTACK_COOLDOWN = 2.0;

    private static final double RENDER_OFFSET_Y = -4.0; // Closer to ground for chest-like appearance
    private static final Rgba BASE_COLOR = Rgba.BROWN.darker();

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new Mimic at the specified position.
     *
     * @param startX Initial X-coordinate (tile position)
     * @param startY Initial Y-coordinate (tile position)
     */
    public Mimic(int startX, int startY) {
        this(startX, startY, new MimicMovementStrategy(SpawnSeed.of(startX, startY)));
    }

    /** Wires the attack of the mimic to the movement it shares its aggro with. */
    private Mimic(int startX, int startY, MimicMovementStrategy movement) {
        super(
                startX,
                startY,
                MAX_HEALTH,
                ATTACK_DAMAGE,
                ATTACK_COOLDOWN,
                movement,
                new MimicAttackStrategy(movement)
        );
        this.setColor(BASE_COLOR);
    }

    // ==================================================================
    //  Rendering Properties
    // ==================================================================

    /**
     * Gives the vertical render offset of a mimic.
     *
     * @return always the offset that keeps it close to the ground, like a chest
     */
    @Override
    public double getRenderOffsetY() {
        return RENDER_OFFSET_Y;
    }

    // ==================================================================
    //  Behavior Methods
    // ==================================================================

    /**
     * Gives the species of a mimic.
     *
     * @return {@link Species#MIMIC}
     */
    @Override
    public Species species() {
        return Species.MIMIC;
    }

}
