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
}
