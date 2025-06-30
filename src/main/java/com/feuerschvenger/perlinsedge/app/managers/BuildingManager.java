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
import javafx.scene.paint.Color;

import java.util.Optional;

/**
 * Manages building operations including placement, removal, interaction, and preview visualization.
 * Handles resource validation, tile suitability checks, and building lifecycle management.
 */
public class BuildingManager {
    // Dependencies
    private final GameStateManager gameState;
    private final RecipeManager recipeManager;
    private final ContainerManager containerManager;

    // Building mode state
    private BuildingType selectedBuildingType;
    private boolean buildingModeActive;

    public BuildingManager(GameStateManager gameState, RecipeManager recipeManager, ContainerManager containerManager) {
        this.gameState = gameState;
        this.recipeManager = recipeManager;
        this.containerManager = containerManager;
        this.buildingModeActive = false;
        this.selectedBuildingType = null;
    }

    public boolean isBuildingModeActive() {
        return buildingModeActive;
    }

    public BuildingType getSelectedBuildingType() {
        return selectedBuildingType;
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
            System.out.println("Building mode: ON");
        }
    }

    /**
     * Deactivates building mode and clears any selected building type.
     */
    public void deactivateBuildingMode() {
        if (buildingModeActive) {
            buildingModeActive = false;
            selectedBuildingType = null;
            System.out.println("Building mode: OFF");
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
            System.out.println("Selected building type: " + type.getDisplayName());
        } else {
            System.out.println("Building type selection cleared.");
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
        System.out.println("Placed " + selectedBuildingType.getDisplayName() + " at (" + worldX + ", " + worldY + ")");
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
            System.err.println("Remove building failed: Map is null");
            return;
        }

        Tile targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null || !targetTile.hasBuilding()) {
            System.out.println("No building at (" + worldX + ", " + worldY + ")");
            return;
        }

        Building building = targetTile.getBuilding();
        if (!building.isDestructible()) {
            System.out.println("Building not destructible: " + building.getType().getDisplayName());
            return;
        }

        targetTile.setBuilding(null);
        System.out.println("Removed " + building.getType().getDisplayName() + " from (" + worldX + ", " + worldY + ")");
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
            System.err.println("Interaction failed: Map is null");
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
            System.out.println("Opened container: " + building.getClass().getSimpleName());
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
            System.out.println("Placement failed: Building mode inactive or no type selected");
            return false;
        }

        TileMap tileMap = gameState.getCurrentMap();
        Player player = gameState.getPlayer();

        if (tileMap == null || player == null) {
            System.err.println("Placement failed: Map or player is null");
            return false;
        }

        Tile targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null) {
            System.out.println("Placement failed: Invalid tile coordinates");
            return false;
        }

        if (targetTile.hasBuilding()) {
            System.out.println("Placement failed: Tile occupied");
            return false;
        }

        if (recipeManager.getBuildingRecipeByType(selectedBuildingType).isEmpty()) {
            System.out.println("Placement failed: No recipe for " + selectedBuildingType.getDisplayName());
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
                System.out.println("Missing resource: " + ingredient.quantity() + "x " + ingredient.itemType().getDisplayName());
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
            System.out.println("Consumed: " + ingredient.quantity() + "x " + ingredient.itemType().getDisplayName());
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
            case ANVIL -> new Anvil(x, y);
            case WORKBENCH -> new Workbench(x, y);
            case BED -> new Bed(x, y);
            default -> {
                System.err.println("Unknown BuildingType: " + type);
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
            case WALL, DOOR, FENCE, CHEST, FURNACE, CAMPFIRE, ANVIL, WORKBENCH, BED -> {
                boolean validLand = tile.getType().isLand() && tile.getType().isWalkable();
                if (!validLand) {
                    System.out.println("Invalid terrain for " + type.getDisplayName() + ": Requires walkable land");
                }
                return validLand;
            }

            // Water-based structures
            case BRIDGE, DOCK -> {
                boolean validWater = tile.getType().isWater();
                if (!validWater) {
                    System.out.println("Invalid terrain for " + type.getDisplayName() + ": Requires water");
                }
                return validWater;
            }

            default -> {
                System.out.println("Unhandled building type: " + type);
                return false;
            }
        }
    }

    /**
     * Generates placement preview color for a tile.
     *
     * @return Color indicating placement feasibility or null when inactive
     */
    public Color getPlacementPreviewColor(int worldX, int worldY) {
        if (!buildingModeActive) return null;
        if (selectedBuildingType == null) return Color.YELLOW.deriveColor(0, 1, 1, 0.7);

        TileMap tileMap = gameState.getCurrentMap();
        Player player = gameState.getPlayer();
        if (tileMap == null || player == null) return errorColor();

        Tile targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null || targetTile.hasBuilding()) return errorColor();

        Optional<BuildingRecipe> recipe = recipeManager.getBuildingRecipeByType(selectedBuildingType);
        if (recipe.isEmpty()) return errorColor();

        if (!canPlaceOnTile(selectedBuildingType, targetTile)) return errorColor();
        if (!playerHasResources(player, recipe.orElse(null))) return Color.ORANGE.deriveColor(0, 1, 1, 0.7);

        return Color.LIMEGREEN.deriveColor(0, 1, 1, 0.7);
    }

    /**
     * Returns standardized error color for placement preview
     */
    private Color errorColor() {
        return Color.RED.deriveColor(0, 1, 1, 0.7);
    }

}