package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void everyTypeHasABuildingToCreate() {
        for (BuildingType type : BuildingType.values()) {
            assertNotNull(BuildingFactory.create(type, 0, 0), type.name());
            assertNotNull(BuildingFactory.create(type, 0, 0, 1), type.name());
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
    void aSleepingBagLiesFlatSoItDoesNotBlockMovement() {
        final Building bag = BuildingFactory.create(BuildingType.SLEEPING_BAG, 1, 1);

        assertFalse(bag.isSolid());
        assertTrue(bag.isDestructible());
        assertEquals("Sleeping Bag", BuildingType.SLEEPING_BAG.getDisplayName());
    }

    @Test
    void fourWayPiecesTwoWayPiecesAndPiecesThatDoNotTurn() {
        for (BuildingType type : new BuildingType[]{BuildingType.CHEST, BuildingType.FURNACE, BuildingType.ANVIL,
                BuildingType.WORKBENCH, BuildingType.BED, BuildingType.SLEEPING_BAG}) {
            assertEquals(4, type.rotationCount(), type.name());
            assertTrue(type.isRotatable());
        }
        assertEquals(2, BuildingType.BRIDGE.rotationCount());
        for (BuildingType type : new BuildingType[]{BuildingType.CAMPFIRE, BuildingType.STANDING_TORCH,
                BuildingType.FENCE}) {
            assertEquals(1, type.rotationCount(), type.name());
            assertFalse(type.isRotatable());
        }
    }

    @Test
    void theStationsAreProcessingStationsAndTheFrontFollowsTheRotation() {
        final int[][] fronts = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        for (BuildingType type : new BuildingType[]{BuildingType.FURNACE, BuildingType.STONE_OVEN}) {
            for (int rotation = 0; rotation < fronts.length; rotation++) {
                final var station = BuildingFactory.create(type, 0, 0, rotation);

                assertTrue(station instanceof ProcessingStation, type.name());
                assertEquals(fronts[rotation][0], station.frontOffset()[0]);
                assertEquals(fronts[rotation][1], station.frontOffset()[1]);
            }
        }
        assertTrue(BuildingFactory.create(BuildingType.CAMPFIRE, 0, 0) instanceof ProcessingStation);
    }

    @Test
    void theEdgesAfterTheFrontTurnClockwise() {
        final var oven = BuildingFactory.create(BuildingType.STONE_OVEN, 0, 0, 1);

        assertEquals(-1, oven.frontOffset(1)[0]);
        assertEquals(0, oven.frontOffset(1)[1]);
        assertEquals(1, oven.frontOffset(3)[0]);
        assertEquals(0, oven.frontOffset(3)[1]);
        assertEquals(oven.frontOffset()[1], oven.frontOffset(4)[1]);
    }

    @Test
    void theStoneOvenIsSolidAndTheCampfireIsNot() {
        assertTrue(BuildingFactory.create(BuildingType.STONE_OVEN, 0, 0).isSolid());
        assertTrue(BuildingFactory.create(BuildingType.FURNACE, 0, 0).isSolid());
        assertFalse(BuildingFactory.create(BuildingType.CAMPFIRE, 0, 0).isSolid());
    }
}
