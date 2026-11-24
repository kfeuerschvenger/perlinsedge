package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;

/**
 * Per tile chances that are a hash of the tile, the seed and a salt rather than draws from a random stream, so a
 * chance never moves the numbers any other pass draws and does not depend on the order the tiles are visited in.
 */
final class Chance {

    /** The hash is cut into this many bits to make a unit value, the width of a double mantissa. */
    private static final int UNIT_SHIFT = 11;
    private static final double UNIT_SCALE = 0x1.0p-53;

    /** The scale of a chance given in ten thousandths. */
    public static final int TEN_THOUSAND_SCALE = 10_000;

    /** Not instantiable: the class only holds static helpers. */
    private Chance() {
    }

    /**
     * Returns a value from 0 up to but not including 1 for a tile, unrelated to the value of any neighbour.
     *
     * @param context the generation, which supplies the seed
     * @param x       the column
     * @param y       the row
     * @param salt    a constant of the use, so two uses of the same tile are independent
     * @return the value
     */
    static double unit(GenerationContext context, int x, int y, long salt) {
        return (context.tileHash(x, y, salt) >>> UNIT_SHIFT) * UNIT_SCALE;
    }

    /**
     * Tells whether a tile passes a chance given in thousandths.
     *
     * @param context  the generation, which supplies the seed
     * @param x        the column
     * @param y        the row
     * @param salt     a constant of the resource, so the chances of two resources are independent
     * @param perMille the chance, from 0 to {@link RockResourceParams#PER_MILLE_SCALE}
     * @return true for about {@code perMille} tiles in a thousand
     */
    static boolean perMille(GenerationContext context, int x, int y, long salt, int perMille) {
        return Math.floorMod(context.tileHash(x, y, salt), (long) RockResourceParams.PER_MILLE_SCALE) < perMille;
    }

    /**
     * Tells whether a tile passes a chance given in ten thousandths, for the rare things a thousandth is too coarse for.
     *
     * @param context        the generation, which supplies the seed
     * @param x              the column
     * @param y              the row
     * @param salt           a constant of the use, so two uses of the same tile are independent
     * @param perTenThousand the chance, from 0 to {@link #TEN_THOUSAND_SCALE}
     * @return true for about {@code perTenThousand} tiles in ten thousand
     */
    static boolean perTenThousand(GenerationContext context, int x, int y, long salt, int perTenThousand) {
        return Math.floorMod(context.tileHash(x, y, salt), (long) TEN_THOUSAND_SCALE) < perTenThousand;
    }
}
