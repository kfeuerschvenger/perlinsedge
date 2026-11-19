package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import java.util.EnumMap;
import java.util.Map;

/**
 * Creates a building of a given type. It is the one place that knows which class stands behind each type, so the
 * placement rules and the views that need a sample piece, such as the palette icons and the placement ghost, agree.
 */
public final class BuildingFactory {
    /** Number of stacks a placed chest holds. */
    public static final int CHEST_CAPACITY = 9;

    /** Builds a piece of one type at a tile. */
    @FunctionalInterface
    private interface Creator {
        Building create(int x, int y);
    }

    /** The creator of every type, built once. A test asserts that no {@link BuildingType} lacks one. */
    private static final Map<BuildingType, Creator> CREATORS = new EnumMap<>(BuildingType.class);

    static {
        CREATORS.put(BuildingType.BRIDGE, Bridge::new);
        CREATORS.put(BuildingType.COBBLESTONE_PATH, CobblestonePath::new);
        CREATORS.put(BuildingType.FENCE, Fence::new);
        CREATORS.put(BuildingType.FENCE_GATE, FenceGate::new);
        CREATORS.put(BuildingType.CHEST, (x, y) -> new Chest(x, y, CHEST_CAPACITY));
        CREATORS.put(BuildingType.FURNACE, Furnace::new);
        CREATORS.put(BuildingType.STONE_OVEN, StoneOven::new);
        CREATORS.put(BuildingType.CAMPFIRE, Campfire::new);
        CREATORS.put(BuildingType.STANDING_TORCH, StandingTorch::new);
        CREATORS.put(BuildingType.ANVIL, Anvil::new);
        CREATORS.put(BuildingType.WORKBENCH, Workbench::new);
        CREATORS.put(BuildingType.BED, Bed::new);
        CREATORS.put(BuildingType.SLEEPING_BAG, SleepingBag::new);
    }

    /** Not instantiable: the class only holds the factory method. */
    private BuildingFactory() {
    }

    /**
     * Creates a building at full health.
     *
     * @param type the kind of building
     * @param x    tile X
     * @param y    tile Y
     * @return a new building of the type
     */
    public static Building create(BuildingType type, int x, int y) {
        return create(type, x, y, 0);
    }

    /**
     * Creates a building at full health, facing a given way.
     *
     * @param type     the kind of building
     * @param x        tile X
     * @param y        tile Y
     * @param rotation the way it faces, which wraps around the ways its type can face
     * @return a new building of the type
     */
    public static Building create(BuildingType type, int x, int y, int rotation) {
        final var building = createUnrotated(type, x, y);
        building.setRotation(rotation);
        return building;
    }

    private static Building createUnrotated(BuildingType type, int x, int y) {
        final var creator = CREATORS.get(type);
        if (creator == null) {
            throw new IllegalArgumentException("No creator registered for " + type);
        }
        return creator.create(x, y);
    }
}
