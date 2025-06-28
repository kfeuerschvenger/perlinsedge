package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.strategies.SlimeAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SlimeMovementStrategy;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Represents a Slime enemy in the game.
 * Its movement and attack behaviors are delegated to strategy objects.
 */
public class Slime extends Enemy {
    // Slime colors
    private static final Color[] SLIME_COLORS = {
            Color.LIGHTGREEN.deriveColor(0, 1.0, 0.8, 1.0),
            Color.LIGHTBLUE.deriveColor(0, 1.0, 0.8, 1.0),
            Color.LIGHTPINK.deriveColor(0, 1.0, 0.8, 1.0)
    };

    private static final double SLIME_MAX_HEALTH = 30.0;
    private static final double SLIME_ATTACK_DAMAGE = 10.0;
    private static final double SLIME_ATTACK_COOLDOWN = 1.0;
    public static final double RENDER_OFFSET_Y = -8; // Fixed offset for Slime rendering

    public Slime(int startX, int startY) {
        super(startX, startY, SLIME_MAX_HEALTH, SLIME_ATTACK_DAMAGE, SLIME_ATTACK_COOLDOWN,
                new SlimeMovementStrategy(), new SlimeAttackStrategy());
        this.color = SLIME_COLORS[new Random().nextInt(SLIME_COLORS.length)];
    }

    @Override
    public double getRenderOffsetY() {
        if (movementStrategy instanceof SlimeMovementStrategy slimeMove) {
            if (slimeMove.isJumping()) {
                double jumpHeight = Math.sin(slimeMove.getJumpPhase() * Math.PI) * SlimeMovementStrategy.JUMP_HEIGHT_FACTOR;
                return RENDER_OFFSET_Y - jumpHeight;
            }
        }
        return RENDER_OFFSET_Y;
    }

}