package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import com.feuerschvenger.perlinsedge.domain.strategies.SlimeAttackStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SlimeMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
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
    private static final Rgba[] COLOR_VARIANTS = {
            Rgba.LIGHT_GREEN.scaleBrightness(0.8),
            Rgba.LIGHT_BLUE.scaleBrightness(0.8),
            Rgba.LIGHT_PINK.scaleBrightness(0.8)
    };

    // Shared random generator
    private static final Random RANDOM = new Random();

    /** The breath phase of the slime, fixed at spawn, see {@link #getBreathPhase()}. */
    private final double breathPhase;

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
                new SlimeMovementStrategy(spawnSeed(startX, startY)),
                new SlimeAttackStrategy()
        );
        this.setColor(selectRandomColor());
        this.breathPhase = breathPhaseAt(startX, startY);
    }

    /**
     * Where in its breath the slime is at clock zero, in breaths. It follows from the tile the slime spawned on, so it
     * is the same on every run and every frame, and slimes on neighbouring tiles breathe out of step.
     *
     * @return the phase, from 0 up to but not including 1
     */
    public double getBreathPhase() {
        return breathPhase;
    }

    /** The seed of the timing of a slime, a hash of the tile it spawned on, so the same tile always gives the same timing. */
    private static long spawnSeed(int tileX, int tileY) {
        return ((long) tileX << 32) ^ (tileY * 0x9E3779B97F4A7C15L);
    }

    /** Spreads spawn tiles over 256 phases with a hash of the coordinates. */
    private static double breathPhaseAt(int tileX, int tileY) {
        return (((tileX * 73_856_093) ^ (tileY * 19_349_663)) & 0xFF) / 256.0;
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

    /**
     * Gives how far the squash that telegraphs the next strike has gone.
     *
     * @return 0 when the slime is not squashing, rising to 1 at the moment of the strike
     */
    public double getWindupProgress() {
        return getAttackStrategy() instanceof SlimeAttackStrategy strike ? strike.getWindupProgress() : 0.0;
    }

    // ==================================================================
    //  Behavior Methods
    // ==================================================================

    /**
     * A hit makes the slime chase the attacker even from outside its aggro range.
     *
     * @param amount Damage amount to apply
     */
    @Override
    public void takeDamage(double amount) {
        if (!isDead && getMovementStrategy() instanceof SlimeMovementStrategy slimeMovement) {
            slimeMovement.alert();
        }
        super.takeDamage(amount);
    }

    // ==================================================================
    //  Helper Methods
    // ==================================================================

    /**
     * Selects a random color variant for the slime.
     */
    private Rgba selectRandomColor() {
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