package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Decides the terrain of a tile from its height and climate. One rule serves a whole map, chosen by its recipe. A rule
 * declares which of the two climate fields it reads, so the ground pass asks only for those and a recipe computes only
 * the noise its rule uses.
 *
 * @param climateFields the climate fields the rule reads, a subset of temperature and moisture, copied so they cannot
 *                      change afterwards
 * @param classifier    the classification itself
 */
public record GroundRule(Set<NoiseField> climateFields, Classifier classifier) {

    /** The fields a rule is allowed to declare. */
    private static final Set<NoiseField> CLIMATE = Collections.unmodifiableSet(
            EnumSet.of(NoiseField.TEMPERATURE, NoiseField.MOISTURE));

    /** Checks the classifier is set and the declared fields are climate fields, and copies them. */
    public GroundRule {
        Objects.requireNonNull(classifier, "classifier");
        climateFields = copyOfClimate(climateFields);
    }

    /**
     * Creates a rule that reads the height only.
     *
     * @param classifier the classification, which receives a constant for the temperature and the moisture
     * @return the rule
     */
    public static GroundRule ofHeight(Classifier classifier) {
        return new GroundRule(Set.of(), classifier);
    }

    /**
     * Classifies a tile.
     *
     * @param height      the height of the tile, about -1 to 1, with the dither of the ground pass already added
     * @param temperature the temperature, 0 to 1, or the neutral constant when the rule does not declare it
     * @param moisture    the moisture, 0 to 1, or the neutral constant when the rule does not declare it
     * @return the terrain of the tile
     */
    public TileType classify(float height, float temperature, float moisture) {
        return classifier.classify(height, temperature, moisture);
    }

    /** Copies the declared fields into an immutable set and refuses a field that is not temperature or moisture. */
    private static Set<NoiseField> copyOfClimate(Collection<NoiseField> declared) {
        final var copy = EnumSet.noneOf(NoiseField.class);
        for (var field : declared) {
            if (!CLIMATE.contains(Objects.requireNonNull(field, "field"))) {
                throw new IllegalArgumentException("a ground rule reads climate fields only, not " + field);
            }
            copy.add(field);
        }
        return Collections.unmodifiableSet(copy);
    }

    /** The classification of a rule, as a function of the height and the two climate values. */
    @FunctionalInterface
    public interface Classifier {

        /**
         * Classifies a tile.
         *
         * @param height      the height of the tile
         * @param temperature the temperature, 0 to 1
         * @param moisture    the moisture, 0 to 1
         * @return the terrain of the tile
         */
        TileType classify(float height, float temperature, float moisture);
    }
}
