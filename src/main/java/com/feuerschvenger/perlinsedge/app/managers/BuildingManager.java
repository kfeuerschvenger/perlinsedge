package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingFactory;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceGate;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Manages building operations including placement, removal, interaction, and preview visualization.
 * Handles resource validation, tile suitability checks, and building lifecycle management.
 */
@Slf4j
public class BuildingManager {
    private static final double PREVIEW_OPACITY = 0.7;
    /** The tile offsets of the four neighbours that share an edge with a tile, not the diagonal ones. */
    private static final int[][] EDGE_NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    /** How many tiles away from the player a refund pile is looked for when the player's own tile is no ground. */
    private static final int REFUND_SEARCH_RADIUS = 4;
    /** The hint of a piece, a demolition or a closing gate that is refused because someone is in the tile. */
    private static final String HINT_IN_THE_WAY = "Something is standing there";

    // Dependencies
    private final GameStateManager gameState;
    private final RecipeManager recipeManager;
    private final ContainerManager containerManager;
    private final ItemDropManager itemDropManager;
    private final Runnable onInventoryChanged;
    private Consumer<String> hintSink = (String _) -> { };

    // Building mode state
    @Getter
    private BuildingType selectedBuildingType;
    /** The piece chosen last, which comes back as the selection when the hammer is picked up again. */
    private BuildingType lastBuildingType;
    /** The rotation chosen for each piece type, kept while the hammer stays in hand. */
    private final Map<BuildingType, Integer> rotations = new EnumMap<>(BuildingType.class);
    /** Whether a piece of each type blocks walking, read once from a sample piece of the type. */
    private final Map<BuildingType, Boolean> blocksWalking = new EnumMap<>(BuildingType.class);
    @Getter
    private boolean buildingModeActive;

    /**
     * Creates the manager with a refund that drops what the inventory cannot hold and tells nobody.
     *
     * @param gameState        shared game state
     * @param recipeManager    building recipes
     * @param containerManager opens the containers the player interacts with
     */
    public BuildingManager(GameStateManager gameState, RecipeManager recipeManager, ContainerManager containerManager) {
        this(gameState, recipeManager, containerManager, new ItemDropManager(gameState, () -> { }), () -> { });
    }

    /**
     * Creates the manager.
     *
     * @param gameState          shared game state
     * @param recipeManager      building recipes
     * @param containerManager   opens the containers the player interacts with
     * @param itemDropManager    drops the part of a refund that does not fit into the inventory
     * @param onInventoryChanged called after a demolition changed the inventory, so the views are redrawn
     */
    public BuildingManager(GameStateManager gameState, RecipeManager recipeManager, ContainerManager containerManager,
                           ItemDropManager itemDropManager, Runnable onInventoryChanged) {
        this.gameState = gameState;
        this.recipeManager = recipeManager;
        this.containerManager = containerManager;
        this.itemDropManager = itemDropManager;
        this.onInventoryChanged = onInventoryChanged;
        this.buildingModeActive = false;
        this.selectedBuildingType = null;
    }

    /**
     * Sets where the short hints of a refused action go.
     *
     * @param hintSink receives the hint text
     */
    public void setHintSink(Consumer<String> hintSink) {
        this.hintSink = hintSink != null ? hintSink : (String _) -> { };
    }

    /**
     * Activates building mode. The piece chosen last is selected again.
     */
    public void activateBuildingMode() {
        if (!buildingModeActive) {
            buildingModeActive = true;
            selectedBuildingType = lastBuildingType;
            log.info("Building mode: ON");
        }
    }

    /**
     * Turns the selected piece a quarter turn clockwise seen from above, which is the next way it can face. The rotation
     * is remembered for that piece type until the hammer leaves the hand.
     *
     * @return true when a rotatable piece is selected and was turned, false when there is none or it cannot turn
     */
    public boolean rotateSelected() {
        if (!buildingModeActive || selectedBuildingType == null || !selectedBuildingType.isRotatable()) {
            return false;
        }
        rotations.put(selectedBuildingType, (getRotation(selectedBuildingType) + 1) % selectedBuildingType.rotationCount());
        return true;
    }

    /**
     * Gives the rotation the next piece of a type is placed with.
     *
     * @param type a building type
     * @return the remembered rotation, 0 when none was chosen or the type cannot turn
     */
    public int getRotation(BuildingType type) {
        return type == null ? 0 : rotations.getOrDefault(type, 0);
    }

    /**
     * Deactivates building mode. Nothing stays selected, and the piece chosen last is remembered. The rotations are
     * forgotten, they only last while the hammer stays in hand.
     */
    public void deactivateBuildingMode() {
        if (buildingModeActive) {
            buildingModeActive = false;
            selectedBuildingType = null;
            rotations.clear();
            log.info("Building mode: OFF");
        }
    }

    /**
     * Sets the currently selected building type for placement. A piece is remembered as the last one chosen, a null
     * clears the selection and keeps the memory.
     *
     * @param type Building type to select (null to clear selection)
     */
    public void setSelectedBuildingType(BuildingType type) {
        selectedBuildingType = type;
        if (type != null) {
            lastBuildingType = type;
            log.debug("Selected building type: {}", type.getDisplayName());
        } else {
            log.debug("Building type selection cleared.");
        }
    }

    /**
     * Gives the building on a tile, for the views that show its health.
     *
     * @param tileX tile X
     * @param tileY tile Y
     * @return the building, or null when the tile is outside the map or has none
     */
    public Building getBuildingAt(int tileX, int tileY) {
        var tileMap = gameState.getCurrentMap();
        Tile tile = tileMap != null ? tileMap.getTile(tileX, tileY) : null;
        return tile != null ? tile.getBuilding() : null;
    }

    /**
     * Places the selected building at specified coordinates after validating conditions.
     *
     * @param worldX X-coordinate in world units
     * @param worldY Y-coordinate in world units
     * @return true when the piece was placed and its resources consumed, false when anything refused it
     */
    public boolean placeBuilding(int worldX, int worldY) {
        // Validate preconditions
        if (!validatePlacementPreconditions(worldX, worldY)) {
            return false;
        }

        var tileMap = gameState.getCurrentMap();
        var player = gameState.getPlayer();
        var targetTile = tileMap.getTile(worldX, worldY);
        Optional<BuildingRecipe> recipe = recipeManager.getBuildingRecipeByType(selectedBuildingType);
        BuildingRecipe existingRecipe = recipe.orElse(null);

        // Validate resources and placement rules
        if (!playerHasResources(player, existingRecipe) || !canPlaceOnTile(selectedBuildingType, targetTile)) {
            return false;
        }
        // Checked last, so the terrain or the missing materials are what the player is told about first
        if (isBlockedByEntity(selectedBuildingType, worldX, worldY)) {
            log.debug("Placement failed: The player or an enemy is on or moving to the tile");
            hintSink.accept(HINT_IN_THE_WAY);
            return false;
        }

        var newBuilding =BuildingFactory.create(selectedBuildingType, worldX, worldY,
                getRotation(selectedBuildingType));
        consumeResources(player, existingRecipe);
        // A bridge is the floor of the tile and every other piece stands on top of it
        if (selectedBuildingType.isFloor()) {
            targetTile.setFloor(newBuilding);
        } else {
            targetTile.setBuilding(newBuilding);
            FenceConnections.refresh(tileMap, worldX, worldY);
        }
        log.info("Placed {} at ({}, {})", selectedBuildingType.getDisplayName(), worldX, worldY);
        return true;
    }

    /**
     * Removes a building from specified coordinates if destructible.
     *
     * @param worldX X-coordinate in world units
     * @param worldY Y-coordinate in world units
     * @return true when a building was demolished and refunded
     */
    public boolean removeBuilding(int worldX, int worldY) {
        var tileMap = gameState.getCurrentMap();
        if (tileMap == null) {
            log.warn("Remove building failed: Map is null");
            return false;
        }

        var targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null || (!targetTile.hasBuilding() && !targetTile.hasFloor())) {
            log.debug("No building at ({}, {})", worldX, worldY);
            return false;
        }
        // The piece on top comes down first, and the floor under it only once nothing stands on it
        if (!targetTile.hasBuilding()) {
            return removeFloor(worldX, worldY);
        }

        var building = targetTile.getBuilding();
        if (!building.isDestructible()) {
            log.debug("Building not destructible: {}", building.getType().getDisplayName());
            return false;
        }

        targetTile.setBuilding(null);
        FenceConnections.refresh(tileMap, worldX, worldY);
        refund(building);
        log.info("Removed {} from ({}, {})", building.getType().getDisplayName(), worldX, worldY);
        return true;
    }

    /**
     * Demolishes the bridge that is the floor of a tile. A floor with a piece standing on it cannot be demolished
     * until that piece is removed, so the player is never left with a piece hanging over the water.
     *
     * @param worldX tile X
     * @param worldY tile Y
     * @return true when the floor was demolished and refunded, false when there is none, a piece stands on it, the
     *         player or a living enemy is on or stepping to the tile, the tile is on the rest of the player's walk, so
     *         the player never ends up in the water, or it cannot be destroyed
     */
    public boolean removeFloor(int worldX, int worldY) {
        final var tileMap = gameState.getCurrentMap();
        final Tile tile = tileMap != null ? tileMap.getTile(worldX, worldY) : null;
        if (tile == null || !tile.hasFloor()) {
            return false;
        }
        if (tile.hasBuilding()) {
            log.debug("The floor at ({}, {}) has a piece on it", worldX, worldY);
            hintSink.accept("Remove what stands on it first");
            return false;
        }
        final Building floor = tile.getFloor();
        if (!floor.isDestructible()) {
            return false;
        }
        if (isEntityOn(worldX, worldY) || isOnPlayerWalk(worldX, worldY)) {
            log.debug("The floor at ({}, {}) has the player or an enemy on it, or is on the player's walk", worldX, worldY);
            hintSink.accept(HINT_IN_THE_WAY);
            return false;
        }
        tile.setFloor(null);
        refund(floor);
        log.info("Removed {} from ({}, {})", floor.getType().getDisplayName(), worldX, worldY);
        return true;
    }

    /**
     * Gives the player back the share of the recipe that matches the health the building has left: for each
     * ingredient {@code floor(quantity * health / maxHealth)} items. What the inventory has no room for is dropped as a
     * pile on the player's tile, or on the nearest ground when that tile is none, and what a chest held falls out on
     * its own tile. The views are told of the inventory once, when everything has moved, while every pile dropped on
     * the ground is announced by the drop manager on its own.
     */
    private void refund(Building building) {
        // What a chest held falls out on its own tile, whole, whatever its health
        if (building instanceof Chest chest) {
            for (Item content : chest.getItems()) {
                if (content != null && content.getQuantity() > 0) {
                    itemDropManager.dropAt(content, building.getTileX(), building.getTileY());
                }
            }
        }
        refundIngredients(building);
        onInventoryChanged.run();
    }

    private void refundIngredients(Building building) {
        var player = gameState.getPlayer();
        Optional<BuildingRecipe> recipe = recipeManager.getBuildingRecipeByType(building.getType());
        if (player == null || recipe.isEmpty()) {
            return;
        }

        for (CraftingIngredient ingredient : recipe.get().ingredients()) {
            int quantity = building.refundOf(ingredient.quantity());
            if (quantity <= 0) {
                continue;
            }
            var refund = new Item(ingredient.itemType(), quantity);
            // Container.addItem leaves in the item what it could not store, so a positive quantity is the leftover
            player.addItem(refund);
            if (refund.getQuantity() > 0) {
                dropOnGround(refund, player);
            }
        }
    }

    /** Drops a pile at the player's feet, or on the nearest walkable tile when the player's own tile is no ground. */
    private void dropOnGround(Item pile, Player player) {
        final var tileMap = gameState.getCurrentMap();
        final int fromX = player.getCurrentTileX();
        final int fromY = player.getCurrentTileY();
        for (var radius = 0; tileMap != null && radius <= REFUND_SEARCH_RADIUS; radius++) {
            if (dropOnRing(pile, tileMap, fromX, fromY, radius)) {
                return;
            }
        }
        itemDropManager.dropAtFeet(pile, player);
    }

    /** Drops the pile on the first ground tile at exactly the given Chebyshev radius from the origin, reporting whether it did. */
    private boolean dropOnRing(Item pile, TileMap tileMap, int fromX, int fromY, int radius) {
        for (var dx = -radius; dx <= radius; dx++) {
            for (var dy = -radius; dy <= radius; dy++) {
                if (Math.max(Math.abs(dx), Math.abs(dy)) == radius && isGround(tileMap, fromX + dx, fromY + dy)) {
                    itemDropManager.dropAt(pile, fromX + dx, fromY + dy);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isGround(TileMap tileMap, int x, int y) {
        final var tile = tileMap.getTile(x, y);
        return tile != null && tile.isWalkable();
    }

    /**
     * Handles player interaction with a building at specified coordinates.
     *
     * @param worldX X-coordinate in world units
     * @param worldY Y-coordinate in world units
     * @return true if interaction occurred, false otherwise
     */
    public boolean interactWithBuilding(int worldX, int worldY) {
        var tileMap = gameState.getCurrentMap();
        if (tileMap == null) {
            log.warn("Interaction failed: Map is null");
            return false;
        }

        var targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null || !targetTile.hasBuilding()) {
            return false;
        }

        var building = targetTile.getBuilding();
        // Closing a gate on someone would trap them in it, so only opening is always allowed
        if (building instanceof FenceGate gate && gate.isOpen() && isEntityOn(worldX, worldY)) {
            log.debug("Gate at ({}, {}) cannot close with the player or an enemy in it", worldX, worldY);
            hintSink.accept(HINT_IN_THE_WAY);
            return false;
        }
        boolean interacted = building.onInteract(targetTile);

        if (interacted && building instanceof Container container) {
            containerManager.openContainer(container);
            log.debug("Opened container: {}", building.getClass().getSimpleName());
        }
        return interacted;
    }

    /**
     * Validates global conditions for building placement.
     *
     * @return true if all preconditions are met, false otherwise
     */
    private boolean validatePlacementPreconditions(int worldX, int worldY) {
        if (!buildingModeActive || selectedBuildingType == null) {
            log.debug("Placement failed: Building mode inactive or no type selected");
            return false;
        }

        var tileMap = gameState.getCurrentMap();
        var player = gameState.getPlayer();

        if (tileMap == null || player == null) {
            log.warn("Placement failed: Map or player is null");
            return false;
        }

        var targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null) {
            log.debug("Placement failed: Invalid tile coordinates");
            return false;
        }

        if (targetTile.hasBuilding()) {
            log.debug("Placement failed: Tile occupied");
            return false;
        }

        if (recipeManager.getBuildingRecipeByType(selectedBuildingType).isEmpty()) {
            log.debug("Placement failed: No recipe for {}", selectedBuildingType.getDisplayName());
            return false;
        }
        return true;
    }

    /**
     * Tells whether a piece that blocks walking would be put on a tile where the player or a living enemy stands or is
     * stepping to, which would trap it inside the piece.
     */
    private boolean isBlockedByEntity(BuildingType type, int tileX, int tileY) {
        return blocksWalking(type) && isEntityOn(tileX, tileY);
    }

    /**
     * Tells whether a piece of a type blocks walking, read from a sample piece of the type. This assumes that solidity
     * depends on the type alone, not on the state of the piece. The one piece whose solidity changes is the gate, which
     * is created closed and so counts as blocking here; opening it is always allowed, and closing it on someone is
     * refused separately by {@link #interactWithBuilding}.
     */
    private boolean blocksWalking(BuildingType type) {
        return blocksWalking.computeIfAbsent(type, t -> BuildingFactory.create(t, 0, 0).isSolid());
    }

    /**
     * Tells whether the player or a living enemy stands on a tile or is stepping to it: the tile an entity stands on,
     * the tile it has reserved as its target and, for the player, the next tile of its walk.
     */
    private boolean isEntityOn(int tileX, int tileY) {
        final var player = gameState.getPlayer();
        if (player != null && !player.isDead()) {
            final var next = player.getNextTile();
            if (holds(player, tileX, tileY) || (next != null && next.x == tileX && next.y == tileY)) {
                return true;
            }
        }
        for (var enemy : gameState.getEnemies()) {
            if (!enemy.isDead() && holds(enemy, tileX, tileY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a tile is the one the player stands on or any tile the player's walk still has to enter. Taking
     * the floor out from under any of them would leave the player in the water.
     */
    private boolean isOnPlayerWalk(int tileX, int tileY) {
        final var player = gameState.getPlayer();
        if (player == null || player.isDead()) {
            return false;
        }
        final boolean standsThere = player.getCurrentTileX() == tileX && player.getCurrentTileY() == tileY;
        return standsThere || player.walkPassesThrough(tileX, tileY);
    }

    private static boolean holds(Entity entity, int tileX, int tileY) {
        return (entity.getCurrentTileX() == tileX && entity.getCurrentTileY() == tileY)
                || (entity.getTargetTileX() == tileX && entity.getTargetTileY() == tileY);
    }

    /**
     * Checks if player has required resources for a recipe.
     *
     * @param player Player whose inventory to check
     * @param recipe Recipe to validate against
     * @return true if player has sufficient resources
     */
    private boolean playerHasResources(Player player, BuildingRecipe recipe) {
        if (player == null || recipe == null) {
            return false;
        }

        for (CraftingIngredient ingredient : recipe.ingredients()) {
            if (!player.canTakeItem(new Item(ingredient.itemType(), 1), ingredient.quantity())) {
                log.debug("Missing resource: {}x {}", ingredient.quantity(), ingredient.itemType().getDisplayName());
                return false;
            }
        }
        return true;
    }

    /**
     * Consumes recipe resources from player's inventory.
     * Precondition: playerHasResources() must return true.
     */
    private void consumeResources(Player player, BuildingRecipe recipe) {
        if (player == null) {
            return;
        }

        for (CraftingIngredient ingredient : recipe.ingredients()) {
            player.removeItem(new Item(ingredient.itemType(), 1), ingredient.quantity());
            log.debug("Consumed: {}x {}", ingredient.quantity(), ingredient.itemType().getDisplayName());
        }
    }

    /**
     * Determines if a building type can be placed on a tile.
     *
     * @param type Building type to check
     * @param tile Target tile for placement
     * @return true if placement is valid according to game rules
     */
    public boolean canPlaceOnTile(BuildingType type, Tile tile) {
        // Nothing is built on a resource or on a piece that already stands there
        if (tile == null || tile.hasResource() || tile.hasBuilding()) {
            return false;
        }

        return switch (type) {
            case BRIDGE -> canPlaceBridge(type, tile);
            // The torch is the one piece that also stands on a floor
            case STANDING_TORCH -> tile.hasFloor() || isWalkableLand(tile);
            // Every other piece stands on land and never on a floor
            case FENCE, FENCE_GATE, CHEST, FURNACE, CAMPFIRE, ANVIL, WORKBENCH, BED, SLEEPING_BAG -> canPlaceOnLand(type, tile);
        };
    }

    /**
     * A bridge is a floor over water that is next to land or to another bridge, so it grows out from the shore one
     * tile at a time. Diagonal neighbours do not count: they share no edge to walk across.
     */
    private boolean canPlaceBridge(BuildingType type, Tile tile) {
        boolean valid = !tile.hasFloor() && tile.getType().isWater() && touchesLandOrFloor(tile);
        if (!valid) {
            log.debug("Invalid tile for {}: needs free water that shares an edge with land or a bridge",
                    type.getDisplayName());
        }
        return valid;
    }

    private static boolean canPlaceOnLand(BuildingType type, Tile tile) {
        boolean valid = !tile.hasFloor() && isWalkableLand(tile);
        if (!valid) {
            log.debug("Invalid terrain for {}: Requires walkable land", type.getDisplayName());
        }
        return valid;
    }

    private static boolean isWalkableLand(Tile tile) {
        return tile.getType().isLand() && tile.getType().isWalkable();
    }

    /**
     * Tells whether a tile shares an edge with land or a bridge. The four neighbours across an edge count, the
     * four diagonal ones do not.
     */
    private boolean touchesLandOrFloor(Tile tile) {
        final var tileMap = gameState.getCurrentMap();
        if (tileMap == null) {
            return false;
        }
        for (int[] edge : EDGE_NEIGHBOURS) {
            final var neighbour = tileMap.getTile(tile.getX() + edge[0], tile.getY() + edge[1]);
            if (neighbour != null && (neighbour.getType().isLand() || neighbour.hasFloor())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates placement preview color for a tile.
     *
     * @return Color indicating placement feasibility or null when inactive
     */
    public Rgba getPlacementPreviewColor(int worldX, int worldY) {
        if (!buildingModeActive) {
            return null;
        }
        if (selectedBuildingType == null) {
            return Rgba.YELLOW.withOpacityFactor(PREVIEW_OPACITY);
        }

        var tileMap = gameState.getCurrentMap();
        var player = gameState.getPlayer();
        if (tileMap == null || player == null) {
            return errorColor();
        }

        var targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null || targetTile.hasBuilding()) {
            return errorColor();
        }

        Optional<BuildingRecipe> recipe = recipeManager.getBuildingRecipeByType(selectedBuildingType);
        if (recipe.isEmpty() || isPlacementBlocked(targetTile, worldX, worldY)) {
            return errorColor();
        }
        if (!playerHasResources(player, recipe.orElse(null))) {
            return Rgba.ORANGE.withOpacityFactor(PREVIEW_OPACITY);
        }

        return Rgba.LIME_GREEN.withOpacityFactor(PREVIEW_OPACITY);
    }

    private boolean isPlacementBlocked(Tile targetTile, int worldX, int worldY) {
        return !canPlaceOnTile(selectedBuildingType, targetTile) || isBlockedByEntity(selectedBuildingType, worldX, worldY);
    }

    /**
     * Returns standardized error color for placement preview
     */
    private static Rgba errorColor() {
        return Rgba.RED.withOpacityFactor(PREVIEW_OPACITY);
    }

}
