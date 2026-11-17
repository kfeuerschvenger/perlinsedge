package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.*;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages building operations including placement, removal, interaction, and preview visualization.
 * Handles resource validation, tile suitability checks, and building lifecycle management.
 */
@Slf4j
public class BuildingManager {
    private static final double PREVIEW_OPACITY = 0.7;

    // Dependencies
    private final GameStateManager gameState;
    private final RecipeManager recipeManager;
    private final ContainerManager containerManager;
    private final ItemDropManager itemDropManager;
    private final Runnable onInventoryChanged;

    // Building mode state
    @Getter
    private BuildingType selectedBuildingType;
    /** The piece chosen last, which comes back as the selection when the hammer is picked up again. */
    private BuildingType lastBuildingType;
    /** The rotation chosen for each piece type, kept while the hammer stays in hand. */
    private final Map<BuildingType, Integer> rotations = new EnumMap<>(BuildingType.class);
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
        TileMap tileMap = gameState.getCurrentMap();
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

        TileMap tileMap = gameState.getCurrentMap();
        Player player = gameState.getPlayer();
        Tile targetTile = tileMap.getTile(worldX, worldY);
        Optional<BuildingRecipe> recipe = recipeManager.getBuildingRecipeByType(selectedBuildingType);
        BuildingRecipe existingRecipe = recipe.orElse(null);

        // Validate resources and placement rules
        if (!playerHasResources(player, existingRecipe) || !canPlaceOnTile(selectedBuildingType, targetTile)) {
            return false;
        }

        Building newBuilding = BuildingFactory.create(selectedBuildingType, worldX, worldY,
                getRotation(selectedBuildingType));
        consumeResources(player, existingRecipe);
        targetTile.setBuilding(newBuilding);
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
        TileMap tileMap = gameState.getCurrentMap();
        if (tileMap == null) {
            log.warn("Remove building failed: Map is null");
            return false;
        }

        Tile targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null || !targetTile.hasBuilding()) {
            log.debug("No building at ({}, {})", worldX, worldY);
            return false;
        }

        Building building = targetTile.getBuilding();
        if (!building.isDestructible()) {
            log.debug("Building not destructible: {}", building.getType().getDisplayName());
            return false;
        }

        targetTile.setBuilding(null);
        refund(building);
        log.info("Removed {} from ({}, {})", building.getType().getDisplayName(), worldX, worldY);
        return true;
    }

    /**
     * Gives the player back the share of the recipe that matches the health the building has left: for each
     * ingredient {@code floor(quantity * health / maxHealth)} items. What the inventory has no room for is dropped as a
     * pile at the player's feet.
     */
    private void refund(Building building) {
        Player player = gameState.getPlayer();
        Optional<BuildingRecipe> recipe = recipeManager.getBuildingRecipeByType(building.getType());
        if (player == null || recipe.isEmpty()) return;

        for (CraftingIngredient ingredient : recipe.get().ingredients()) {
            int quantity = building.refundOf(ingredient.quantity());
            if (quantity <= 0) continue;
            Item refund = new Item(ingredient.itemType(), quantity);
            player.addItem(refund);
            if (refund.getQuantity() > 0) {
                itemDropManager.dropAtFeet(refund, player);
            }
        }
        onInventoryChanged.run();
    }

    /**
     * Handles player interaction with a building at specified coordinates.
     *
     * @param worldX X-coordinate in world units
     * @param worldY Y-coordinate in world units
     * @return true if interaction occurred, false otherwise
     */
    public boolean interactWithBuilding(int worldX, int worldY) {
        TileMap tileMap = gameState.getCurrentMap();
        if (tileMap == null) {
            log.warn("Interaction failed: Map is null");
            return false;
        }

        Tile targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null || !targetTile.hasBuilding()) {
            return false;
        }

        Building building = targetTile.getBuilding();
        boolean interacted = building.onInteract(targetTile);

        if (interacted && building instanceof Container) {
            containerManager.openContainer((Container) building);
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

        TileMap tileMap = gameState.getCurrentMap();
        Player player = gameState.getPlayer();

        if (tileMap == null || player == null) {
            log.warn("Placement failed: Map or player is null");
            return false;
        }

        Tile targetTile = tileMap.getTile(worldX, worldY);
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
     * Checks if player has required resources for a recipe.
     *
     * @param player Player whose inventory to check
     * @param recipe Recipe to validate against
     * @return true if player has sufficient resources
     */
    private boolean playerHasResources(Player player, BuildingRecipe recipe) {
        if (player == null || recipe == null) return false;

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
        if (player == null) return;

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
        if (!tile.isBuildable()) return false;

        switch (type) {
            // Land-based structures
            case FENCE, CHEST, FURNACE, CAMPFIRE, STANDING_TORCH, ANVIL, WORKBENCH, BED -> {
                boolean validLand = tile.getType().isLand() && tile.getType().isWalkable();
                if (!validLand) {
                    log.debug("Invalid terrain for {}: Requires walkable land", type.getDisplayName());
                }
                return validLand;
            }

            // Water-based structures
            case BRIDGE, DOCK -> {
                boolean validWater = tile.getType().isWater();
                if (!validWater) {
                    log.debug("Invalid terrain for {}: Requires water", type.getDisplayName());
                }
                return validWater;
            }

            default -> {
                log.warn("Unhandled building type: {}", type);
                return false;
            }
        }
    }

    /**
     * Generates placement preview color for a tile.
     *
     * @return Color indicating placement feasibility or null when inactive
     */
    public Rgba getPlacementPreviewColor(int worldX, int worldY) {
        if (!buildingModeActive) return null;
        if (selectedBuildingType == null) return Rgba.YELLOW.withOpacityFactor(PREVIEW_OPACITY);

        TileMap tileMap = gameState.getCurrentMap();
        Player player = gameState.getPlayer();
        if (tileMap == null || player == null) return errorColor();

        Tile targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null || targetTile.hasBuilding()) return errorColor();

        Optional<BuildingRecipe> recipe = recipeManager.getBuildingRecipeByType(selectedBuildingType);
        if (recipe.isEmpty()) return errorColor();

        if (!canPlaceOnTile(selectedBuildingType, targetTile)) return errorColor();
        if (!playerHasResources(player, recipe.orElse(null))) return Rgba.ORANGE.withOpacityFactor(PREVIEW_OPACITY);

        return Rgba.LIME_GREEN.withOpacityFactor(PREVIEW_OPACITY);
    }

    /**
     * Returns standardized error color for placement preview
     */
    private Rgba errorColor() {
        return Rgba.RED.withOpacityFactor(PREVIEW_OPACITY);
    }

}