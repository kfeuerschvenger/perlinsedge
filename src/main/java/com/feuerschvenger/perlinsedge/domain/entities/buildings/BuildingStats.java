package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import java.util.EnumMap;
import java.util.Map;

/**
 * The one table that says how much damage each kind of building takes before it falls. Balancing the structures is
 * editing this table and nothing else. Sturdy blocks such as walls and anvils hold the most, light pieces such as a
 * torch or a fence the least.
 */
public final class BuildingStats {
    private static final Map<BuildingType, Integer> MAX_HEALTH = new EnumMap<>(BuildingType.class);

    static {
        MAX_HEALTH.put(BuildingType.BRIDGE, 80);
        MAX_HEALTH.put(BuildingType.DOCK, 80);
        MAX_HEALTH.put(BuildingType.FENCE, 40);
        MAX_HEALTH.put(BuildingType.FENCE_GATE, 50);
        MAX_HEALTH.put(BuildingType.CHEST, 70);
        MAX_HEALTH.put(BuildingType.FURNACE, 100);
        MAX_HEALTH.put(BuildingType.ANVIL, 120);
        MAX_HEALTH.put(BuildingType.WORKBENCH, 80);
        MAX_HEALTH.put(BuildingType.STANDING_TORCH, 20);
        MAX_HEALTH.put(BuildingType.CAMPFIRE, 30);
        MAX_HEALTH.put(BuildingType.BED, 50);
    }

    /** Not instantiable: the class only holds the table. */
    private BuildingStats() {
    }

    /**
     * Gives the health a new building of a type starts with.
     *
     * @param type building type
     * @return the maximum health, always positive
     */
    public static int maxHealth(BuildingType type) {
        return MAX_HEALTH.get(type);
    }
}
