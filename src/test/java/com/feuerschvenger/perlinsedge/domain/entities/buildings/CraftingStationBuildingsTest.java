package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.crafting.CraftingStation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingStationBuildingsTest {

    @Test
    void theWorkbenchAndTheAnvilOpenTheirOwnStation() {
        assertEquals(Optional.of(CraftingStation.WORKBENCH), BuildingType.WORKBENCH.craftingStation());
        assertEquals(Optional.of(CraftingStation.ANVIL), BuildingType.ANVIL.craftingStation());
    }

    @Test
    void noOtherBuildingOpensAStation() {
        for (BuildingType type : BuildingType.values()) {
            if (type != BuildingType.WORKBENCH && type != BuildingType.ANVIL) {
                assertTrue(type.craftingStation().isEmpty(), type.name());
            }
        }
    }

    @Test
    void theStationTitlesAreTheNamesOfTheBuildings() {
        assertEquals(BuildingType.WORKBENCH.getDisplayName(), CraftingStation.WORKBENCH.getDisplayName());
        assertEquals(BuildingType.ANVIL.getDisplayName(), CraftingStation.ANVIL.getDisplayName());
    }

    @Test
    void theWorkbenchAndTheAnvilAreClickableAndHandleTheClick() {
        for (Building station : new Building[] {new Workbench(1, 1), new Anvil(1, 1)}) {
            assertTrue(station.isInteractable(), station.getType().name());
            assertTrue(station.onInteract(null), station.getType().name());
            assertTrue(station.isSolid(), "a station blocks movement");
        }
    }

    @Test
    void aChestIsNotACraftingStationAndAFenceIsNotClickable() {
        assertFalse(new Fence(1, 1).isInteractable());
        assertTrue(BuildingType.CHEST.craftingStation().isEmpty());
    }
}
