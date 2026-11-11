package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The drawables of one frame, in the order they will be drawn.
 * The {@link Renderable} instances and the backing lists are kept between frames: {@link #clear()} rewinds the
 * queue and {@link #add} refills recycled instances, so a steady frame allocates nothing here.
 */
public final class RenderQueue {

    private static final Comparator<Renderable> BY_DEPTH = Comparator.comparingDouble(Renderable::drawDepth);

    private final List<Renderable> pool = new ArrayList<>();
    private final List<Renderable> queue = new ArrayList<>();

    /**
     * Empties the queue for the next frame, keeping the recycled instances.
     * The instances used by the frame drop their object, sprite and tile references, so a pooled instance never
     * keeps a despawned enemy or a replaced map alive.
     */
    public void clear() {
        for (int i = 0; i < queue.size(); i++) {
            queue.get(i).clearReferences();
        }
        queue.clear();
    }

    /**
     * Adds a drawable, recycling an instance from an earlier frame when one is free.
     *
     * @param screenX    X-coordinate on the canvas
     * @param screenY    Y-coordinate on the canvas
     * @param drawDepth  depth used for sorting
     * @param object     the game object to draw
     * @param sprite     sprite to draw, null when a helper draws the object
     * @param flashing   true for a flashing effect
     * @param parentTile parent tile for overlays, otherwise null
     */
    public void add(double screenX, double screenY, double drawDepth, Object object, Image sprite,
                    boolean flashing, Tile parentTile) {
        int index = queue.size();
        Renderable item;
        if (index < pool.size()) {
            item = pool.get(index);
        } else {
            item = new Renderable();
            pool.add(item);
        }
        item.set(screenX, screenY, drawDepth, object, sprite, flashing, parentTile);
        queue.add(item);
    }

    /** Sorts the queue by depth, stable for equal depths. */
    public void sortByDepth() {
        queue.sort(BY_DEPTH);
    }

    /** @return the number of drawables in the queue */
    public int size() {
        return queue.size();
    }

    /**
     * Returns a drawable of the queue.
     *
     * @param index position in the queue
     * @return the drawable at that position
     */
    public Renderable get(int index) {
        return queue.get(index);
    }

    /** @return the number of {@link Renderable} instances created so far, which stops growing once frames are steady */
    int allocatedInstances() {
        return pool.size();
    }
}
