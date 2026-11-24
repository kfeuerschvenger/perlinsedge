package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Guards the state the harness shares with the game and pins what the generator places for one fixed seed. */
class MapSourceTest {

    private static final int SIZE = 150;
    private static final long SEED = 1L;
    private static final int PINNED_IRON_ORE = 54;
    private static final int PINNED_CRYSTALS = 17;

    @Test
    void generatingLeavesTheCurrentMapTypeOfTheWorldAsItWas() {
        final var world = AppConfig.getInstance().world();
        final var before = world.getCurrentMapType();
        final var other = before == MapType.DESERT ? MapType.TUNDRA : MapType.DESERT;

        MapSource.current().generate(new MapSpec(other, SEED, SIZE));

        assertEquals(before, world.getCurrentMapType());
    }

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
