package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins what the generator places for one fixed seed. The pinned ore counts depend on the rock chances, the crystal
 * threshold, the noise of the crystal patches and the salts of the tile hash, so changing any of them means capturing
 * the counts again.
 */
class MapSourceTest {

    private static final int SIZE = 150;
    private static final long SEED = 1L;
    private static final int PINNED_IRON_ORE = 46;
    private static final int PINNED_CRYSTALS = 6;

    @Test
    void ironAndCrystalCountsArePinnedForOneSeedAndType() {
        final var map = MapSource.current().generate(new MapSpec(MapType.DESERT, SEED, SIZE));
        var iron = 0;
        var crystals = 0;
        for (var y = 0; y < SIZE; y++) {
            for (var x = 0; x < SIZE; x++) {
                final var resource = map.getTile(x, y).getWorldResource();
                if (resource != null && resource.getType() == ResourceType.IRON_ORE) {
                    iron++;
                } else if (resource != null && resource.getType() == ResourceType.CRYSTALS) {
                    crystals++;
                }
            }
        }

        assertEquals(PINNED_IRON_ORE, iron);
        assertEquals(PINNED_CRYSTALS, crystals);
    }

    @Test
    void thePerTypeInvariantsCannotBeModified() {
        final var perType = MapInvariants.perType();

        assertThrows(UnsupportedOperationException.class, () -> perType.put(MapType.DESERT, List.of()));
    }
}
