package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * A cobblestone path, a floor laid on walkable land. It only changes how the ground looks: it never blocks movement and
 * changes nothing about what can stand on the tile.
 */
public class CobblestonePath extends Building {

    /**
     * Creates a new path tile at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public CobblestonePath(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.COBBLESTONE_PATH, false, true);
    }
}
