package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * The noise fields a recipe asks for and the settings each one is sampled with. A field that is not listed is not
 * computed, so a recipe pays only for what its passes read.
 *
 * @param fields the settings by field, copied so the parameters cannot change afterwards
 */
public record NoiseFieldsParams(Map<NoiseField, NoiseSettings> fields) {

    /** Copies the map into one that cannot be modified and refuses a missing field or setting. */
    public NoiseFieldsParams {
        final var copy = new EnumMap<NoiseField, NoiseSettings>(NoiseField.class);
        for (var entry : fields.entrySet()) {
            copy.put(Objects.requireNonNull(entry.getKey(), "field"),
                    Objects.requireNonNull(entry.getValue(), "settings"));
        }
        fields = Collections.unmodifiableMap(copy);
    }
}
