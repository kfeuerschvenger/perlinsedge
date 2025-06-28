package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.strategies.MimicAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.MimicMovementStrategy;
import javafx.scene.paint.Color;

public class Mimic extends Enemy {
    private static final double MIMIC_MAX_HEALTH = 50.0;
    private static final double MIMIC_ATTACK_DAMAGE = 20.0;
    private static final double MIMIC_ATTACK_COOLDOWN = 2.0;
    public static final double RENDER_OFFSET_Y = -4; // Mimics sit closer to ground

    public Mimic(int startX, int startY) {
        super(startX, startY, MIMIC_MAX_HEALTH, MIMIC_ATTACK_DAMAGE, MIMIC_ATTACK_COOLDOWN,
                new MimicMovementStrategy(), new MimicAttackStrategy());
        this.color = Color.BROWN.darker(); // Mimics are usually brown
    }

    @Override
    public double getRenderOffsetY() {
        return RENDER_OFFSET_Y; // Mimics don't jump
    }

    // You can add specific Mimic interaction methods here, e.g., onInteract() when clicked
    // which could then set their movementStrategy's aggroed state.
    public void activateMimic() {
        if (movementStrategy instanceof MimicMovementStrategy mimicMove) {
            mimicMove.setAggroed(true);
            System.out.println("Mimic activated!");
        }
    }

}