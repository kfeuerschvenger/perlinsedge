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

/**
 * Manages the placement, removal, and interaction with buildings in the game world.
 */
public class BuildingManager {
    private final GameStateManager gameState; // Now depends on GameStateManager
    private final RecipeManager recipeManager;
    private final ContainerManager containerManager;

    // State for building mode
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

    public void toggleBuildingMode() {
        this.buildingModeActive = !this.buildingModeActive;
        // Reset selected building when toggling off
        if (!this.buildingModeActive) {
            this.selectedBuildingType = null;
        }
        System.out.println("Building mode: " + (buildingModeActive ? "ON" : "OFF"));
    }

    public BuildingType getSelectedBuildingType() {
        return selectedBuildingType;
    }

    public void setSelectedBuildingType(BuildingType type) {
        this.selectedBuildingType = type;
        if (type != null) {
            System.out.println("Selected building type: " + type.getDisplayName());
        } else {
            System.out.println("Building type selection cleared.");
        }
    }

    /**
     * Attempts to place the currently selected building type at the given world coordinates.
     * @param worldX The world X coordinate of the tile.
     * @param worldY The world Y coordinate of the tile.
     * @return True if the building was successfully placed, false otherwise.
     */
    public boolean placeBuilding(int worldX, int worldY) {
        if (!buildingModeActive || selectedBuildingType == null) {
            System.out.println("Cannot place building: Building mode not active or no building type selected.");
            return false;
        }

        TileMap tileMap = gameState.getCurrentMap(); // Get map from GameStateManager
        Player player = gameState.getPlayer(); // Get player from GameStateManager

        if (tileMap == null || player == null) {
            System.err.println("Cannot place building: Map or Player is null.");
            return false;
        }

        Tile targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null) {
            System.out.println("Cannot place building: Invalid tile coordinates.");
            return false;
        }

        if (targetTile.hasBuilding()) {
            System.out.println("Cannot place building: Tile already has a building.");
            return false;
        }

        BuildingRecipe recipe = recipeManager.getBuildingRecipeByType(selectedBuildingType);
        if (recipe == null) {
            System.out.println("Cannot place building: No recipe found for " + selectedBuildingType.getDisplayName());
            return false;
        }

        // 1. Check if player has required resources
        if (!playerHasResources(player, recipe)) { // Pass player
            System.out.println("Cannot place building: Not enough resources.");
            return false;
        }

        // 2. Check specific tile placement rules (more detailed than Tile.isBuildable)
        if (!canPlaceOnTile(selectedBuildingType, targetTile)) {
            System.out.println("Cannot place building: Invalid terrain for " + selectedBuildingType.getDisplayName());
            return false;
        }

        // 3. Create the building instance
        Building newBuilding = createBuildingInstance(selectedBuildingType, worldX, worldY);
        if (newBuilding == null) {
            System.out.println("Failed to create building instance.");
            return false;
        }

        // 4. Consume resources from player's inventory
        consumeResources(player, recipe); // Pass player

        // 5. Place the building on the tile
        targetTile.setBuilding(newBuilding);
        System.out.println("Successfully placed " + selectedBuildingType.getDisplayName() + " at (" + worldX + ", " + worldY + ")");
        return true;
    }

    /**
     * Attempts to remove a building from the given world coordinates.
     * @param worldX The world X coordinate of the tile.
     * @param worldY The world Y coordinate of the tile.
     * @return True if the building was successfully removed, false otherwise.
     */
    public boolean removeBuilding(int worldX, int worldY) {
        TileMap tileMap = gameState.getCurrentMap(); // Get map from GameStateManager
        if (tileMap == null) {
            System.err.println("Cannot remove building: Map is null.");
            return false;
        }

        Tile targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null || !targetTile.hasBuilding()) {
            System.out.println("No building to remove at (" + worldX + ", " + worldY + ").");
            return false;
        }

        Building buildingToRemove = targetTile.getBuilding();
        if (!buildingToRemove.isDestructible()) {
            System.out.println("Building " + buildingToRemove.getType().getDisplayName() + " at (" + worldX + ", " + worldY + ") is not destructible.");
            return false;
        }

        // Optional: Return some resources to the player upon destruction?
        // For simplicity, we'll just destroy it for now.

        targetTile.setBuilding(null); // Remove the building from the tile
        System.out.println("Successfully removed " + buildingToRemove.getType().getDisplayName() + " from (" + worldX + ", " + worldY + ").");
        return true;
    }

    /**
     * Checks if the player has all the required ingredients for a given building recipe.
     * @param player The player entity.
     * @param recipe The BuildingRecipe to check.
     * @return True if the player has all ingredients, false otherwise.
     */
    private boolean playerHasResources(Player player, BuildingRecipe recipe) { // Now accepts Player
        if (player == null) return false;
        for (CraftingIngredient ingredient : recipe.ingredients()) {
            // Player (as Container) must have at least the required quantity of each item type
            if (!player.canTakeItem(new Item(ingredient.getItemType(), 1), ingredient.getQuantity())) { // Direct call on player
                System.out.println("Missing: " + ingredient.getQuantity() + "x " + ingredient.getItemType().getDisplayName());
                return false;
            }
        }
        return true;
    }

    /**
     * Consumes the required resources from the player's inventory based on the recipe.
     * Assumes playerHasResources() has already returned true.
     * @param player The player entity.
     * @param recipe The BuildingRecipe whose ingredients to consume.
     */
    private void consumeResources(Player player, BuildingRecipe recipe) { // Now accepts Player
        if (player == null) return;
        for (CraftingIngredient ingredient : recipe.ingredients()) {
            player.removeItem(new Item(ingredient.getItemType(), 1), ingredient.getQuantity()); // Direct call on player
            System.out.println("Consumed: " + ingredient.getQuantity() + "x " + ingredient.getItemType().getDisplayName());
        }
    }

    /**
     * Creates an instance of a Building based on its type.
     * This uses a simple factory pattern.
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
            // case STANDING_TORCH -> new StandingTorch(x, y);
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
     * Determines if a specific building type can be placed on a given tile.
     * This contains specific game rules for placement (e.g., docks only on water).
     * @param type The BuildingType to check.
     * @param tile The Tile to place the building on.
     * @return True if placement is valid, false otherwise.
     */
    public boolean canPlaceOnTile(BuildingType type, Tile tile) {
        // Basic check: Tile must be buildable (no resource, no existing building)
        if (!tile.isBuildable()) {
            return false;
        }

        // Specific rules based on building type
        switch (type) {
            case WALL:
            case DOOR:
            case FENCE:
            case CHEST:
            case FURNACE:
            case STANDING_TORCH:
            case CAMPFIRE:
            case ANVIL:
            case WORKBENCH:
            case BED:
                // These generally need to be placed on walkable land tiles
                boolean canPlaceOnLand = tile.getType().isLand() && tile.getType().isWalkable();
                if (!canPlaceOnLand) {
                    System.out.println("Cannot place " + type.getDisplayName() + ": Must be placed on walkable land tile.");
                }
                return canPlaceOnLand;
            case BRIDGE:
                // Bridges must be placed on water tiles
                boolean canPlaceOnWaterBridge = tile.getType().isWater();
                if (!canPlaceOnWaterBridge) {
                    System.out.println("Cannot place " + type.getDisplayName() + ": Must be placed on water tile.");
                }
                return canPlaceOnWaterBridge;
            case DOCK:
                // Docks must be placed on water tiles
                boolean canPlaceOnWaterDock = tile.getType().isWater();
                if (!canPlaceOnWaterDock) {
                    System.out.println("Cannot place " + type.getDisplayName() + ": Must be placed on water tile.");
                }
                return canPlaceOnWaterDock;
            default:
                System.out.println("Cannot place " + type.getDisplayName() + ": Unknown building type for placement rules.");
                return false; // Unknown type, cannot place
        }
    }

    /**
     * Gets the preview color for a building placement.
     * @param worldX The world X coordinate of the tile.
     * @param worldY The world Y coordinate of the tile.
     * @return Green if placable, Red if not, Yellow if no type selected.
     */
    public Color getPlacementPreviewColor(int worldX, int worldY) {
        if (!buildingModeActive) {
            return null;
        }
        if (selectedBuildingType == null) {
            return Color.YELLOW.deriveColor(0, 1, 1, 0.7);
        }

        TileMap tileMap = gameState.getCurrentMap(); // Get map from GameStateManager
        Player player = gameState.getPlayer(); // Get player from GameStateManager

        if (tileMap == null || player == null) {
            return Color.RED.deriveColor(0, 1, 1, 0.7); // Cannot determine preview if map or player is null
        }

        Tile targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null) {
            return Color.RED.deriveColor(0, 1, 1, 0.7); // Invalid tile
        }

        // Check placement rules without consuming resources
        if (targetTile.hasBuilding()) {
            return Color.RED.deriveColor(0, 1, 1, 0.7); // Already has building
        }

        if (!canPlaceOnTile(selectedBuildingType, targetTile)) {
            return Color.RED.deriveColor(0, 1, 1, 0.7); // Invalid terrain
        }

        BuildingRecipe recipe = recipeManager.getBuildingRecipeByType(selectedBuildingType);
        // Pass player to playerHasResources
        if (recipe == null || !playerHasResources(player, recipe)) {
            return Color.ORANGE.deriveColor(0, 1, 1, 0.7); // Not enough resources or no recipe
        }

        return Color.LIMEGREEN.deriveColor(0, 1, 1, 0.7); // All good, can place
    }

    /**
     * Handles interaction with a building on a specific tile.
     * @param worldX The world X coordinate of the tile.
     * @param worldY The world Y coordinate of the tile.
     * @return True if an interaction occurred, false otherwise.
     */
    public boolean interactWithBuilding(int worldX, int worldY) {
        TileMap tileMap = gameState.getCurrentMap();
        if (tileMap == null) {
            System.err.println("Cannot interact with building: Map is null.");
            return false;
        }

        Tile targetTile = tileMap.getTile(worldX, worldY);
        if (targetTile == null || !targetTile.hasBuilding()) {
            return false;
        }

        Building building = targetTile.getBuilding();
        boolean interacted = building.onInteract(targetTile);

        // Abrir contenedor si es un cofre
        if (interacted && building instanceof Container) {
            containerManager.openContainer((Container) building);
            System.out.println("Container opened: " + building.getClass().getSimpleName());
        }

        return interacted;
    }

    // You might want methods to get lists of buildings, save/load buildings, etc.
}
