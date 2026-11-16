package com.feuerschvenger.perlinsedge.domain.strategies;

/**
 * The timing of a slime's strike on a player within reach, free of any entity so it can be tested on its own.
 * <p>
 * A slime that has the player in reach squashes for {@code windupSeconds}, which is the telegraph the player can see,
 * then strikes, then waits {@code waitSeconds} before it can start the next squash. A squash that is cut short because
 * the player left the reach starts over from nothing, and the wait after a strike keeps running whatever the player does.
 */
public final class SlimeStrikeCycle {

    /** Where the slime is in the cycle. */
    public enum Phase {
        /** Nothing is running: the next time the player is in reach the squash begins. */
        READY,
        /** The slime is squashing before it strikes. */
        WINDUP,
        /** The slime has struck and is waiting before it can strike again. */
        WAIT
    }

    private final double windupSeconds;
    private final double waitSeconds;
    private Phase phase = Phase.READY;
    private double timer;

    /**
     * Creates the cycle.
     *
     * @param windupSeconds seconds of squashing before a strike, above zero
     * @param waitSeconds   seconds of waiting after a strike, zero or more
     */
    public SlimeStrikeCycle(double windupSeconds, double waitSeconds) {
        if (windupSeconds <= 0 || waitSeconds < 0) {
            throw new IllegalArgumentException("The squash must take time and the wait cannot be negative");
        }
        this.windupSeconds = windupSeconds;
        this.waitSeconds = waitSeconds;
    }

    /**
     * Advances the cycle.
     *
     * @param deltaTime seconds since the last update
     * @param inReach   true when the player is within reach of the slime and alive
     * @return true on the update in which the squash ends and the strike lands
     */
    public boolean update(double deltaTime, boolean inReach) {
        switch (phase) {
            case READY -> {
                if (inReach) {
                    phase = Phase.WINDUP;
                    timer = 0;
                }
            }
            case WINDUP -> {
                if (!inReach) {
                    phase = Phase.READY;
                    timer = 0;
                } else {
                    timer += deltaTime;
                    if (timer >= windupSeconds) {
                        phase = Phase.WAIT;
                        timer = 0;
                        return true;
                    }
                }
            }
            case WAIT -> {
                timer += deltaTime;
                if (timer >= waitSeconds) {
                    phase = Phase.READY;
                    timer = 0;
                }
            }
        }
        return false;
    }

    /**
     * Gives the phase of the cycle.
     *
     * @return the phase
     */
    public Phase getPhase() {
        return phase;
    }

    /**
     * Gives how far the squash has gone.
     *
     * @return 0 when the slime is not squashing, rising to 1 at the moment of the strike
     */
    public double getWindupProgress() {
        return phase == Phase.WINDUP ? Math.min(1.0, timer / windupSeconds) : 0.0;
    }
}
