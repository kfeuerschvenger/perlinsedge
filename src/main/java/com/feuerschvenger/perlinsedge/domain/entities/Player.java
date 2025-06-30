package com.feuerschvenger.perlinsedge.domain.entities;

import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.pathfinding.Pathfinder;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.*;

/**
 * Represents the player character with movement, inventory, and combat capabilities.
 * Handles pathfinding, movement interpolation, inventory management, and resource interaction.
 */
public class Player extends Entity implements Container {
    // ==================================================================
    //  Constants
    // ==================================================================

    // Animation configuration
    private static final double ANIMATION_FRAME_DURATION = 0.1;
    private static final int IDLE_FRAME = 1;
    private static final int WALKING_FRAME_START = 0;
    private static final int WALKING_FRAME_END = 2;
    public static final double RENDER_OFFSET_Y = 0;

    // Health configuration
    private static final double MAX_HEALTH = 100.0;

    // Combat configuration
    private static final double DEFAULT_ATTACK_RANGE = 1.5;     // In tiles
    private static final double DEFAULT_ATTACK_DAMAGE = 10.0;   // Base damage per attack
    private static final double DEFAULT_ATTACK_COOLDOWN = 0.8;  // Time between attacks
    private static final double ATTACK_ANIMATION_DURATION = 0.3;

    // Movement configuration
    private static final double MOVE_SPEED = 5.0;

    // Inventory configuration
    private static final int INVENTORY_CAPACITY = 18;

    // ==================================================================
    //  Instance Variables
    // ==================================================================

    // Combat state
    private double attackRange = DEFAULT_ATTACK_RANGE;
    private double attackDamage = DEFAULT_ATTACK_DAMAGE;
    private double attackCooldown = DEFAULT_ATTACK_COOLDOWN;
    private double attackTimer = 0;
    private boolean isAttacking = false;
    private double attackAnimationTime = 0;
    private int attackDirection = -1;

    // Inventory
    private final List<Item> inventory = new ArrayList<>(Collections.nCopies(INVENTORY_CAPACITY, null));

    // Movement and pathfinding
    private final LinkedList<PathNode> currentPath = new LinkedList<>();

    // Animation state
    private int direction = DIR_UP;
    private int animationFrame = IDLE_FRAME;
    private double animationTimer = 0;

    // ==================================================================
    //  Constructor
    // ==================================================================

    public Player(int startX, int startY) {
        super(startX, startY, MAX_HEALTH);
    }

    // ==================================================================
    //  Public API - Combat
    // ==================================================================

    /**
     * Initiates an attack in the specified direction.
     *
     * @param direction The attack direction (using DIR_* constants)
     */
    public void startAttack(int direction) {
        if (attackTimer <= 0 && !isDead) {
            isAttacking = true;
            attackTimer = attackCooldown;
            attackAnimationTime = 0;
            attackDirection = direction;
        }
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    public int getAttackDirection() {
        return attackDirection;
    }

    public double getAttackAnimationProgress() {
        return Math.min(1.0, attackAnimationTime / ATTACK_ANIMATION_DURATION);
    }

    public double[] getAttackPosition() {
        double[] pos = getInterpolatedPosition(0);
        double x = pos[0];
        double y = pos[1];

        // Calculate attack position based on direction
        switch (attackDirection) {
            case DIR_UP:         return new double[]{x, y - 1};
            case DIR_UP_RIGHT:   return new double[]{x + 0.7, y - 0.7};
            case DIR_RIGHT:      return new double[]{x + 1, y};
            case DIR_DOWN_RIGHT: return new double[]{x + 0.7, y + 0.7};
            case DIR_DOWN:       return new double[]{x, y + 1};
            case DIR_DOWN_LEFT:  return new double[]{x - 0.7, y + 0.7};
            case DIR_LEFT:       return new double[]{x - 1, y};
            case DIR_UP_LEFT:    return new double[]{x - 0.7, y - 0.7};
            default:             return new double[]{x, y};
        }
    }

    // ==================================================================
    //  Public API - Movement
    // ==================================================================

    /**
     * Initiates movement to a target position using pathfinding.
     *
     * @param map     The game world tile map
     * @param targetX Target X coordinate
     * @param targetY Target Y coordinate
     */
    public void moveTo(TileMap map, int targetX, int targetY) {
        // Skip if already at target position
        if (isAtPosition(targetX, targetY)) return;

        Pathfinder pathfinder = new Pathfinder(map);
        List<PathNode> path = pathfinder.findPath(currentTileX, currentTileY, targetX, targetY);

        if (path == null || path.isEmpty()) {
            clearPath();
            return;
        }

        startNewPath(path);
    }

    public boolean isMoving() {
        return !isAtPosition(targetTileX, targetTileY) || !currentPath.isEmpty();
    }

    // ==================================================================
    //  Public API - Inventory Management
    // ==================================================================

    public void clearInventory() {
        for (int i = 0; i < INVENTORY_CAPACITY; i++) {
            inventory.set(i, null);
        }
    }

    public void removeItemFromSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < INVENTORY_CAPACITY) {
            inventory.set(slotIndex, null);
        }
    }

    /**
     * Attempts to pick up an item from the ground.
     *
     * @param item The item to pick up
     * @return The quantity actually picked up
     */
    public int pickUpItem(Item item) {
        if (item == null || item.getQuantity() <= 0) return 0;

        int quantityBeforePickup = item.getQuantity();
        boolean added = addItem(item);

        int quantityActuallyPickedUp = quantityBeforePickup - item.getQuantity();

        if (added && quantityActuallyPickedUp > 0) {
            System.out.println("Player picked up " + quantityActuallyPickedUp + "x " + item.getDisplayName());
        }
        return quantityActuallyPickedUp;
    }

    /**
     * Moves an item within the inventory.
     *
     * @param draggedItem The item being moved
     * @param targetSlotIndex The destination slot index
     */
    public void moveItemInInventory(Item draggedItem, int targetSlotIndex) {
        if (targetSlotIndex < 0 || targetSlotIndex >= INVENTORY_CAPACITY) {
            return;
        }

        // Find source slot of dragged item
        int sourceSlot = findItemSlot(draggedItem);
        if (sourceSlot == -1) {
            System.err.println("Error: Item not found in inventory");
            return;
        }

        Item targetItem = inventory.get(targetSlotIndex);

        // Case 1: Target slot is empty
        if (targetItem == null) {
            inventory.set(targetSlotIndex, draggedItem);
            inventory.set(sourceSlot, null);
            return;
        }

        // Case 2: Same item type - try to stack
        if (canStack(targetItem, draggedItem)) {
            int spaceInTarget = targetItem.getType().getMaxStackSize() - targetItem.getQuantity();
            int transferAmount = Math.min(spaceInTarget, draggedItem.getQuantity());

            if (transferAmount > 0) {
                targetItem.addQuantity(transferAmount);
                draggedItem.removeQuantity(transferAmount);

                // Clear source slot if item depleted
                if (draggedItem.getQuantity() <= 0) {
                    inventory.set(sourceSlot, null);
                }
                return;
            }
        }

        // Case 3: Different items or no stacking possible - swap positions
        if (sourceSlot != targetSlotIndex) {
            inventory.set(sourceSlot, targetItem);
            inventory.set(targetSlotIndex, draggedItem);
        }
    }

    // ==================================================================
    //  Public API - Crafting
    // ==================================================================

    /**
     * Checks if the player has the required ingredients for a recipe.
     */
    public boolean canCraft(Recipe recipe) {
        for (CraftingIngredient ingredient : recipe.getIngredients()) {
            if (!canTakeItem(new Item(ingredient.itemType(), 1), ingredient.quantity())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Crafts an item from a recipe.
     */
    public void craftItem(Recipe recipe) {
        if (!canCraft(recipe)) {
            System.err.println("Cannot craft: Missing ingredients");
            return;
        }

        // Remove ingredients
        for (CraftingIngredient ingredient : recipe.getIngredients()) {
            removeItem(new Item(ingredient.itemType(), 1), ingredient.quantity());
        }

        // Add crafted item
        Item craftedItem = new Item(recipe.getOutputItemType(), recipe.getOutputQuantity());
        addItem(craftedItem);
    }

    // ==================================================================
    //  Container Interface Implementation
    // ==================================================================

    @Override
    public List<Item> getItems() {
        return new ArrayList<>(inventory); // Defensive copy
    }

    @Override
    public Item getItemAt(int slotIndex) {
        if (!isValidSlot(slotIndex)) return null;
        return inventory.get(slotIndex);
    }

    @Override
    public void setItemAt(int slotIndex, Item item) {
        if (!isValidSlot(slotIndex)) return;
        inventory.set(slotIndex, item);
    }

    @Override
    public boolean addItem(Item item) {
        if (item == null || item.getQuantity() <= 0) {
            System.out.println("Cannot add null or zero quantity item");
            return false;
        }

        int originalQuantity = item.getQuantity();
        int remaining = originalQuantity;
        // Try stacking with existing items
        for (int i = 0; i < INVENTORY_CAPACITY && remaining > 0; i++) {
            Item existing = inventory.get(i);
            if (existing != null && canStack(existing, item)) {
                int availableSpace = existing.getType().getMaxStackSize() - existing.getQuantity();
                int transfer = Math.min(availableSpace, remaining);

                existing.addQuantity(transfer);
                remaining -= transfer;
            }
        }

        // Add to empty slots
        for (int i = 0; i < INVENTORY_CAPACITY && remaining > 0; i++) {
            Item existing = inventory.get(i);
            if (existing == null) {
                int stackSize = Math.min(remaining, item.getType().getMaxStackSize());
                inventory.set(i, new Item(item.getType(), stackSize));
                remaining -= stackSize;
            }
        }

        item.setQuantity(remaining); // Update the original item with remaining quantity
        return remaining < originalQuantity; // Return true if any items were added
    }

    @Override
    public boolean removeItem(Item itemToRemove, int quantity) {
        if (itemToRemove == null || quantity <= 0) {
            System.out.println("Cannot remove null or zero quantity item");
            return false;
        }

        int remaining = quantity;
        for (int i = 0; i < INVENTORY_CAPACITY && remaining > 0; i++) {
            Item current = inventory.get(i);
            if (current != null && current.getType() == itemToRemove.getType()) {
                int toRemove = Math.min(current.getQuantity(), remaining);
                current.removeQuantity(toRemove);
                remaining -= toRemove;

                if (current.getQuantity() <= 0) {
                    inventory.set(i, null);
                }
            }
        }

        return remaining == 0; // Return true if all requested quantity was removed
    }

    @Override
    public int getItemCount(Item item) {
        if (item == null) return 0;
        return inventory.stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getType() == item.getType())
                .mapToInt(Item::getQuantity)
                .sum();
    }

    @Override
    public int getCapacity() {
        return INVENTORY_CAPACITY;
    }

    @Override
    public boolean hasSpace(Item item) {
        if (item == null) return false;

        // Check for stacking space
        boolean hasStackSpace = inventory.stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getType() == item.getType())
                .anyMatch(i -> i.getQuantity() < i.getType().getMaxStackSize());

        if (hasStackSpace) return true;

        // Check for empty slots
        return inventory.stream().anyMatch(Objects::isNull);
    }

    @Override
    public boolean canTakeItem(Item item, int quantity) {
        return getItemCount(item) >= quantity;
    }

    @Override
    public void setItems(List<Item> items) {
        clearInventory();
        items.stream()
                .filter(Objects::nonNull)
                .forEach(this::addItem);
    }

    // ==================================================================
    //  Game Loop Updates
    // ==================================================================

    /**
     * Updates player state including movement, animation, and cooldowns.
     *
     * @param deltaTime Time since last update in seconds
     */
    public void update(double deltaTime) {
        snapshot();
        updateMovement(deltaTime);
        updateDirection();
        updateAnimation(deltaTime);
        updateCooldowns(deltaTime);
        updateAttackAnimation(deltaTime);
    }

    // ==================================================================
    //  Getters
    // ==================================================================

    @Override
    public double getRenderOffsetY() { return RENDER_OFFSET_Y; }
    public double getMaxHealth() { return MAX_HEALTH; }
    public List<Item> getInventory() { return Collections.unmodifiableList(inventory); }
    public int getDirection() { return direction; }
    public int getAnimationFrame() { return animationFrame; }
    public double getAttackDamage() { return attackDamage; }
    public double getAttackRange() { return attackRange; }

    // ==================================================================
    //  Private Helper Methods
    // ==================================================================

    // --- Movement Helpers ---

    private void updateMovement(double deltaTime) {
        if (!currentPath.isEmpty()) {
            followPath(deltaTime);
        } else if (!isAtPosition(targetTileX, targetTileY)) {
            moveDirectly(deltaTime);
        }
    }

    private void followPath(double deltaTime) {
        PathNode next = currentPath.peek();
        double dx = next.x - posX;
        double dy = next.y - posY;
        double dist = Math.hypot(dx, dy);
        double maxStep = MOVE_SPEED * deltaTime;

        if (dist <= maxStep) {
            // Reached node
            posX = next.x;
            posY = next.y;
            currentTileX = next.x;
            currentTileY = next.y;
            currentPath.poll();

            // Set next target if available
            if (!currentPath.isEmpty()) {
                PathNode nextNode = currentPath.peek();
                targetTileX = nextNode.x;
                targetTileY = nextNode.y;
            }
        } else {
            // Move toward node
            posX += (dx / dist) * maxStep;
            posY += (dy / dist) * maxStep;
        }
    }

    private void moveDirectly(double deltaTime) {
        double dx = targetTileX - posX;
        double dy = targetTileY - posY;
        double dist = Math.hypot(dx, dy);
        double maxStep = MOVE_SPEED * deltaTime;

        if (dist <= maxStep) {
            // Reached target
            posX = targetTileX;
            posY = targetTileY;
            currentTileX = targetTileX;
            currentTileY = targetTileY;
        } else {
            // Move toward target
            posX += (dx / dist) * maxStep;
            posY += (dy / dist) * maxStep;
        }
    }

    private void startNewPath(List<PathNode> path) {
        currentPath.clear();
        currentPath.addAll(path);
        advanceToNextNode();
    }

    private void advanceToNextNode() {
        if (!currentPath.isEmpty()) {
            PathNode nextNode = currentPath.poll();
            targetTileX = nextNode.x;
            targetTileY = nextNode.y;
        }
    }

    private void clearPath() {
        currentPath.clear();
        targetTileX = currentTileX;
        targetTileY = currentTileY;
    }

    private boolean isAtPosition(int x, int y) {
        return currentTileX == x && currentTileY == y;
    }

    private void updateDirection() {
        if (currentPath.isEmpty()) return;

        PathNode next = currentPath.peek();
        double dx = next.x - posX;
        double dy = next.y - posY;

        // Calculate movement direction
        if (dx > 0 && dy < 0) direction = DIR_UP_RIGHT;
        else if (dx > 0 && dy > 0) direction = DIR_DOWN_RIGHT;
        else if (dx < 0 && dy > 0) direction = DIR_DOWN_LEFT;
        else if (dx < 0 && dy < 0) direction = DIR_UP_LEFT;
        else if (dx > 0) direction = DIR_RIGHT;
        else if (dx < 0) direction = DIR_LEFT;
        else if (dy < 0) direction = DIR_UP;
        else if (dy > 0) direction = DIR_DOWN;
    }

    // --- Animation Helpers ---

    private void updateAnimation(double deltaTime) {
        if (isMoving()) {
            animationTimer += deltaTime;
            if (animationTimer >= ANIMATION_FRAME_DURATION) {
                animationFrame = (animationFrame == WALKING_FRAME_END) ?
                        WALKING_FRAME_START : animationFrame + 1;
                animationTimer = 0;
            }
        } else {
            resetToIdleAnimation();
        }
    }

    private void resetToIdleAnimation() {
        animationFrame = IDLE_FRAME;
        animationTimer = 0;
    }

    private void updateCooldowns(double deltaTime) {
        attackTimer = Math.max(0, attackTimer - deltaTime);
    }

    private void updateAttackAnimation(double deltaTime) {
        if (isAttacking) {
            attackAnimationTime += deltaTime;
            if (attackAnimationTime > ATTACK_ANIMATION_DURATION) {
                isAttacking = false;
                attackAnimationTime = 0;
            }
        }
    }

    // --- Inventory Helpers ---

    private int findItemSlot(Item item) {
        for (int i = 0; i < INVENTORY_CAPACITY; i++) {
            if (inventory.get(i) == item) {
                return i;
            }
        }
        return -1;
    }

    private boolean canStack(Item existing, Item newItem) {
        return existing != null &&
                newItem != null &&
                existing.getType() == newItem.getType() &&
                existing.getQuantity() < existing.getType().getMaxStackSize();
    }

    private boolean isValidSlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < INVENTORY_CAPACITY;
    }

}