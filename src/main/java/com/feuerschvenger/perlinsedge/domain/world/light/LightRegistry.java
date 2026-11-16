package com.feuerschvenger.perlinsedge.domain.world.light;

import java.util.ArrayList;
import java.util.List;

/**
 * The lights of the world at the moment. A feature that gives light adds its source and takes it out again, and the
 * renderer reads them by index every frame, which allocates nothing. The registry holds each source once, compared by
 * identity, and is emptied when the world is replaced, since the sources belong to the world they were added for.
 */
public final class LightRegistry {

    /** The registered sources in the order they were added. */
    private final List<LightSource> sources = new ArrayList<>();

    /**
     * Registers a source.
     *
     * @param source the source to add
     * @return true when it was added, false when this very source was already registered
     * @throws IllegalArgumentException if the source is null
     */
    public boolean add(LightSource source) {
        if (source == null) {
            throw new IllegalArgumentException("A light source cannot be null");
        }
        if (contains(source)) {
            return false;
        }
        sources.add(source);
        return true;
    }

    /**
     * Takes a source out.
     *
     * @param source the source to remove
     * @return true when it was registered
     */
    public boolean remove(LightSource source) {
        for (int i = 0, count = sources.size(); i < count; i++) {
            if (sources.get(i) == source) {
                sources.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a source is registered.
     *
     * @param source the source to look for, compared by identity
     * @return true when it is registered
     */
    public boolean contains(LightSource source) {
        for (int i = 0, count = sources.size(); i < count; i++) {
            if (sources.get(i) == source) {
                return true;
            }
        }
        return false;
    }

    /** Takes every source out, which is what a new world or a rebuilt map starts from. */
    public void clear() {
        sources.clear();
    }

    /** @return the number of registered sources */
    public int size() {
        return sources.size();
    }

    /** @return true when no source is registered */
    public boolean isEmpty() {
        return sources.isEmpty();
    }

    /**
     * Returns a source by its place in the order they were added. Walking the registry by index allocates nothing.
     *
     * @param index from 0 up to but excluding {@link #size()}
     * @return the source
     */
    public LightSource get(int index) {
        return sources.get(index);
    }
}
