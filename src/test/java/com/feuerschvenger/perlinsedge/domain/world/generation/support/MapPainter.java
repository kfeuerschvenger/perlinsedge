package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Paints a map as an image with one pixel per tile: the colour of the terrain, a colour over it for a resource. */
public final class MapPainter {

    private static final int OPAQUE = 0xFF00_0000;
    private static final int CHANNEL_MAX = 255;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    /** Half the side of the square marker that shows the spawn, in pixels. */
    private static final int MARKER_REACH = 2;
    private static final Rgba MARKER_COLOUR = Rgba.RED;
    private static final Rgba TREE_COLOUR = Rgba.rgb(10, 50, 15);
    private static final Rgba STONE_COLOUR = Rgba.rgb(170, 170, 175);

    private static final Map<ResourceType, Rgba> RESOURCE_COLOURS = resourceColours();

    private MapPainter() {
    }

    private static Map<ResourceType, Rgba> resourceColours() {
        final var colours = new EnumMap<ResourceType, Rgba>(ResourceType.class);
        colours.put(ResourceType.TREE, TREE_COLOUR);
        colours.put(ResourceType.STONE, STONE_COLOUR);
        colours.put(ResourceType.IRON_ORE, Rgba.ORANGE);
        colours.put(ResourceType.CRYSTALS, Rgba.MAGENTA);
        return colours;
    }

    /**
     * Paints a map without a marker.
     *
     * @param map the map
     * @return an image as wide and as high as the map
     */
    public static BufferedImage paint(TileMap map) {
        return paint(map, Optional.empty());
    }

    /**
     * Paints a map and marks a spot in red.
     *
     * @param map  the map
     * @param spot where to put the marker, or empty for none
     * @return an image as wide and as high as the map
     */
    public static BufferedImage paint(TileMap map, Optional<Spot> spot) {
        final var image = new BufferedImage(map.getWidth(), map.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                final var colour = tile.hasResource()
                        ? RESOURCE_COLOURS.get(tile.getWorldResource().getType())
                        : tile.getColor();
                image.setRGB(x, y, argb(colour));
            }
        }
        spot.ifPresent(marker -> mark(image, marker));
        return image;
    }

    private static void mark(BufferedImage image, Spot spot) {
        for (var dy = -MARKER_REACH; dy <= MARKER_REACH; dy++) {
            for (var dx = -MARKER_REACH; dx <= MARKER_REACH; dx++) {
                final var x = spot.x() + dx;
                final var y = spot.y() + dy;
                if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
                    image.setRGB(x, y, argb(MARKER_COLOUR));
                }
            }
        }
    }

    private static int argb(Rgba colour) {
        return OPAQUE | channel(colour.r()) << RED_SHIFT | channel(colour.g()) << GREEN_SHIFT | channel(colour.b());
    }

    private static int channel(double value) {
        return (int) Math.round(value * CHANNEL_MAX);
    }
}
