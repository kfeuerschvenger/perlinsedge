package com.feuerschvenger.perlinsedge.domain.entities;

import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.entities.items.ToolStats;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.pathfinding.Pathfinder;
import com.feuerschvenger.perlinsedge.domain.utils.Doubles;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the player character with movement, inventory, and combat capabilities.
 * Handles pathfinding, movement interpolation, inventory management, and resource interaction.
 */
@Slf4j
public class Player extends Entity implements Container {
    // ==================================================================
    //  Constants
    // ==================================================================

    // Animation configuration
    private static final double ANIMATION_FRAME_DURATION = 0.1;
    private static final int IDLE_FRAME = WalkCycle.IDLE;
    public static final double RENDER_OFFSET_Y = 0;

    // Health configuration
    private static final double MAX_HEALTH = 100.0;

    // Combat configuration
    private static final double DEFAULT_ATTACK_COOLDOWN = 0.8;  // Time between attacks
    private static final double ATTACK_ANIMATION_DURATION = 0.3;
    /** How far a diagonal attack reaches along each axis, so its distance from the player is about one tile. */
    private static final double DIAGONAL_ATTACK_REACH = 0.7;

    // Movement configuration
    private static final double MOVE_SPEED = 5.0;

    // Inventory configuration
    /** Number of slots of the hotbar, the first slots of the storage. */
    public static final int HOTBAR_SIZE = 9;
    /** Number of slots of the inventory grid, the slots that follow the hotbar. */
    public static final int INVENTORY_SIZE = 18;
    /** Number of slots the player stores items in: the hotbar first, then the inventory, so the index order is the fill order. */
    private static final int INVENTORY_CAPACITY = HOTBAR_SIZE + INVENTORY_SIZE;

    // ==================================================================
    //  Instance Variables
    // ==================================================================

    // Combat state
    private double attackTimer;
    private boolean isAttacking;
    private double attackAnimationTime;
    private int attackDirection = -1;
    private double secondsSinceHurt = Double.POSITIVE_INFINITY;
    /** Health lost since {@link #drainDamageTaken()} was last called. */
    private double damageTaken;

    // Inventory
    private final List<Item> inventory = new ArrayList<>(Collections.nCopies(INVENTORY_CAPACITY, null));
    /** The hotbar slot held in hand. It is a position, so it stays put while the items in the slots change. */
    private int selectedHotbarIndex;
    /** Grows on every change to the content of a slot, see {@link #getInventoryVersion()}. */
    private long inventoryVersion;

    // Movement and pathfinding
    private final LinkedList<PathNode> currentPath = new LinkedList<>();

    // Animation state
    private int direction = DIR_UP;
    private int animationFrame = IDLE_FRAME;
    private int animationStep;
    private double animationTimer;

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
     * @return true if the attack started, false while the cooldown runs or the player is dead
     */
    public boolean startAttack(int direction) {
        if (attackTimer > 0 || isDead) {
            return false;
        }
        isAttacking = true;
        attackTimer = DEFAULT_ATTACK_COOLDOWN;
        attackAnimationTime = 0;
        attackDirection = direction;
        return true;
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
        return switch (attackDirection) {
            case DIR_UP -> new double[]{x, y - 1};
            case DIR_UP_RIGHT -> new double[]{x + DIAGONAL_ATTACK_REACH, y - DIAGONAL_ATTACK_REACH};
            case DIR_RIGHT -> new double[]{x + 1, y};
            case DIR_DOWN_RIGHT -> new double[]{x + DIAGONAL_ATTACK_REACH, y + DIAGONAL_ATTACK_REACH};
            case DIR_DOWN -> new double[]{x, y + 1};
            case DIR_DOWN_LEFT -> new double[]{x - DIAGONAL_ATTACK_REACH, y + DIAGONAL_ATTACK_REACH};
            case DIR_LEFT -> new double[]{x - 1, y};
            case DIR_UP_LEFT -> new double[]{x - DIAGONAL_ATTACK_REACH, y - DIAGONAL_ATTACK_REACH};
            default -> new double[]{x, y};
        };
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
     * @return true when the player started walking, false when the order did nothing: the player is dead, already on
     *         the tile, or the tile cannot be reached. An order to the tile the player stands on ends the walk in
     *         progress, as any refused order does, so a player that has just reached a node of a longer path does not
     *         carry on past the tile that was clicked.
     */
    public boolean moveTo(TileMap map, int targetX, int targetY) {
        return moveTo(map, targetX, targetY, Set.of());
    }

    /**
     * Initiates movement like {@link #moveTo(TileMap, int, int)} around tiles that are taken: the walk neither crosses
     * them nor ends on them, so an order onto one is refused.
     *
     * @param map      The game world tile map
     * @param targetX  Target X coordinate
     * @param targetY  Target Y coordinate
     * @param occupied tiles the walk may not use, such as those the enemies stand on
     * @return true when the player started walking, false when the order did nothing
     */
    public boolean moveTo(TileMap map, int targetX, int targetY, Set<PathNode> occupied) {
        // A dead player stays where it fell until the respawn
        if (isDead) {
            return false;
        }
        final PathNode step = stepInProgress();
        // Already at the target position: nothing to walk to, and no walk left over from an earlier order
        if (step == null && isAtPosition(targetX, targetY)) {
            stop();
            return false;
        }

        if (isFreeStepTo(step, targetX, targetY, occupied)) {
            startNewPath(List.of(step));
            return true;
        }
        // A player between two tiles plans from the tile it is approaching, so the new walk never starts with a
        // diagonal across a blocked corner from an off-grid position
        final int fromX = step != null ? step.x : currentTileX;
        final int fromY = step != null ? step.y : currentTileY;

        var pathfinder = new Pathfinder(map, occupied);
        var path = pathfinder.findPath(fromX, fromY, targetX, targetY).nodes();

        if (path.isEmpty()) {
            // A refused order ends the walk the way any stop does: a player between two tiles finishes its step
            stop();
            return false;
        }

        startNewPath(path);
        return true;
    }

    /** Tells whether the step in progress already ends on the tile an order asks for, and that tile is not taken. */
    private static boolean isFreeStepTo(PathNode step, int targetX, int targetY, Set<PathNode> occupied) {
        return step != null && step.x == targetX && step.y == targetY && !occupied.contains(step);
    }

    /**
     * Gives the tile a new walk is planned from: the tile the player is approaching when it is between two tiles,
     * otherwise the tile it stands on. {@link #walkAlong(List)} expects a path that starts next to this tile.
     * Both coordinates come from one reading of the walk state, so they always belong to the same tile.
     *
     * @return the tile a new walk starts from
     */
    public PathNode getWalkOrigin() {
        final PathNode step = stepInProgress();
        return step != null ? step : new PathNode(currentTileX, currentTileY);
    }

    /**
     * Starts walking along a path that was already found, replacing the walk in progress. A player between two tiles
     * first finishes the step it is on, so the path must be planned from {@link #getWalkOrigin()}.
     *
     * @param path the tiles to walk, excluding the tile the walk starts from; an empty or null path changes nothing
     * @return true when the player started walking
     */
    public boolean walkAlong(List<PathNode> path) {
        if (isDead || path == null || path.isEmpty()) {
            return false;
        }
        startNewPath(path);
        return true;
    }

    /**
     * Ends the walk in progress. A player between two tiles finishes the step it is on, so it never freezes off the
     * tile grid; a player standing on a tile stops there.
     */
    public void stop() {
        final PathNode head = currentPath.peek();
        if (head != null && !isOnTileCentre()) {
            currentPath.clear();
            currentPath.add(head);
            return;
        }
        clearPath();
    }

    /**
     * Gives the next tile of the walk in progress, for the checks that run every tick and must not allocate. The node
     * is the one the walk itself holds, so it is read only: a caller must not change its costs, and gets a copy from
     * {@code new PathNode(x, y)} when it needs one.
     *
     * @return the tile the player is walking to, or null when there is no walk
     */
    public PathNode getNextTile() {
        return currentPath.peek();
    }

    /**
     * Ends the walk when the next tile of it has become taken since the walk was planned, so the player never steps
     * onto it. A player standing on a tile stops there, a player between two tiles walks back to the tile it left.
     *
     * @param occupied tiles the walk may not enter, such as those the enemies stand on or step to
     * @return true when the walk was ended
     */
    public boolean haltBefore(Set<PathNode> occupied) {
        final PathNode head = currentPath.peek();
        if (head == null || !occupied.contains(head)) {
            return false;
        }
        if (isOnTileCentre()) {
            clearPath();
        } else {
            currentPath.clear();
            currentPath.add(new PathNode(currentTileX, currentTileY));
            targetTileX = currentTileX;
            targetTileY = currentTileY;
        }
        return true;
    }

    /** Stops any walk in progress, so a player that dies mid-path does not keep walking. */
    @Override
    public void handleDeath() {
        super.handleDeath();
        clearPath();
    }

    /**
     * Applies damage and restarts the hurt clock, which the renderer reads to flash and shake the sprite.
     * Every source of damage to the player goes through this method, so none of them needs to know about the reaction.
     *
     * @param amount damage to apply; zero or less changes nothing
     */
    @Override
    public void takeDamage(double amount) {
        if (isDead || amount <= 0) {
            return;
        }
        final double before = health;
        super.takeDamage(amount);
        damageTaken += before - health;
        secondsSinceHurt = 0;
    }

    /**
     * Gives the health the player lost since this method was last called, which the game turns into the floating
     * number that shows the hit. Every source of damage goes through {@link #takeDamage}, so none of them reports its own.
     *
     * @return the health lost, 0 when the player took no damage meanwhile
     */
    public double drainDamageTaken() {
        final double taken = damageTaken;
        damageTaken = 0;
        return taken;
    }

    /**
     * Returns the time since the player last took damage.
     *
     * @return seconds, infinite when the player was never hurt or has respawned since
     */
    public double getSecondsSinceHurt() {
        return secondsSinceHurt;
    }

    /** Forgets the last hit when the player respawns, so the reaction does not play on the new life. */
    @Override
    public void revive(int x, int y) {
        super.revive(x, y);
        secondsSinceHurt = Double.POSITIVE_INFINITY;
        damageTaken = 0;
    }

    public boolean isMoving() {
        return !isAtPosition(targetTileX, targetTileY) || !currentPath.isEmpty();
    }

    // ==================================================================
    //  Public API - Inventory Management
    // ==================================================================

    public void clearInventory() {
        inventoryVersion++;
        for (var i = 0; i < INVENTORY_CAPACITY; i++) {
            inventory.set(i, null);
        }
    }

    public void removeItemFromSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < INVENTORY_CAPACITY) {
            inventoryVersion++;
            inventory.set(slotIndex, null);
        }
    }

    /**
     * Gives a number that grows whenever the content of a slot changes: an item stored, removed, moved or crafted, a
     * stack that grows or shrinks, or the whole inventory replaced. The views compare it with the value they last drew
     * to skip work while nothing changed.
     *
     * @return the change count, which only ever increases
     */
    public long getInventoryVersion() {
        return inventoryVersion;
    }

    /** Counts a change to a stack that code outside the player edited in place. */
    @Override
    public void noteStackChanged() {
        inventoryVersion++;
    }

    /**
     * Attempts to pick up an item from the ground.
     *
     * @param item The item to pick up
     * @return The quantity actually picked up
     */
    public int pickUpItem(Item item) {
        if (item == null || item.getQuantity() <= 0) {
            return 0;
        }

        int quantityBeforePickup = item.getQuantity();
        boolean added = addItem(item);

        int quantityActuallyPickedUp = quantityBeforePickup - item.getQuantity();

        if (added && quantityActuallyPickedUp > 0) {
            log.debug("Player picked up {}x {}", quantityActuallyPickedUp, item.getDisplayName());
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
        var sourceSlot = findItemSlot(draggedItem);
        if (sourceSlot == -1) {
            log.warn("Item not found in inventory");
            return;
        }
        inventoryVersion++;

        var targetItem = inventory.get(targetSlotIndex);

        // Case 1: Target slot is empty
        if (targetItem == null) {
            inventory.set(targetSlotIndex, draggedItem);
            inventory.set(sourceSlot, null);
            return;
        }

        // Case 2: Same item type - try to stack
        if (canStack(targetItem, draggedItem)) {
            var spaceInTarget = targetItem.getType().getMaxStackSize() - targetItem.getQuantity();
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
    //  Public API - Hotbar
    // ==================================================================

    /**
     * Gives the hotbar slot that is held in hand.
     *
     * @return the slot index, from 0 to {@link #HOTBAR_SIZE} - 1
     */
    public int getSelectedHotbarIndex() {
        return selectedHotbarIndex;
    }

    /**
     * Holds a hotbar slot in hand. The choice is a position and is kept when the items move, are picked up, used up or
     * thrown, so the slot keeps being the active one with whatever it holds next.
     *
     * @param index slot index from 0 to {@link #HOTBAR_SIZE} - 1; any other value changes nothing
     */
    public void selectHotbarSlot(int index) {
        if (index >= 0 && index < HOTBAR_SIZE) {
            selectedHotbarIndex = index;
        }
    }

    /**
     * Moves the hand along the hotbar, wrapping around at both ends.
     *
     * @param delta slots to move, positive toward the last slot and negative toward the first
     */
    public void cycleHotbar(int delta) {
        selectedHotbarIndex = Math.floorMod(selectedHotbarIndex + delta, HOTBAR_SIZE);
    }

    /**
     * Gives the stack in the selected hotbar slot, the item the player holds in hand.
     *
     * @return the stack, or null when the slot is empty, which means a bare hand
     */
    public Item getActiveItem() {
        return inventory.get(selectedHotbarIndex);
    }

    /**
     * Gives the type of the item held in hand.
     *
     * @return the type, or null for a bare hand
     */
    public ItemType getActiveItemType() {
        final var active = getActiveItem();
        return active != null ? active.getType() : null;
    }

    /**
     * Uses up one item of the stack held in hand, emptying the slot when it was the last one.
     *
     * @return true when an item was removed, false when the hand is empty
     */
    public boolean consumeActiveItem() {
        final var active = getActiveItem();
        if (active == null) {
            return false;
        }
        inventoryVersion++;
        if (active.getQuantity() <= 1) {
            inventory.set(selectedHotbarIndex, null);
        } else {
            active.removeQuantity(1);
        }
        return true;
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
     * Nothing changes unless the ingredients are present and the whole output fits once they are consumed,
     * so a craft never destroys items.
     *
     * @param recipe recipe to craft
     * @return true when the ingredients were consumed and the output added, false when nothing changed
     */
    public boolean craftItem(Recipe recipe) {
        if (!canCraft(recipe)) {
            log.warn("Cannot craft: Missing ingredients");
            return false;
        }
        if (!outputFitsAfterConsuming(recipe)) {
            log.debug("Cannot craft {}: no room for the output", recipe.getName());
            return false;
        }

        for (CraftingIngredient ingredient : recipe.getIngredients()) {
            removeItem(new Item(ingredient.itemType(), 1), ingredient.quantity());
        }

        addItem(new Item(recipe.getOutputItemType(), recipe.getOutputQuantity()));
        return true;
    }

    /**
     * Simulates consuming the ingredients and then adding the output, without touching the inventory.
     */
    private boolean outputFitsAfterConsuming(Recipe recipe) {
        var types = new ItemType[INVENTORY_CAPACITY];
        var quantities = new int[INVENTORY_CAPACITY];
        for (var i = 0; i < INVENTORY_CAPACITY; i++) {
            var item = inventory.get(i);
            if (item != null) {
                types[i] = item.getType();
                quantities[i] = item.getQuantity();
            }
        }

        for (CraftingIngredient ingredient : recipe.getIngredients()) {
            consumeFromSimulation(types, quantities, ingredient);
        }

        return simulatedRoomFor(types, quantities, recipe.getOutputItemType()) >= recipe.getOutputQuantity();
    }

    /**
     * Removes one ingredient from the simulated slots, front to back.
     */
    private static void consumeFromSimulation(ItemType[] types, int[] quantities, CraftingIngredient ingredient) {
        int remaining = ingredient.quantity();
        for (var i = 0; i < INVENTORY_CAPACITY && remaining > 0; i++) {
            if (types[i] == ingredient.itemType()) {
                int taken = Math.min(quantities[i], remaining);
                quantities[i] -= taken;
                remaining -= taken;
                if (quantities[i] == 0) {
                    types[i] = null;
                }
            }
        }
    }

    /**
     * Counts how many units of the output type the simulated slots can still take.
     */
    private static int simulatedRoomFor(ItemType[] types, int[] quantities, ItemType outputType) {
        int maxStack = outputType.getMaxStackSize();
        var room = 0;
        for (var i = 0; i < INVENTORY_CAPACITY; i++) {
            if (types[i] == null) {
                room += maxStack;
            } else if (types[i] == outputType) {
                room += maxStack - quantities[i];
            }
        }
        return room;
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
        if (!isValidSlot(slotIndex)) {
            return null;
        }
        return inventory.get(slotIndex);
    }

    @Override
    public void setItemAt(int slotIndex, Item item) {
        if (!isValidSlot(slotIndex)) {
            return;
        }
        inventoryVersion++;
        inventory.set(slotIndex, item);
    }

    /**
     * Stores an item in the fill order of the player: first into the stacks of the same type already held, hotbar
     * before inventory, then into the first free hotbar slot, then into the first free inventory slot. Slot indexes
     * run from the hotbar to the inventory, so both passes walk the slots in index order.
     *
     * @param item the item to store; its quantity is reduced by what was stored
     * @return true when any part of it was stored
     */
    @Override
    public boolean addItem(Item item) {
        if (item == null || item.getQuantity() <= 0) {
            log.debug("Cannot add null or zero quantity item");
            return false;
        }

        inventoryVersion++;
        int originalQuantity = item.getQuantity();
        int remaining = originalQuantity;
        // Try stacking with existing items
        for (var i = 0; i < INVENTORY_CAPACITY && remaining > 0; i++) {
            Item existing = inventory.get(i);
            if (existing != null && canStack(existing, item)) {
                int availableSpace = existing.getType().getMaxStackSize() - existing.getQuantity();
                int transfer = Math.min(availableSpace, remaining);

                existing.addQuantity(transfer);
                remaining -= transfer;
            }
        }

        // Add to empty slots
        for (var i = 0; i < INVENTORY_CAPACITY && remaining > 0; i++) {
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
            log.debug("Cannot remove null or zero quantity item");
            return false;
        }

        if (getItemCount(itemToRemove) < quantity) {
            log.debug("Cannot remove {}x {}: not enough in the inventory", quantity, itemToRemove.getDisplayName());
            return false;
        }

        inventoryVersion++;
        int remaining = quantity;
        for (var i = 0; i < INVENTORY_CAPACITY && remaining > 0; i++) {
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

        return true;
    }

    @Override
    public int getItemCount(Item item) {
        if (item == null) {
            return 0;
        }
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
        if (item == null) {
            return false;
        }

        // Check for stacking space
        boolean hasStackSpace = inventory.stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getType() == item.getType())
                .anyMatch(i -> i.getQuantity() < i.getType().getMaxStackSize());

        if (hasStackSpace) {
            return true;
        }

        // Check for empty slots
        return inventory.stream().anyMatch(Objects::isNull);
    }

    @Override
    public boolean canTakeItem(Item item, int quantity) {
        return getItemCount(item) >= quantity;
    }

    @Override
    public void setItems(List<Item> items) {
        // clearInventory counts the change
        clearInventory();
        for (var i = 0; i < items.size() && i < INVENTORY_CAPACITY; i++) {
            var source = items.get(i);
            if (source != null && source.getQuantity() > 0) {
                inventory.set(i, new Item(source.getType(), Math.min(source.getQuantity(), source.getMaxStackSize())));
            }
        }
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
        secondsSinceHurt += deltaTime;
    }

    // ==================================================================
    //  Getters
    // ==================================================================

    @Override
    public double getRenderOffsetY() { return RENDER_OFFSET_Y; }
    @Override
    public double getMaxHealth() { return MAX_HEALTH; }
    public List<Item> getInventory() { return Collections.unmodifiableList(inventory); }
    public int getDirection() { return direction; }
    public int getAnimationFrame() { return animationFrame; }
    /**
     * Gives the damage one melee hit deals, which depends on the item held in hand, see {@link ToolStats}.
     *
     * @return the damage of the item in hand, or of a bare hand
     */
    public double getAttackDamage() { return ToolStats.combatDamage(getActiveItemType()); }

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

            // The target is the next node, or the node just reached when the walk is over: a target left on an
            // earlier node would pull the player back to it
            PathNode nextNode = currentPath.isEmpty() ? null : currentPath.peek();
            targetTileX = nextNode != null ? nextNode.x : currentTileX;
            targetTileY = nextNode != null ? nextNode.y : currentTileY;
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

    /** The walker heads for the head of the queue and drops a node only once it stands on it, so every node is visited. */
    private void startNewPath(List<PathNode> path) {
        final PathNode step = stepInProgress();
        currentPath.clear();
        // The step in progress is finished first, so no straight line is drawn from an off-grid position
        if (step != null && !step.equals(path.get(0))) {
            currentPath.add(step);
        }
        currentPath.addAll(path);
        final PathNode first = currentPath.peek();
        if (first != null) {
            targetTileX = first.x;
            targetTileY = first.y;
        }
    }

    /**
     * The tile the player is walking to when it is between two tiles.
     *
     * @return that tile, or null when the player stands on a tile
     */
    private PathNode stepInProgress() {
        if (isOnTileCentre()) {
            return null;
        }
        final PathNode head = currentPath.peek();
        return head != null ? head : new PathNode(currentTileX, currentTileY);
    }

    private void clearPath() {
        currentPath.clear();
        targetTileX = currentTileX;
        targetTileY = currentTileY;
    }

    /** Tells whether the player stands exactly on the centre of its tile, which is how every step of a walk ends. */
    private boolean isOnTileCentre() {
        return Doubles.areEqual(posX, currentTileX) && Doubles.areEqual(posY, currentTileY);
    }

    private boolean isAtPosition(int x, int y) {
        return currentTileX == x && currentTileY == y;
    }

    /** Marks the pair of signs of a step that is no step at all, which leaves the direction as it is. */
    private static final int KEEP_DIRECTION = -1;
    /**
     * The direction of a step by the signs of its two components, indexed by {@code (sign of dx + 1) * 3 + (sign of
     * dy + 1)}: the columns run from up (negative dy) through level to down, the rows from left through level to right.
     */
    private static final int[] DIRECTION_BY_SIGNS = {
            DIR_UP_LEFT, DIR_LEFT, DIR_DOWN_LEFT,
            DIR_UP, KEEP_DIRECTION, DIR_DOWN,
            DIR_UP_RIGHT, DIR_RIGHT, DIR_DOWN_RIGHT
    };
    private static final int SIGN_OFFSET = 1;
    private static final int SIGN_COUNT = 3;

    private void updateDirection() {
        if (currentPath.isEmpty()) {
            return;
        }

        PathNode next = currentPath.peek();
        // A component that is not a number counts as no movement along its axis
        final int signX = (int) Math.signum(next.x - posX);
        final int signY = (int) Math.signum(next.y - posY);
        final int facing = DIRECTION_BY_SIGNS[(signX + SIGN_OFFSET) * SIGN_COUNT + signY + SIGN_OFFSET];
        if (facing != KEEP_DIRECTION) {
            direction = facing;
        }
    }

    // --- Animation Helpers ---

    private void updateAnimation(double deltaTime) {
        if (isMoving()) {
            animationTimer += deltaTime;
            if (animationTimer >= ANIMATION_FRAME_DURATION) {
                animationStep = (animationStep + 1) % WalkCycle.length();
                animationFrame = WalkCycle.frameAt(animationStep);
                animationTimer = 0;
            }
        } else {
            resetToIdleAnimation();
        }
    }

    private void resetToIdleAnimation() {
        animationFrame = IDLE_FRAME;
        animationStep = 0;
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
        for (var i = 0; i < INVENTORY_CAPACITY; i++) {
            if (isSameInstance(inventory.get(i), item)) {
                return i;
            }
        }
        return -1;
    }

    /** Tells whether two references are the very same stack: items compare equal by type, so equals cannot tell them apart. */
    private static boolean isSameInstance(Object first, Object second) {
        return first == second;
    }

    private static boolean canStack(Item existing, Item newItem) {
        return existing != null &&
                newItem != null &&
                existing.getType() == newItem.getType() &&
                existing.getQuantity() < existing.getType().getMaxStackSize();
    }

    private static boolean isValidSlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < INVENTORY_CAPACITY;
    }

}
