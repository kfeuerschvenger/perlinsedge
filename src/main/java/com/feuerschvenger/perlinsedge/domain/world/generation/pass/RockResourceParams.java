package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How iron ore and crystals are scattered over the rock.
 *
 * @param ironPerMille      the chance in thousandths that a rocky tile holds iron ore, from 0 to 1000
 * @param crystalsPerMille  the chance in thousandths that a rocky tile inside a crystal patch holds crystals, from 0
 *                          to 1000
 * @param crystalsThreshold the crystal noise has to pass this value, from 0 to 1, for a tile to be inside a patch
 */
public record RockResourceParams(int ironPerMille, int crystalsPerMille, float crystalsThreshold) {

    /** The scale the chances are given in. */
    public static final int PER_MILLE_SCALE = 1000;

    /** Checks the chances are in range and the threshold is a number from 0 to 1. */
    public RockResourceParams {
        requirePerMille(ironPerMille, "iron");
        requirePerMille(crystalsPerMille, "crystals");
        if (Float.isNaN(crystalsThreshold) || crystalsThreshold < 0F || crystalsThreshold > 1F) {
            throw new IllegalArgumentException("the crystal threshold must be from 0 to 1: " + crystalsThreshold);
        }
    }

    /** Refuses a chance outside 0 to 1000. */
    private static void requirePerMille(int value, String name) {
        if (value < 0 || value > PER_MILLE_SCALE) {
            throw new IllegalArgumentException("the " + name + " chance must be from 0 to " + PER_MILLE_SCALE
                    + " per mille: " + value);
        }
    }
}
