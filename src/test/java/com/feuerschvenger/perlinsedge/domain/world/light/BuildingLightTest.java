package com.feuerschvenger.perlinsedge.domain.world.light;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingFactory;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.ProcessingStation;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The light of a building: on or off with the state of the building, from the centre of its tile.
 */
class BuildingLightTest {
    private static final AppConfig.LightsConfig CONFIG = AppConfig.getInstance().lights();
    private static final double RADIUS = 3.0;
    private static final double INTENSITY = 0.5;
    private static final double SECONDS = 100.0;

    private static BuildingLight light(AtomicBoolean on) {
        return new BuildingLight(4, 7, RADIUS, new Rgba(1.0, 0.5, 0.2, 1.0), INTENSITY, Flicker.STEADY, on::get);
    }

    @Test
    void itShinesFromTheCentreOfTheTileOfTheBuilding() {
        final var light = light(new AtomicBoolean(true));

        assertEquals(4.0, light.x());
        assertEquals(7.0, light.y());
    }

    @Test
    void itGivesItsRadiusAndIntensityWhileOnAndNothingWhileOff() {
        final var on = new AtomicBoolean(true);
        final var light = light(on);

        assertEquals(RADIUS, light.radius());
        assertEquals(INTENSITY, light.intensity());

        on.set(false);

        assertEquals(0.0, light.radius());
        assertEquals(0.0, light.intensity());
        assertFalse(light.isOn());
    }

    @Test
    void itRejectsValuesThatCannotShine() {
        final var colour = new Rgba(1.0, 1.0, 1.0, 1.0);

        assertThrows(IllegalArgumentException.class,
                () -> new BuildingLight(0, 0, 0.0, colour, INTENSITY, Flicker.STEADY, () -> true));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingLight(0, 0, RADIUS, colour, 1.5, Flicker.STEADY, () -> true));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingLight(0, 0, RADIUS, null, INTENSITY, Flicker.STEADY, () -> true));
        assertThrows(IllegalArgumentException.class,
                () -> new BuildingLight(0, 0, RADIUS, colour, INTENSITY, Flicker.STEADY, null));
    }

    @Test
    void theStandingTorchIsAlwaysOnLikeTheHeldTorch() {
        final var light = CONFIG.newBuildingLight(BuildingFactory.create(BuildingType.STANDING_TORCH, 1, 1)).orElseThrow();

        assertTrue(light.isOn());
        assertEquals(CONFIG.newHeldTorch().radius(), light.radius());
    }

    @Test
    void theCampfireIsOnOnlyWhileItHasFuelAndReachesFurtherThanATorch() {
        final var campfire = (ProcessingStation) BuildingFactory.create(BuildingType.CAMPFIRE, 1, 1);
        final var light = CONFIG.newBuildingLight(campfire).orElseThrow();
        assertFalse(light.isOn());

        campfire.load(ItemType.WOOD_LOG);

        assertTrue(light.isOn());
        assertTrue(light.radius() > CONFIG.newHeldTorch().radius());

        campfire.advance(SECONDS);

        assertFalse(light.isOn(), "the fuel burnt out");
    }

    @Test
    void theFurnaceAndTheStoneOvenGlowOnlyWhileTheyWorkAndGlowLessThanATorch() {
        final var furnace = (ProcessingStation) BuildingFactory.create(BuildingType.FURNACE, 1, 1);
        final var oven = (ProcessingStation) BuildingFactory.create(BuildingType.STONE_OVEN, 1, 1);
        final var furnaceLight = CONFIG.newBuildingLight(furnace).orElseThrow();
        final var ovenLight = CONFIG.newBuildingLight(oven).orElseThrow();
        furnace.load(ItemType.IRON_ORE);
        oven.load(ItemType.RAW_MEAT);
        assertFalse(furnaceLight.isOn(), "ore without fuel does not smelt");
        assertFalse(ovenLight.isOn(), "meat without a log does not cook");

        furnace.load(ItemType.COAL);
        oven.load(ItemType.WOOD_LOG);

        assertTrue(furnaceLight.isOn());
        assertTrue(ovenLight.isOn());
        assertTrue(furnaceLight.radius() < CONFIG.newHeldTorch().radius());
    }

    @Test
    void aBuildingThatGivesNoLightHasNone() {
        for (BuildingType type : new BuildingType[]{BuildingType.CHEST, BuildingType.ANVIL, BuildingType.WORKBENCH,
                BuildingType.FENCE, BuildingType.BRIDGE}) {
            assertTrue(CONFIG.newBuildingLight(BuildingFactory.create(type, 1, 1)).isEmpty(), type.name());
        }
    }
}
