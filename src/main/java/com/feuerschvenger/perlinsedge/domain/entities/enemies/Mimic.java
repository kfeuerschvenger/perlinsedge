package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.strategies.MimicAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.MimicMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a Mimic enemy that disguises itself as a chest until activated.
 * Mimics remain passive until triggered, then become aggressive toward the player.
 */
@Slf4j
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
        super(
                startX,
                startY,
                MAX_HEALTH,
                ATTACK_DAMAGE,
                ATTACK_COOLDOWN,
                new MimicMovementStrategy(),
                new MimicAttackStrategy()
        );
        this.setColor(BASE_COLOR);
    }

    // ==================================================================
    //  Rendering Properties
    // ==================================================================

    @Override
    public double getRenderOffsetY() {
        return RENDER_OFFSET_Y;
    }

    // ==================================================================
    //  Behavior Methods
    // ==================================================================

    /**
     * Activates the Mimic when discovered by the player.
     * Triggers aggressive behavior and combat mode.
     */
    public void activate() {
        if (getMovementStrategy() instanceof MimicMovementStrategy mimicMovement) {
            mimicMovement.setAggroed(true);
            log.debug("Mimic activated at ({}, {})", getPosX(), getPosY());
        }
    }

}