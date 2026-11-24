package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

/** Questions about a terrain that more than one pass asks, so they have one answer. */
final class Terrain {

    /** Not instantiable: the class only holds static questions. */
    private Terrain() {
    }

    /**
     * Tells whether a terrain is the rock that iron ore and crystals lie in.
     *
     * @param type the terrain
     * @return true for hills and mountains
     */
    static boolean isRocky(TileType type) {
        return type == TileType.HILL || type == TileType.MOUNTAIN;
    }

    /**
     * Tells whether something can stand on a terrain: it has to be land, and ice is not, because it is the surface of
     * frozen water.
     *
     * @param type the terrain
     * @return true for the ground that stone and trees may be put on
     */
    static boolean canHoldResource(TileType type) {
        return type.isLand() && type != TileType.ICE;
    }

    /**
     * Tells whether a terrain is a body of water that a river runs into, which is all the water except the frozen
     * kind.
     *
     * @param type the terrain
     * @return true for rivers, shallow water and deep water
     */
    static boolean isFlowingInto(TileType type) {
        return type == TileType.RIVER || type == TileType.SHALLOW_WATER || type == TileType.DEEP_WATER;
    }
}
