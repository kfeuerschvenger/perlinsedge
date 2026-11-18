package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * Creates a building of a given type. It is the one place that knows which class stands behind each type, so the
 * placement rules and the views that need a sample piece, such as the palette icons and the placement ghost, agree.
 */
public final class BuildingFactory {
    /** Number of stacks a placed chest holds. */
    public static final int CHEST_CAPACITY = 9;

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
        return switch (type) {
            case BRIDGE -> new Bridge(x, y);
            case FENCE -> new Fence(x, y);
            case FENCE_GATE -> new FenceGate(x, y);
            case CHEST -> new Chest(x, y, CHEST_CAPACITY);
            case FURNACE -> new Furnace(x, y);
            case CAMPFIRE -> new Campfire(x, y);
            case STANDING_TORCH -> new StandingTorch(x, y);
            case ANVIL -> new Anvil(x, y);
            case WORKBENCH -> new Workbench(x, y);
            case BED -> new Bed(x, y);
        };
    }
}
