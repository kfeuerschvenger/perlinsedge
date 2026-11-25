package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.ui.view.FxMinimapDrawer.Layout;

/**
 * The pixels of the minimap tile layer, kept in two parts: the colours of the tiles, which cost a sample per pixel and
 * only change with the map, and the resource markers on top, which change whenever a resource is harvested. The markers
 * are laid over a copy of the colours again when the revision of the map moved, so a harvested resource loses its marker
 * without the colours being sampled again, and a frame in which the map did not change only compares two numbers.
 */
final class MinimapPixels {

    /** The map the pixels show. */
    private final TileMap map;

    /** The colours of the tiles without any marker, never changed after construction. */
    private final int[] base;

    /** The size of the buffer and where the tiles lie in it. */
    private final Layout layout;

    /** The pixels with the markers of the revision in {@link #seenRevision}. */
    private final int[] current;

    /** The revision of the map the markers were laid for. */
    private long seenRevision;

    /**
     * Creates the pixels and lays the markers of the map as it is now.
     *
     * @param map    the map the pixels show
     * @param base   the colours of the tiles without markers, from {@link FxMinimapDrawer#samplePixels}
     * @param layout the size of the buffer and where the tiles lie in it
     */
    MinimapPixels(TileMap map, int[] base, Layout layout) {
        this.map = map;
        this.base = base;
        this.layout = layout;
        this.current = new int[base.length];
        layMarkers();
    }

    /**
     * Tells whether the map changed since the markers were laid.
     *
     * @return true when {@link #refresh()} would change the pixels
     */
    boolean isStale() {
        return map.getRevision() != seenRevision;
    }

    /**
     * Lays the markers again over the colours of the tiles, so they match the resources the map has now.
     *
     * @return the pixels in 0xAARRGGBB layout, the same array every time
     */
    int[] refresh() {
        layMarkers();
        return current;
    }

    /**
     * Returns the pixels as they were last laid.
     *
     * @return the pixels in 0xAARRGGBB layout, the same array every time
     */
    int[] pixels() {
        return current;
    }

    /** Copies the colours of the tiles, puts the markers of the map on them and notes the revision they match. */
    private void layMarkers() {
        seenRevision = map.getRevision();
        System.arraycopy(base, 0, current, 0, base.length);
        FxMinimapDrawer.paintMarkers(map, current, layout);
    }
}
