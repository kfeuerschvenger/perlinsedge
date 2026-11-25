package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.ui.view.FxMinimapDrawer.Layout;

import java.util.ArrayList;
import java.util.BitSet;

/**
 * The pixels of the minimap tile layer, kept in two parts: the colours of the tiles, which cost a sample per pixel and
 * only change with the map, and the resource markers on top, which change whenever a resource is harvested. The tiles
 * that carry a marker are collected once when the pixels are built. When the revision of the map moved, only those
 * tiles are looked at again and only the pixels whose marker changed are restored from the colours and marked again,
 * so a harvested resource loses its marker without the whole map being scanned or the whole image being uploaded. When
 * several tiles share one pixel the marker with the highest priority shows, crystals over iron over stone. The class
 * keeps its own copy of the colours and never hands out its working array. Only the removal of the markers collected
 * at construction is tracked: a resource or terrain added to the map afterwards is not seen, so a feature that
 * regrows resources must build new pixels instead of refreshing these.
 */
final class MinimapPixels {

    /** The marker a resource tile shows, from the lowest priority to the highest. */
    private enum Marker {
        NONE(0, 0),
        STONE(1, 0xFFCFCFCF),
        IRON(2, 0xFFB5502E),
        CRYSTALS(3, 0xFF4FE0E8);

        /** The priority of the marker, 0 for none: when tiles share a pixel the highest priority shows. */
        private final byte priority;

        /** The colour of the marker in 0xAARRGGBB layout. */
        private final int argb;

        /** Creates a marker with its priority and colour. */
        Marker(int priority, int argb) {
            this.priority = (byte) priority;
            this.argb = argb;
        }
    }

    /** What a refresh answers when no pixel changed. */
    private static final int[] NO_PIXELS = new int[0];

    /** The map the pixels show. */
    private final TileMap map;

    /** The colours of the tiles without any marker, a private copy that is never changed. */
    private final int[] base;

    /** The pixels with the markers of the revision in {@link #seenRevision}, never handed out. */
    private final int[] current;

    /** The priority of the marker laid on each pixel, 0 for a pixel without one. */
    private final byte[] laid;

    /** The pixels of the tiles that carry a marker, in the order the tiles were collected. */
    private final int[] markerPixels;

    /** The columns of the tiles that carry a marker. */
    private final int[] markerColumns;

    /** The rows of the tiles that carry a marker. */
    private final int[] markerRows;

    /** The marker each collected tile shows now. */
    private final Marker[] markers;

    /** The pixels a refresh is rebuilding, empty between refreshes. */
    private final BitSet touched;

    /** The revision of the map the markers were laid for. */
    private long seenRevision;

    /**
     * Creates the pixels and lays the markers of the map as it is now. The colours are copied, so the caller keeps its
     * array.
     *
     * @param map     the map the pixels show
     * @param colours the colours of the tiles without markers, from {@link FxMinimapDrawer#samplePixels}
     * @param layout  the size of the buffer and where the tiles lie in it
     * @throws IllegalArgumentException when the colours do not fill the buffer of the layout exactly
     */
    MinimapPixels(TileMap map, int[] colours, Layout layout) {
        if (colours.length != layout.imageWidth() * layout.imageHeight()) {
            throw new IllegalArgumentException("the colours hold " + colours.length + " pixels but the layout is "
                    + layout.imageWidth() + " by " + layout.imageHeight());
        }
        this.map = map;
        this.base = colours.clone();
        this.current = colours.clone();
        this.laid = new byte[colours.length];
        this.touched = new BitSet(colours.length);
        final var tiles = collectMarkerTiles(map, layout);
        final var count = tiles.size();
        this.markerPixels = new int[count];
        this.markerColumns = new int[count];
        this.markerRows = new int[count];
        this.markers = new Marker[count];
        for (var i = 0; i < count; i++) {
            final var tile = tiles.get(i);
            markerColumns[i] = tile.getX();
            markerRows[i] = tile.getY();
            markerPixels[i] = pixelOf(layout, tile.getX(), tile.getY());
            markers[i] = markerOf(tile);
            stamp(i);
        }
        seenRevision = map.getRevision();
    }

    /**
     * Tells whether the map changed since the markers were laid.
     *
     * @return true when {@link #refresh()} may change the pixels
     */
    boolean isStale() {
        return map.getRevision() != seenRevision;
    }

    /**
     * Looks again at the tiles that carried a marker, and for every pixel whose marker changed puts back the colour of
     * the tile and lays the marker with the highest priority among the tiles on it.
     *
     * @return the indices of the pixels that were rebuilt, empty when the map did not change or no marker did; when
     * a tile cannot be read the exception is thrown with the map still stale, so the next call reads it again and
     * rebuilds the pixels already touched
     */
    int[] refresh() {
        if (!isStale()) {
            return NO_PIXELS;
        }
        final var revision = map.getRevision();
        for (var i = 0; i < markers.length; i++) {
            final var now = markerOf(map.getTile(markerColumns[i], markerRows[i]));
            if (now != markers[i]) {
                markers[i] = now;
                touched.set(markerPixels[i]);
            }
        }
        seenRevision = revision;
        if (touched.isEmpty()) {
            return NO_PIXELS;
        }
        final var rebuilt = touched.stream().toArray();
        for (var pixel : rebuilt) {
            current[pixel] = base[pixel];
            laid[pixel] = 0;
        }
        for (var i = 0; i < markers.length; i++) {
            if (touched.get(markerPixels[i])) {
                stamp(i);
            }
        }
        touched.clear();
        return rebuilt;
    }

    /**
     * Returns the colour of one pixel as it was last laid.
     *
     * @param index the index of the pixel in the buffer
     * @return the colour in 0xAARRGGBB layout
     */
    int pixelAt(int index) {
        return current[index];
    }

    /**
     * Returns a copy of the pixels as they were last laid.
     *
     * @return a new array in 0xAARRGGBB layout
     */
    int[] snapshot() {
        return current.clone();
    }

    /** Lays the marker of a collected tile on its pixel when it outranks what is laid there. */
    private void stamp(int slot) {
        final var marker = markers[slot];
        final var pixel = markerPixels[slot];
        if (marker.priority > laid[pixel]) {
            laid[pixel] = marker.priority;
            current[pixel] = marker.argb;
        }
    }

    /** Collects the tiles whose resource has a marker and whose centre lies inside the buffer. */
    private static ArrayList<Tile> collectMarkerTiles(TileMap map, Layout layout) {
        final var tiles = new ArrayList<Tile>();
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                if (markerOf(tile) != Marker.NONE && pixelOf(layout, x, y) >= 0) {
                    tiles.add(tile);
                }
            }
        }
        return tiles;
    }

    /** Returns the index of the pixel under the centre of a tile, or -1 when it lies outside the buffer. */
    private static int pixelOf(Layout layout, int tileX, int tileY) {
        final var px = (int) Math.floor((tileX - tileY) * layout.tileW() + layout.offsetX());
        final var py = (int) Math.floor((tileX + tileY + 1) * layout.tileH() + layout.offsetY());
        final var inside = px >= 0 && py >= 0 && px < layout.imageWidth() && py < layout.imageHeight();
        return inside ? py * layout.imageWidth() + px : -1;
    }

    /** Returns the marker of the resource of a tile, none for a tile without a marked resource. */
    private static Marker markerOf(Tile tile) {
        if (!tile.hasResource()) {
            return Marker.NONE;
        }
        final ResourceType type = tile.getWorldResource().getType();
        return switch (type) {
            case STONE -> Marker.STONE;
            case IRON_ORE -> Marker.IRON;
            case CRYSTALS -> Marker.CRYSTALS;
            case TREE -> Marker.NONE;
        };
    }
}
