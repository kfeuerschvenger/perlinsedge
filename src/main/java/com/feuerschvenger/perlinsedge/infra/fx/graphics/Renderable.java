package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import javafx.scene.image.Image;

/**
 * Represents a renderable object in the game world.
 *
 * @param screenX        The X-coordinate on the canvas for drawing.
 * @param screenY        The Y-coordinate on the canvas for drawing.
 * @param drawDepth      The depth value used for sorting (determines draw order).
 * @param object         The actual game object (Tile, Player, Enemy, etc.).
 * @param sprite         The Image sprite to be drawn (can be null if drawn by helper).
 * @param isHighlighted  True if this object is currently highlighted (e.g., under mouse).
 * @param isFlashing     True if this object should have a flashing effect.
 * @param parentTile     Reference to the parent Tile if this renderable is an overlay (e.g., resource).
 */
public record Renderable(double screenX, double screenY, double drawDepth, Object object, Image sprite,
                         boolean isHighlighted, boolean isFlashing, Tile parentTile) {

    @Override
    public double screenX() {
        return screenX;
    }

    @Override
    public double screenY() {
        return screenY;
    }

    @Override
    public double drawDepth() {
        return drawDepth;
    }

    @Override
    public Object object() {
        return object;
    }

    @Override
    public Image sprite() {
        return sprite;
    }

    @Override
    public boolean isHighlighted() {
        return isHighlighted;
    }

    @Override
    public boolean isFlashing() {
        return isFlashing;
    }

    @Override
    public Tile parentTile() {
        return parentTile;
    }
}
