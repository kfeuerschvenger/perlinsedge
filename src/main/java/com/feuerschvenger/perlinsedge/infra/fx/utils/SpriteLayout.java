package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.entities.Entity;

/**
 * Pure layout rules for the animated sheets: which player column shows a direction, and which slime frame shows a
 * jump phase.
 */
public final class SpriteLayout {

    /** Frames in the slime strip: rest first, then the five frames of a jump. */
    public static final int SLIME_FRAMES = 6;

    private SpriteLayout() {}

    /**
     * Returns the sheet column that shows a movement direction.
     * The sheet columns follow the screen compass, while the game directions are tile directions, which the isometric
     * projection turns by 45 degrees: moving up the map (DIR_UP) heads up and to the right on screen. The column is
     * therefore one past the direction, wrapping after the last one.
     *
     * @param direction one of the Entity.DIR_* constants
     * @return the column, 0 to 7
     */
    public static int playerColumn(int direction) {
        return Math.floorMod(direction + 1, Entity.DIR_UP_LEFT + 1);
    }

    /**
     * Returns the slime frame for its jump state.
     *
     * @param jumping whether the slime is in the air
     * @param phase   the jump progress, 0 at take off and 1 at landing
     * @return 0 on the ground, 1 to 5 through the jump
     */
    public static int slimeFrame(boolean jumping, double phase) {
        if (!jumping) {
            return 0;
        }
        double clamped = Math.max(0.0, Math.min(1.0, phase));
        return 1 + Math.min(SLIME_FRAMES - 2, (int) (clamped * (SLIME_FRAMES - 1)));
    }
}
