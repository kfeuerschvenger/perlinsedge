package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.strategies.SlimeAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SlimeMovementStrategy;
import javafx.scene.paint.Color;
import java.util.Random;

/**
 * Represents a Slime enemy with unique jumping movement and varied coloration.
 * Slimes exhibit bouncy movement patterns and come in various colors.
 */
public class Slime extends Enemy {
    // ==================================================================
    //  Configuration Constants
    // ==================================================================
    private static final double MAX_HEALTH = 30.0;
    private static final double ATTACK_DAMAGE = 10.0;
    private static final double ATTACK_COOLDOWN = 1.0;

    private static final double BASE_RENDER_OFFSET_Y = -8.0;

    // Color variants
    private static final Color[] COLOR_VARIANTS = {
            Color.LIGHTGREEN.deriveColor(0, 1.0, 0.8, 1.0),
            Color.LIGHTBLUE.deriveColor(0, 1.0, 0.8, 1.0),
            Color.LIGHTPINK.deriveColor(0, 1.0, 0.8, 1.0)
    };

    // Shared random generator
    private static final Random RANDOM = new Random();

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new Slime at the specified position with random coloration.
     *
     * @param startX Initial X-coordinate (tile position)
     * @param startY Initial Y-coordinate (tile position)
     */
    public Slime(int startX, int startY) {
        super(
                startX,
                startY,
                MAX_HEALTH,
                ATTACK_DAMAGE,
                ATTACK_COOLDOWN,
                new SlimeMovementStrategy(),
                new SlimeAttackStrategy()
        );
        this.setColor(selectRandomColor());
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    /**
     * Calculates vertical render offset based on jump state.
     * During jumps, slimes rise according to a sine wave pattern.
     *
     * @return Vertical offset for rendering
     */
    @Override
    public double getRenderOffsetY() {
        if (getMovementStrategy() instanceof SlimeMovementStrategy slimeMovement) {
            if (slimeMovement.isJumping()) {
                return calculateJumpOffset(slimeMovement);
            }
        }
        return BASE_RENDER_OFFSET_Y;
    }

    // ==================================================================
    //  Helper Methods
    // ==================================================================

    /**
     * Selects a random color variant for the slime.
     */
    private Color selectRandomColor() {
        return COLOR_VARIANTS[RANDOM.nextInt(COLOR_VARIANTS.length)];
    }

    /**
     * Calculates vertical offset during jump animation.
     * Uses sine wave for smooth bouncing motion.
     *
     * @param movement Active movement strategy
     * @return Current jump offset
     */
    private double calculateJumpOffset(SlimeMovementStrategy movement) {
        final double jumpPhase = movement.getJumpPhase();
        final double jumpHeight = Math.sin(jumpPhase * Math.PI) * SlimeMovementStrategy.JUMP_HEIGHT_FACTOR;
        return BASE_RENDER_OFFSET_Y - jumpHeight;
    }

}