package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildingFactoryTest {

    @Test
    void everyTypeIsCreatedAtItsTileWithItsOwnType() {
        for (BuildingType type : BuildingType.values()) {
            Building building = BuildingFactory.create(type, 4, 7);

            assertEquals(type, building.getType(), type.name());
            assertEquals(4, building.getTileX());
            assertEquals(7, building.getTileY());
            assertEquals(building.getMaxHealth(), building.getHealth());
        }
    }

    @Test
    void theRotationIsStoredOnTheBuildingAndWrapsAroundTheWaysItsTypeCanFace() {
        assertEquals(2, BuildingFactory.create(BuildingType.CHEST, 0, 0, 2).getRotation());
        assertEquals(1, BuildingFactory.create(BuildingType.CHEST, 0, 0, 5).getRotation());
        assertEquals(3, BuildingFactory.create(BuildingType.BED, 0, 0, -1).getRotation());
        assertEquals(1, BuildingFactory.create(BuildingType.BRIDGE, 0, 0, 3).getRotation());
        assertEquals(0, BuildingFactory.create(BuildingType.CAMPFIRE, 0, 0, 3).getRotation(), "it cannot turn");
    }

    @Test
    void fourWayPiecesTwoWayPiecesAndPiecesThatDoNotTurn() {
        for (BuildingType type : new BuildingType[]{BuildingType.CHEST, BuildingType.FURNACE, BuildingType.ANVIL,
                BuildingType.WORKBENCH, BuildingType.BED, BuildingType.DOCK}) {
            assertEquals(4, type.rotationCount(), type.name());
            assertEquals(true, type.isRotatable());
        }
        assertEquals(2, BuildingType.BRIDGE.rotationCount());
        for (BuildingType type : new BuildingType[]{BuildingType.CAMPFIRE, BuildingType.STANDING_TORCH,
                BuildingType.FENCE}) {
            assertEquals(1, type.rotationCount(), type.name());
            assertEquals(false, type.isRotatable());
        }
    }
}
