package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How the palms and cacti of a desert are scattered. The chance of a tree on a sand tile is the density times a
 * factor, thin on dry sand and thick near water, and never more than the maximum.
 *
 * @param density   the base tree density, from 0 to 1
 * @param dryFactor what the density is multiplied by on sand with no water nearby, 0 or more and finite
 * @param wetFactor what the density is multiplied by on sand with water within {@link com.feuerschvenger.perlinsedge.domain.world.model.ResourceType#PALM_REACH}, 0
 *                  or more and finite
 * @param maxChance the largest chance of a tree on any tile, from 0 to 1
 */
public record DesertFloraParams(float density, float dryFactor, float wetFactor, float maxChance) {

    /** Checks the density and the maximum are shares and the factors are not negative. */
    public DesertFloraParams {
        requireShare(density, "density");
        requireShare(maxChance, "maximum chance");
        requireFactor(dryFactor, "dry factor");
        requireFactor(wetFactor, "wet factor");
    }

    /** Refuses a value that is not from 0 to 1. */
    private static void requireShare(float value, String name) {
        if (Float.isNaN(value) || value < 0F || value > 1F) {
            throw new IllegalArgumentException("the " + name + " must be from 0 to 1: " + value);
        }
    }

    /** Refuses a factor that is negative or not finite. */
    private static void requireFactor(float value, String name) {
        if (!Float.isFinite(value) || value < 0F) {
            throw new IllegalArgumentException("the " + name + " must be finite and not negative: " + value);
        }
    }
}
