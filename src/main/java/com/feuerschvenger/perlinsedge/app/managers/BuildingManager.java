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

    // Building mode state
    @Getter
    private BuildingType selectedBuildingType;
    @Getter
    private boolean buildingModeActive;

    public BuildingManager(GameStateManager gameState, RecipeManager recipeManager, ContainerManager containerManager) {
        this.gameState = gameState;
        this.recipeManager = recipeManager;
        this.containerManager = containerManager;
        this.buildingModeActive = false;
        this.selectedBuildingType = null;
    }

    /**
     * Toggles building mode on/off. Resets selection when deactivated.
     */
    public void toggleBuildingMode() {
        if (buildingModeActive) {
            deactivateBuildingMode();
        } else {
            activateBuildingMode();
        }
    }

    /**
     * Activates building mode.
     */
    public void activateBuildingMode() {
        if (!buildingModeActive) {
            buildingModeActive = true;
            log.info("Building mode: ON");
        }
    }

    /**
     * Deactivates building mode and clears any selected building type.
     */
    public void deactivateBuildingMode() {
        if (buildingModeActive) {
            buildingModeActive = false;
            selectedBuildingType = null;
            log.info("Building mode: OFF");
        }
    }

    /**
     * Sets the currently selected building type for placement.
     *
     * @param type Building type to select (null to clear selection)
     */
    public void setSelectedBuildingType(BuildingType type) {
        selectedBuildingType = type;
        if (type != null) {
            log.debug("Selected building type: {}", type.getDisplayName());
        } else {
            log.debug("Building type selection cleared.");
        }
    }

    /**
     * Places the selected building at specified coordinates after validating conditions.
     *
     * @param worldX X-coordinate in world units
     * @param worldY Y-coordinate in world units
     */
    public void placeBuilding(int worldX, int worldY) {
        // Validate preconditions
        if (!validatePlacementPreconditions(worldX, worldY)) {
            return;
        }

        TileMap tileMap = gameState.getCurrentMap();
        Player player = gameState.getPlayer();
        Tile targetTile = tileMap.getTile(worldX, worldY);
        Optional<BuildingRecipe> recipe = recipeManager.getBuildingRecipeByType(selectedBuildingType);
        BuildingRecipe existingRecipe = recipe.orElse(null);

        // Validate resources and placement rules
        if (!playerHasResources(player, existingRecipe) || !canPlaceOnTile(selectedBuildingType, targetTile)) {
            return;
        }

        // Create and place building
        Building newBuilding = createBuildingInstance(selectedBuildingType, worldX, worldY);
        if (newBuilding == null) return;

        consumeResources(player, existingRecipe);
        targetTile.setBuilding(newBuilding);
        log.info("Placed {} at ({}, {})", selectedBuildingType.getDisplayName(), worldX, worldY);
    }

    /**
     * Removes a building from specified coordinates if destructible.
     *
     * @param worldX X-coordinate in world units
     * @param worldY Y-coordinate in world units
     */
    public void removeBuilding(int worldX, int worldY) {
        TileMap tileMap = gameState.getCurrentMap();
        if (tileMap == null) {
            log.warn("Remove building failed: Map is null");
            return;
        }

        Tile targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null || !targetTile.hasBuilding()) {
            log.debug("No building at ({}, {})", worldX, worldY);
            return;
        }

        Building building = targetTile.getBuilding();
        if (!building.isDestructible()) {
            log.debug("Building not destructible: {}", building.getType().getDisplayName());
            return;
        }

        targetTile.setBuilding(null);
        log.info("Removed {} from ({}, {})", building.getType().getDisplayName(), worldX, worldY);
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
     * Factory method for building creation.
     *
     * @return Building instance or null for unknown types
     */
    private Building createBuildingInstance(BuildingType type, int x, int y) {
        return switch (type) {
            case WALL -> new Wall(x, y);
            case DOOR -> new Door(x, y);
            case BRIDGE -> new Bridge(x, y);
            case DOCK -> new Dock(x, y);
            case FENCE -> new Fence(x, y);
            case CHEST -> new Chest(x, y, 9);
            case FURNACE -> new Furnace(x, y);
            case CAMPFIRE -> new Campfire(x, y);
            case STANDING_TORCH -> new StandingTorch(x, y);
            case ANVIL -> new Anvil(x, y);
            case WORKBENCH -> new Workbench(x, y);
            case BED -> new Bed(x, y);
            default -> {
                log.warn("Unknown BuildingType: {}", type);
                yield null;
            }
        };
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
            case WALL, DOOR, FENCE, CHEST, FURNACE, CAMPFIRE, STANDING_TORCH, ANVIL, WORKBENCH, BED -> {
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