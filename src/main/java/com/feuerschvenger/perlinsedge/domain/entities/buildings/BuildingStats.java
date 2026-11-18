package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import java.util.EnumMap;
import java.util.Map;

/**
 * The one table that says how much damage each kind of building takes before it falls. Balancing the structures is
 * editing this table and nothing else. Sturdy pieces such as furnaces and anvils hold the most, light pieces such as a
 * torch or a fence the least.
 */
public final class BuildingStats {
    private static final int BRIDGE_HEALTH = 80;
    private static final int FENCE_HEALTH = 40;
    private static final int FENCE_GATE_HEALTH = 50;
    private static final int CHEST_HEALTH = 70;
    private static final int FURNACE_HEALTH = 100;
    private static final int ANVIL_HEALTH = 120;
    private static final int WORKBENCH_HEALTH = 80;
    private static final int STANDING_TORCH_HEALTH = 20;
    private static final int CAMPFIRE_HEALTH = 30;
    private static final int BED_HEALTH = 50;

    private static final Map<BuildingType, Integer> MAX_HEALTH = new EnumMap<>(BuildingType.class);

    static {
        MAX_HEALTH.put(BuildingType.BRIDGE, BRIDGE_HEALTH);
        MAX_HEALTH.put(BuildingType.FENCE, FENCE_HEALTH);
        MAX_HEALTH.put(BuildingType.FENCE_GATE, FENCE_GATE_HEALTH);
        MAX_HEALTH.put(BuildingType.CHEST, CHEST_HEALTH);
        MAX_HEALTH.put(BuildingType.FURNACE, FURNACE_HEALTH);
        MAX_HEALTH.put(BuildingType.ANVIL, ANVIL_HEALTH);
        MAX_HEALTH.put(BuildingType.WORKBENCH, WORKBENCH_HEALTH);
        MAX_HEALTH.put(BuildingType.STANDING_TORCH, STANDING_TORCH_HEALTH);
        MAX_HEALTH.put(BuildingType.CAMPFIRE, CAMPFIRE_HEALTH);
        MAX_HEALTH.put(BuildingType.BED, BED_HEALTH);
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
