package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.generation.pass.ClearingPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.recipe.MapRecipes;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

/**
 * The invariants of the two content maps: the dense forest, whose clearings and trails keep the free land joined and
 * leave a glade around the player, and the mountains, whose rock bands cover a share of the map while their passes
 * keep the valleys joined and the player starts on a valley floor. Every share is of tiles the map holds, so a check
 * holds at every size from {@link MapRecipes#MIN_CONTENT_SIDE} tiles on the shorter side, on a map at most twice as
 * long as it is wide; outside that the recipes make no promise.
 */
final class ContentInvariants {

    /** The share of the free land that the free region of the spawn holds at the least, in a dense forest. */
    private static final double MIN_FOREST_SPAWN_REGION_SHARE = 0.60;
    /** The radius, in tiles, of the disc around the spawn of a dense forest that holds no resource and no water. */
    private static final int FREE_DISC_RADIUS = ClearingPass.MIN_SPAWN_RADIUS - 1;
    /** The share of the map that is mountain, from the least to the most. */
    private static final double MIN_MOUNTAIN_SHARE = 0.20;
    private static final double MAX_MOUNTAIN_SHARE = 0.35;
    /** The share of the walkable ground that the region of the spawn holds at the least, in the mountains. */
    private static final double MIN_GROUND_REGION_SHARE = 0.85;
    /** How many tiles the spawn of the mountains is from the nearest mountain at the least, as the recipe keeps. */
    private static final int MIN_SPAWN_MOUNTAIN_DISTANCE = MapRecipes.MOUNTAINOUS_SPAWN_CLEARANCE;

    /** Not instantiable: the class only holds the checks. */
    private ContentInvariants() {
    }

    /** The trails link the pockets of a dense forest: the free region of the spawn holds most of the free land. */
    static void denseForestSpawnRegionHoldsMostFreeLand(TileMap map) {
        final var spawn = map.getSpawn().orElseThrow();
        final var regionSize = MapStats.regionSizes(map)[spawn.y() * map.getWidth() + spawn.x()];
        final var share = regionSize / (double) MapStats.freeTiles(map);
        MapInvariants.require(share >= MIN_FOREST_SPAWN_REGION_SHARE,
                "the free region of the spawn holds only " + share + " of the free land");
    }

    /** The player starts in a glade: every tile within the free disc radius of the spawn is free ground. */
    static void denseForestSpawnHasAFreeDisc(TileMap map) {
        final var spawn = map.getSpawn().orElseThrow();
        final var squared = FREE_DISC_RADIUS * FREE_DISC_RADIUS;
        for (var dy = -FREE_DISC_RADIUS; dy <= FREE_DISC_RADIUS; dy++) {
            for (var dx = -FREE_DISC_RADIUS; dx <= FREE_DISC_RADIUS; dx++) {
                if (dx * dx + dy * dy <= squared) {
                    final var x = spawn.x() + dx;
                    final var y = spawn.y() + dy;
                    MapInvariants.require(map.isValidCoordinate(x, y) && MapStats.isFree(map.getTile(x, y)),
                            "the disc of radius " + FREE_DISC_RADIUS + " around the spawn " + spawn
                                    + " is not free at (" + x + "," + y + ")");
                }
            }
        }
    }

    /** The mountains cover a fifth to a third of the map. */
    static void mountainsCoverPartOfTheMap(TileMap map) {
        final var share = MapStats.count(map, TileType.MOUNTAIN) / (double) ((long) map.getWidth() * map.getHeight());
        MapInvariants.require(share >= MIN_MOUNTAIN_SHARE && share <= MAX_MOUNTAIN_SHARE,
                "mountains cover " + share + " of the map");
    }

    /** The passes join the valleys: the ground the spawn can walk to holds most of the walkable ground of the map. */
    static void mountainousSpawnRegionHoldsTheGround(TileMap map) {
        final var spawn = map.getSpawn().orElseThrow();
        final var walkable = Components.of(map, TileType::isWalkable);
        final var region = walkable.sizeOf(walkable.labelAt(spawn.x(), spawn.y()));
        final var share = region / (double) MapStats.walkableTiles(map);
        MapInvariants.require(share >= MIN_GROUND_REGION_SHARE,
                "the walkable region of the spawn holds only " + share + " of the walkable ground");
    }

    /** The player starts in a valley, at least a few tiles from the nearest mountain. */
    static void mountainousSpawnIsInAValley(TileMap map) {
        final var spawn = map.getSpawn().orElseThrow();
        var nearest = Integer.MAX_VALUE;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                if (map.getTile(x, y).getType() == TileType.MOUNTAIN) {
                    nearest = Math.min(nearest, Math.max(Math.abs(x - spawn.x()), Math.abs(y - spawn.y())));
                }
            }
        }
        MapInvariants.require(nearest >= MIN_SPAWN_MOUNTAIN_DISTANCE,
                "the spawn " + spawn + " is " + nearest + " tiles from a mountain");
    }
}
