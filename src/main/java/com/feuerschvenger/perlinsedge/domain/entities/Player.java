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
    // Animation constants
    private static final double ANIMATION_FRAME_DURATION = 0.1;
    private static final int IDLE_FRAME = 1;
    private static final int WALKING_FRAME_START = 0;
    private static final int WALKING_FRAME_END = 2;
    public static final double RENDER_OFFSET_Y = 0;

    // Health management
    private static final double MAX_HEALTH = 100.0;

    private double attackRange = 1.5;     // In tiles
    private double attackDamage = 10.0;   // Base damage per attack
    private double attackCooldown = 0.8;  // Time between attacks
    private double attackTimer = 0;
    private boolean isAttacking = false;
    private double attackAnimationTime = 0;
    private int attackDirection = -1;

    // Inventory
    private static final int INVENTORY_CAPACITY = 18;
    private final List<Item> inventory = new ArrayList<>(Collections.nCopies(INVENTORY_CAPACITY, null));

    // Movement and pathfinding
    private static final double MOVE_SPEED = 5.0;
    private final LinkedList<PathNode> currentPath = new LinkedList<>();

    // Animation state
    private int direction = DIR_UP;
    private int animationFrame = IDLE_FRAME;
    private double animationTimer = 0;

    public Player(int startX, int startY) {
        super(startX, startY, MAX_HEALTH);
    }

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
        return Math.min(1.0, attackAnimationTime / 0.3);
    }

    /**
     * Attempts to move the player to a new target position using pathfinding.
     *
     * @param map     The tile map for collision detection
     * @param targetX Target X coordinate
     * @param targetY Target Y coordinate
     */
    public void moveTo(TileMap map, int targetX, int targetY) {
        // Already at target position
        if (isAtPosition(targetX, targetY)) return;

        Pathfinder pathfinder = new Pathfinder(map);
        List<PathNode> path = pathfinder.findPath(currentTileX, currentTileY, targetX, targetY);

        if (path == null || path.isEmpty()) {
            clearPath();
            return;
        }

        startNewPath(path);
    }

    /**
     * Updates player state including movement, animation, and cooldowns.
     *
     * @param deltaTime Time since last update in seconds
     */
    public void update(double deltaTime) {
        snapshot();

        if (!currentPath.isEmpty()) {
            PathNode next = currentPath.peek();
            double targetX = next.x;
            double targetY = next.y;

            double dx = targetX - posX;
            double dy = targetY - posY;
            double dist = Math.hypot(dx, dy);
            double maxStep = MOVE_SPEED * deltaTime;

            if (dist <= maxStep) {
                // Llegamos al nodo
                posX = targetX;
                posY = targetY;
                currentTileX = next.x; // Actualizar posición de tile
                currentTileY = next.y;
                currentPath.poll();

                // Si hay más nodos, establecer el siguiente target
                if (!currentPath.isEmpty()) {
                    PathNode nextNode = currentPath.peek();
                    targetTileX = nextNode.x;
                    targetTileY = nextNode.y;
                }
            } else {
                // Movimiento continuo hacia el target
                posX += (dx / dist) * maxStep;
                posY += (dy / dist) * maxStep;
            }
        } else if (!isAtPosition(targetTileX, targetTileY)) {
            // Movimiento entre tiles (sin pathfinding)
            double dx = targetTileX - posX;
            double dy = targetTileY - posY;
            double dist = Math.hypot(dx, dy);
            double maxStep = MOVE_SPEED * deltaTime;

            if (dist <= maxStep) {
                posX = targetTileX;
                posY = targetTileY;
                currentTileX = targetTileX;
                currentTileY = targetTileY;
            } else {
                posX += (dx / dist) * maxStep;
                posY += (dy / dist) * maxStep;
            }
        }

        updateDirection();
        updateAnimation(deltaTime);
        updateCooldowns(deltaTime);
        updateAttackAnimation(deltaTime);
    }

    // --- Container Interface Implementations ---

    @Override
    public List<Item> getItems() {
        return new ArrayList<>(inventory); // Return a copy
    }

    @Override
    public Item getItemAt(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= inventory.size()) {
            System.out.println("GET ITEM AT: Invalid slot " + slotIndex);
            return null;
        }
        return inventory.get(slotIndex);
    }

    @Override
    public void setItemAt(int slotIndex, Item item) {
        if (slotIndex < 0 || slotIndex >= inventory.size()) {
            System.out.println("SET ITEM AT: Invalid slot " + slotIndex);
            return;
        }
        System.out.println("SET ITEM AT: Slot " + slotIndex + " -> " +
                (item != null ? item.getDisplayName() : "Empty"));
        inventory.set(slotIndex, item);
    }

    @Override
    public boolean addItem(Item item) {
        if (item == null || item.getQuantity() <= 0) {
            System.out.println("Player Inventory: Cannot add null or zero quantity item.");
            return false;
        }

        // Try to stack with existing items
        for (int i = 0; i < INVENTORY_CAPACITY; i++) {
            Item existingItem = inventory.get(i);
            if (existingItem != null && existingItem.getType() == item.getType() && existingItem.getQuantity() < existingItem.getMaxStackSize()) {
                int canAdd = existingItem.getMaxStackSize() - existingItem.getQuantity();
                int toAdd = Math.min(canAdd, item.getQuantity());
                existingItem.setQuantity(existingItem.getQuantity() + toAdd);
                item.setQuantity(item.getQuantity() - toAdd);
                if (item.getQuantity() == 0) {
                    System.out.println("Player Inventory: Added " + toAdd + "x " + item.getDisplayName() + " to existing stack.");
                    return true; // All items added
                }
            }
        }

        // Add to new slot if space available
        for (int i = 0; i < INVENTORY_CAPACITY; i++) {
            if (inventory.get(i) == null) {
                int toAdd = Math.min(item.getQuantity(), item.getMaxStackSize());
                inventory.set(i, new Item(item.getType(), toAdd)); // Add a new stack
                item.setQuantity(item.getQuantity() - toAdd);
                System.out.println("Player Inventory: Added " + toAdd + "x " + item.getDisplayName() + " to new slot.");
                return true; // Item added (partially or fully)
            }
        }
        System.out.println("Player Inventory: No space for " + item.getDisplayName() + ".");
        return false; // No space
    }

    @Override
    public boolean removeItem(Item itemToRemove, int quantity) {
        if (itemToRemove == null || quantity <= 0) {
            System.out.println("Player Inventory: Cannot remove null or zero quantity item.");
            return false;
        }

        int removedCount = 0;
        // Iterate forwards to safely remove items from a List (or backwards if removing actual elements)
        // For inventory slots, iterating forwards and setting to null is fine.
        for (int i = 0; i < inventory.size(); i++) {
            Item currentItem = inventory.get(i);
            if (currentItem != null && currentItem.getType() == itemToRemove.getType()) {
                int canRemove = currentItem.getQuantity();
                int toRemove = Math.min(canRemove, quantity - removedCount); // Quantity needed from this stack

                currentItem.setQuantity(currentItem.getQuantity() - toRemove);
                removedCount += toRemove;

                if (currentItem.getQuantity() <= 0) {
                    inventory.set(i, null); // Remove item from slot if quantity is zero
                }

                if (removedCount == quantity) {
                    System.out.println("Player Inventory: Removed " + quantity + "x " + itemToRemove.getDisplayName() + ".");
                    return true; // All requested quantity removed
                }
            }
        }
        System.out.println("Player Inventory: Could not remove " + quantity + "x " + itemToRemove.getDisplayName() + ". Only removed " + removedCount + ".");
        return false; // Not enough quantity found
    }

    @Override
    public int getItemCount(Item item) {
        if (item == null) return 0;
        return inventory.stream()
                .filter(i -> i != null && i.getType() == item.getType())
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
        // Check if there's space in existing stacks
        for (Item existingItem : inventory) {
            if (existingItem != null && existingItem.getType() == item.getType() && existingItem.getQuantity() < existingItem.getMaxStackSize()) {
                return true;
            }
        }
        // Check if there's an empty slot
        return inventory.contains(null);
    }

    @Override
    public boolean canTakeItem(Item item, int quantity) {
        if (item == null || quantity <= 0) return false;
        return getItemCount(item) >= quantity;
    }

    @Override
    public void setItems(List<Item> items) {
        // This method can be used for loading saved inventories.
        // Clear current inventory first.
        Collections.fill(this.inventory, null);
        for (Item item : items) {
            addItem(item); // Add items one by one, respecting stack limits and capacity
        }
        System.out.println("Player Inventory: Set items. Current items count: " + inventory.stream().filter(Objects::nonNull).count());
    }

    // --- End Container Interface Implementations ---

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
        Item itemCopy = new Item(item.getType(), item.getQuantity()); // Work with a copy

        boolean added = addItem(itemCopy); // Use the addItem from Container interface
        int quantityActuallyPickedUp = quantityBeforePickup - itemCopy.getQuantity();

        if (added && quantityActuallyPickedUp > 0) {
            System.out.println("Player picked up " + quantityActuallyPickedUp + "x " + item.getDisplayName());
        }
        return quantityActuallyPickedUp;
    }

    public void moveItemInInventory(Item draggedItem, int targetSlotIndex) {
        if (targetSlotIndex < 0 || targetSlotIndex >= INVENTORY_CAPACITY) {
            return;
        }

        // Encontrar el slot de origen del ítem arrastrado
        int sourceSlot = -1;
        for (int i = 0; i < INVENTORY_CAPACITY; i++) { // Iterar hasta INVENTORY_CAPACITY
            if (inventory.get(i) == draggedItem) { // Compara por referencia de objeto, asumiendo que `draggedItem` es la misma instancia de la lista
                sourceSlot = i;
                break;
            }
        }

        if (sourceSlot == -1) {
            // Esto podría ocurrir si draggedItem no es la instancia exacta en la lista,
            // o si ya fue movido/eliminado de alguna manera.
            System.err.println("Error: El draggedItem no se encontró en el inventario.");
            return;
        }

        Item targetItem = inventory.get(targetSlotIndex); // Ahora esto es seguro

        // Caso 1: Slot destino vacío o nulo
        if (targetItem == null) {
            inventory.set(targetSlotIndex, draggedItem);
            inventory.set(sourceSlot, null); // El slot de origen ahora está vacío
            return;
        }

        // Caso 2: Mismo tipo de ítem - intentar apilar
        if (canStack(targetItem, draggedItem)) {
            // Calcula cuánto podemos transferir del draggedItem al targetItem
            int spaceInTarget = targetItem.getType().getMaxStackSize() - targetItem.getQuantity();
            int transferAmount = Math.min(spaceInTarget, draggedItem.getQuantity());

            if (transferAmount > 0) {
                targetItem.addQuantity(transferAmount);
                draggedItem.removeQuantity(transferAmount); // Reduce la cantidad del ítem arrastrado

                // Si el draggedItem se agotó después de la transferencia, eliminarlo del slot de origen
                if (draggedItem.getQuantity() <= 0) {
                    inventory.set(sourceSlot, null);
                }
                return;
            }
        }

        // Caso 3: Ítems diferentes o no apilables - intercambiar posiciones
        // Asegúrate de que el sourceSlot no sea el mismo que el targetSlot
        if (sourceSlot != targetSlotIndex) {
            inventory.set(sourceSlot, targetItem);
            inventory.set(targetSlotIndex, draggedItem);
        }

    }

    public boolean canCraft(Recipe recipe) {
        for (CraftingIngredient ingredient : recipe.getIngredients()) {
            if (!canTakeItem(new Item(ingredient.getItemType(), 1), ingredient.getQuantity())) {
                return false;
            }
        }
        return true;
    }

    public void craftItem(Recipe recipe) {
        if (!canCraft(recipe)) {
            System.err.println("Cannot craft: Missing ingredients.");
            return;
        }
        for (CraftingIngredient ingredient : recipe.getIngredients()) {
            removeItem(new Item(ingredient.getItemType(), 1), ingredient.getQuantity());
        }
        Item craftedItem = new Item(recipe.getOutputItemType(), recipe.getOutputQuantity());
        addItem(craftedItem);
    }

    // Getters -----------------------------------------------------------------

    @Override
    public double getRenderOffsetY() { return RENDER_OFFSET_Y; }
    public double getMaxHealth() { return MAX_HEALTH; }
    public List<Item> getInventory() { return inventory; }
    public int getDirection() { return direction; }
    public int getAnimationFrame() { return animationFrame; }
    public boolean isMoving() { return !isAtPosition(targetTileX, targetTileY) || !currentPath.isEmpty(); }
    public double getAttackDamage() { return attackDamage; }
    public double getAttackRange() { return attackRange; }

    // Private helper methods --------------------------------------------------

    private void updateAnimation(double deltaTime) {
        if (isMoving()) {
            animationTimer += deltaTime;
            if (animationTimer >= ANIMATION_FRAME_DURATION) {
                animationFrame = (animationFrame == WALKING_FRAME_END) ? WALKING_FRAME_START : animationFrame + 1;
                animationTimer = 0;
            }
        } else {
            resetToIdleAnimation();
        }
    }

    private void updateCooldowns(double deltaTime) {
        attackTimer = Math.max(0, attackTimer - deltaTime);
    }

    private void updateAttackAnimation(double deltaTime) {
        if (isAttacking) {
            attackAnimationTime += deltaTime;
            if (attackAnimationTime > 0.3) {
                isAttacking = false;
                attackAnimationTime = 0;
            }
        }
    }

    public double[] getAttackPosition() {
        double[] pos = getInterpolatedPosition(0);
        double x = pos[0];
        double y = pos[1];

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

    private void resetToIdleAnimation() {
        animationFrame = IDLE_FRAME;
        animationTimer = 0;
    }

    private void updateDirection() {
        if (currentPath.isEmpty()) return;

        PathNode next = currentPath.peek();
        double dx = next.x - posX;
        double dy = next.y - posY;

        // Actualizar dirección basada en el vector de movimiento
        if (dx > 0 && dy < 0) direction = DIR_UP_RIGHT;
        else if (dx > 0 && dy > 0) direction = DIR_DOWN_RIGHT;
        else if (dx < 0 && dy > 0) direction = DIR_DOWN_LEFT;
        else if (dx < 0 && dy < 0) direction = DIR_UP_LEFT;
        else if (dx > 0) direction = DIR_RIGHT;
        else if (dx < 0) direction = DIR_LEFT;
        else if (dy < 0) direction = DIR_UP;
        else if (dy > 0) direction = DIR_DOWN;
    }

    private void advanceToNextNode() {
        if (!currentPath.isEmpty()) {
            PathNode nextNode = currentPath.poll();
            targetTileX = nextNode.x;
            targetTileY = nextNode.y;
        }
    }

    private void startNewPath(List<PathNode> path) {
        currentPath.clear();
        currentPath.addAll(path);
        advanceToNextNode();
    }

    private void clearPath() {
        currentPath.clear();
        targetTileX = currentTileX;
        targetTileY = currentTileY;
    }

    private boolean isAtPosition(int x, int y) {
        return currentTileX == x && currentTileY == y;
    }

    private boolean canStack(Item existing, Item newItem) {
        return existing.getType() == newItem.getType() &&
                existing.getQuantity() < existing.getType().getMaxStackSize();
    }

}