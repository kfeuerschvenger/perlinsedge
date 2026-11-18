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

    /** Weights of the tile coordinates in the rest of a slime: the reciprocals of the golden and the plastic ratio. */
    private static final double REST_WEIGHT_X = 0.6180339887498949;
    private static final double REST_WEIGHT_Y = 0.7548776662466927;

    /** Layout and constants of the splitmix64 finalizer that turns a spawn tile into a seed. */
    private static final int SEED_X_SHIFT = 32;
    private static final long SEED_LOW_MASK = 0xFFFF_FFFFL;
    private static final long SEED_GAMMA = 0x9E37_79B9_7F4A_7C15L;
    private static final long SEED_MIX_FIRST = 0xBF58_476D_1CE4_E5B9L;
    private static final long SEED_MIX_SECOND = 0x94D0_49BB_1331_11EBL;
    private static final int SEED_SHIFT_FIRST = 30;
    private static final int SEED_SHIFT_SECOND = 27;
    private static final int SEED_SHIFT_FINAL = 31;

    /** Hash primes that mix the tile coordinates into a breath phase, and the number of phases it spreads over. */
    private static final int BREATH_HASH_PRIME_X = 73_856_093;
    private static final int BREATH_HASH_PRIME_Y = 19_349_663;
    private static final int BREATH_PHASE_MASK = 0xFF;
    private static final double BREATH_PHASE_COUNT = 256.0;

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
                new SlimeMovementStrategy(spawnSeed(startX, startY), restFractionAt(startX, startY)),
                new SlimeAttackStrategy()
        );
        setColor(selectRandomColor());
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

    /**
     * The seed of the idle hops of a slime: the tile it spawned on, packed and run through the splitmix64 finalizer,
     * so the same tile always gives the same seed and tiles that differ by one coordinate give unrelated seeds.
     */
    private static long spawnSeed(int tileX, int tileY) {
        var z = (((long) tileX << SEED_X_SHIFT) | (tileY & SEED_LOW_MASK)) + SEED_GAMMA;
        z = (z ^ (z >>> SEED_SHIFT_FIRST)) * SEED_MIX_FIRST;
        z = (z ^ (z >>> SEED_SHIFT_SECOND)) * SEED_MIX_SECOND;
        return z ^ (z >>> SEED_SHIFT_FINAL);
    }

    /**
     * Where the rest between hops of a slime falls in its range. It is the fractional part of a weighted sum of the
     * coordinates with two irrational weights, so the rest of any tile differs from that of each of the eight
     * tiles around it by at least 0.137 of the range, about 25 ms, which is more than a tick at 60 updates per second.
     * Slimes on neighbouring tiles therefore never hop in lockstep.
     */
    private static double restFractionAt(int tileX, int tileY) {
        final double sum = tileX * REST_WEIGHT_X + tileY * REST_WEIGHT_Y;
        return sum - Math.floor(sum);
    }

    /** Spreads spawn tiles over 256 phases with a hash of the coordinates. */
    private static double breathPhaseAt(int tileX, int tileY) {
        return (((tileX * BREATH_HASH_PRIME_X) ^ (tileY * BREATH_HASH_PRIME_Y)) & BREATH_PHASE_MASK) / BREATH_PHASE_COUNT;
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
        if (getMovementStrategy() instanceof SlimeMovementStrategy slimeMovement && slimeMovement.isJumping()) {
            return calculateJumpOffset(slimeMovement);
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
    private static Rgba selectRandomColor() {
        return COLOR_VARIANTS[RANDOM.nextInt(COLOR_VARIANTS.length)];
    }

    /**
     * Calculates vertical offset during jump animation.
     * Uses sine wave for smooth bouncing motion.
     *
     * @param movement Active movement strategy
     * @return Current jump offset
     */
    private static double calculateJumpOffset(SlimeMovementStrategy movement) {
        final double jumpPhase = movement.getJumpPhase();
        final double jumpHeight = Math.sin(jumpPhase * Math.PI) * SlimeMovementStrategy.JUMP_HEIGHT_FACTOR;
        return BASE_RENDER_OFFSET_Y - jumpHeight;
    }

}
