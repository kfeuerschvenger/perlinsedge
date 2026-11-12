package com.feuerschvenger.perlinsedge.app.managers;

/**
 * The short answer the cursor gives to a click order: the tile that was clicked flashes green when the order was
 * accepted and red when it was refused.
 * The state is one tile, one outcome and the game time of the click, so the renderer can read it every frame without
 * any allocation, and a later click replaces the earlier one. The flash fades on the game clock, so it stands still
 * while the game is paused.
 */
public final class ClickFeedback {

    /** What the cursor shows for the clicked tile. */
    public enum Kind {
        /** No flash is running. */
        NONE,
        /** The order was accepted. */
        ACCEPTED,
        /** The order was refused. */
        REJECTED
    }

    /** How long the flash lasts, in seconds of game time. */
    public static final double DURATION_SECONDS = 0.45;

    private int tileX = -1;
    private int tileY = -1;
    private boolean accepted;
    private double startTime = Double.NEGATIVE_INFINITY;

    /**
     * Starts a flash on a tile, replacing the one that runs.
     *
     * @param x        tile column
     * @param y        tile row
     * @param accepted true for the accepted flash, false for the refused one
     * @param gameTime the game time of the click in seconds
     */
    public void flash(int x, int y, boolean accepted, double gameTime) {
        this.tileX = x;
        this.tileY = y;
        this.accepted = accepted;
        this.startTime = gameTime;
    }

    /** Forgets the flash that runs. */
    public void clear() {
        tileX = -1;
        tileY = -1;
        startTime = Double.NEGATIVE_INFINITY;
    }

    /**
     * Returns what the flash shows at a game time.
     *
     * @param gameTime the game time in seconds
     * @return {@link Kind#NONE} when no flash runs or it has ended
     */
    public Kind kind(double gameTime) {
        final double elapsed = gameTime - startTime;
        if (elapsed < 0 || elapsed >= DURATION_SECONDS) {
            return Kind.NONE;
        }
        return accepted ? Kind.ACCEPTED : Kind.REJECTED;
    }

    /**
     * Returns how much of the flash is left.
     *
     * @param gameTime the game time in seconds
     * @return 1 at the click, falling to 0 when the flash ends, and 0 when none runs
     */
    public double strength(double gameTime) {
        if (kind(gameTime) == Kind.NONE) {
            return 0;
        }
        return 1.0 - (gameTime - startTime) / DURATION_SECONDS;
    }

    /** @return the column of the flashing tile, -1 when there is none */
    public int getTileX() {
        return tileX;
    }

    /** @return the row of the flashing tile, -1 when there is none */
    public int getTileY() {
        return tileY;
    }
}
