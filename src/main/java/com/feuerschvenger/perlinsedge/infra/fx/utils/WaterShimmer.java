package com.feuerschvenger.perlinsedge.infra.fx.utils;

/**
 * The slow shimmer on the water surface: a few thin light lines per tile that fade in and out.
 * Every value is a pure function of the game time and the tile coordinates. The animation clock is counted in cycles
 * and computed once per frame by {@link #cycles}. Each tile and each line has its own phase, so the lines of a lake
 * never blink in sync, and a line reappears at a new spot every cycle, always while it is fully faded out so it never
 * jumps in view.
 */
public final class WaterShimmer {

    /** The most lines a tile can show, and the number of line sprites. */
    public static final int MAX_LINES = 3;

    /** Seconds one line takes to fade in and out. */
    static final double PERIOD_SECONDS = 2.0;

    /** The opacity of a line at its brightest. */
    static final double PEAK_ALPHA = 0.35;

    /** How far from the tile centre a line may sit along each isometric axis, as a fraction of the tile. */
    static final double MAX_OFFSET = 0.3;

    /** Phase distance between two lines of the same tile, in cycles. */
    private static final double LINE_STAGGER = 0.31;

    private static final long SALT_COUNT = 0x1B87_3593_A5C1L;
    private static final long SALT_PHASE = 0x6A09_E667_F3BCL;
    private static final long SALT_U = 0x3C6E_F372_FE94L;
    private static final long SALT_V = 0x510E_527F_ADE6L;

    /** Not instantiable: the class only holds static rules. */
    private WaterShimmer() {}

    /**
     * Converts the game time into animation cycles, which the other methods take.
     *
     * @param gameTime the game time in seconds
     * @return the elapsed cycles, fractional
     */
    public static double cycles(double gameTime) {
        return gameTime / PERIOD_SECONDS;
    }

    /**
     * Returns how many lines a tile shows.
     *
     * @param x the tile column
     * @param y the tile row
     * @return two or three
     */
    public static int lineCount(int x, int y) {
        return 2 + (int) (GroundDecals.mix(x, y, 0L, SALT_COUNT) & 1L);
    }

    /**
     * Returns the opacity of one line of a tile.
     *
     * @param cycles the elapsed cycles from {@link #cycles}
     * @param x      the tile column
     * @param y      the tile row
     * @param line   the line index, below {@link #MAX_LINES}
     * @return an opacity in [0, PEAK_ALPHA], zero when the tile has no such line or the line is faded out
     */
    public static double alpha(double cycles, int x, int y, int line) {
        if (line >= lineCount(x, y)) {
            return 0.0;
        }
        double s = linePhase(cycles, x, y, line);
        double t = s - Math.floor(s);
        return (1.0 - Math.abs(2.0 * t - 1.0)) * PEAK_ALPHA;
    }

    /**
     * Returns the offset of a line along the first isometric axis.
     *
     * @param cycles the elapsed cycles from {@link #cycles}
     * @param x      the tile column
     * @param y      the tile row
     * @param line   the line index
     * @return a fraction of the tile in [-MAX_OFFSET, MAX_OFFSET], constant during one fade
     */
    public static double offsetU(double cycles, int x, int y, int line) {
        return unit(lineHash(cycles, x, y, line, SALT_U)) * MAX_OFFSET;
    }

    /**
     * Returns the offset of a line along the second isometric axis.
     *
     * @param cycles the elapsed cycles from {@link #cycles}
     * @param x      the tile column
     * @param y      the tile row
     * @param line   the line index
     * @return a fraction of the tile in [-MAX_OFFSET, MAX_OFFSET], constant during one fade
     */
    public static double offsetV(double cycles, int x, int y, int line) {
        return unit(lineHash(cycles, x, y, line, SALT_V)) * MAX_OFFSET;
    }

    /** The tile's own phase in [0, 1), so neighbouring tiles are out of step. */
    static double tileOffset(int x, int y) {
        return (GroundDecals.mix(x, y, 0L, SALT_PHASE) >>> 11) * 0x1.0p-53;
    }

    /** Returns the phase of a line in cycles: its fade is over each time the whole part grows by one. */
    private static double linePhase(double cycles, int x, int y, int line) {
        return cycles + tileOffset(x, y) + line * LINE_STAGGER;
    }

    /** Hashes the tile, the line and the fade the line is in, so a line moves only between two fades. */
    private static long lineHash(double cycles, int x, int y, int line, long salt) {
        long fade = (long) Math.floor(linePhase(cycles, x, y, line));
        return GroundDecals.mix(x, y, fade * 31L + line, salt);
    }

    /** Maps a hash to a value in [-1, 1). */
    private static double unit(long hash) {
        return ((hash >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }
}
