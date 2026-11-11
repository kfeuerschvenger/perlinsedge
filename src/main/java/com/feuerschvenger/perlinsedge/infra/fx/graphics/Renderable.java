package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import javafx.scene.image.Image;

/**
 * One drawable object of a frame: its screen position, its depth for sorting and what to draw.
 * Instances are mutable and recycled by {@link RenderQueue}, so a frame does not allocate one per object.
 * Read the fields only while the frame that filled them is being drawn.
 */
public final class Renderable {
    private double screenX;
    private double screenY;
    private double drawDepth;
    private Object object;
    private Image sprite;
    private boolean flashing;
    private Tile parentTile;

    Renderable() {
    }

    void set(double screenX, double screenY, double drawDepth, Object object, Image sprite,
             boolean flashing, Tile parentTile) {
        this.screenX = screenX;
        this.screenY = screenY;
        this.drawDepth = drawDepth;
        this.object = object;
        this.sprite = sprite;
        this.flashing = flashing;
        this.parentTile = parentTile;
    }

    /** Drops the references to the game objects so a pooled instance does not keep them alive between frames. */
    void clearReferences() {
        object = null;
        sprite = null;
        parentTile = null;
    }

    /** @return the X-coordinate on the canvas for drawing */
    public double screenX() {
        return screenX;
    }

    /** @return the Y-coordinate on the canvas for drawing */
    public double screenY() {
        return screenY;
    }

    /** @return the depth value used for sorting, larger values are drawn later */
    public double drawDepth() {
        return drawDepth;
    }

    /** @return the game object to draw (Tile, Player, Enemy, etc.) */
    public Object object() {
        return object;
    }

    /** @return the sprite to draw, null when a helper draws the object */
    public Image sprite() {
        return sprite;
    }

    /** @return true if the object should have a flashing effect */
    public boolean isFlashing() {
        return flashing;
    }

    /** @return the parent tile when this is an overlay such as a resource, otherwise null */
    public Tile parentTile() {
        return parentTile;
    }
}
