package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;

/**
 * Represents a blacksmith's anvil in the game.
 * It is a solid, destructible building used for crafting items.
 */
public class Anvil extends Building {

    /**
     * Creates a new anvil at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Anvil(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.ANVIL, true, true);
    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public boolean onInteract(Tile tile) {
        return true;
    }
}
