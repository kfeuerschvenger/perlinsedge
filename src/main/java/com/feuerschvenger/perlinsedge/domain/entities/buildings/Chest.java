package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a chest building that provides storage functionality.
 * Implements the Container interface for item storage management.
 */
public class Chest extends Building implements Container {
    // ==================================================================
    //  Rendering Constants
    // ==================================================================
    private static final double CHEST_SIZE_FACTOR = 0.6;
    private static final double CHEST_VISUAL_HEIGHT_FACTOR = 1.2;
    private static final double CHEST_ELEVATION_OFFSET_FACTOR = 0.3;
    private static final double LOCK_SIZE_FACTOR = 0.4;
    private static final double STROKE_WIDTH = 1.0;

    // Color definitions
    private static final Color CHEST_BROWN = Color.rgb(139, 69, 19); // SaddleBrown
    private static final Color LID_TOP_COLOR = CHEST_BROWN.deriveColor(0, 1, 1.2, 1);
    private static final Color BODY_RIGHT_COLOR = CHEST_BROWN.deriveColor(0, 1, 0.8, 1);
    private static final Color BODY_LEFT_COLOR = CHEST_BROWN.deriveColor(0, 1, 0.9, 1);
    private static final Color LOCK_COLOR = Color.DARKGRAY;
    private static final Color OUTLINE_COLOR = Color.BLACK;

    // ==================================================================
    //  Instance Properties
    // ==================================================================
    private final List<Item> inventory;
    private final int capacity;

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new chest at the specified tile position.
     *
     * @param tileX    X-coordinate in tile units
     * @param tileY    Y-coordinate in tile units
     * @param capacity Maximum number of item stacks the chest can hold
     */
    public Chest(int tileX, int tileY, int capacity) {
        super(tileX, tileY, BuildingType.CHEST, true, true);
        this.inventory = new ArrayList<>();
        this.capacity = capacity;
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

        if (slotIndex < inventory.size()) {
            inventory.set(slotIndex, item);
        } else if (slotIndex == inventory.size() && hasAvailableSlot()) {
            inventory.add(item);
        }
    }

    @Override
    public boolean addItem(Item item) {
        if (item == null || item.getQuantity() <= 0) return false;

        // Try to stack with existing items
        int remaining = stackIntoExistingItems(item);
        if (remaining == 0) return true;

        // Add to new slot if space available
        return addToNewSlot(new Item(item.getType(), remaining));
    }

    @Override
    public boolean removeItem(Item itemToRemove, int quantity) {
        if (itemToRemove == null || quantity <= 0) return false;

        int remaining = quantity;
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            Item item = inventory.get(i);
            if (item.getType() == itemToRemove.getType()) {
                int toRemove = Math.min(item.getQuantity(), remaining);
                item.removeQuantity(toRemove);
                remaining -= toRemove;

                if (item.isEmpty()) {
                    inventory.remove(i);
                    i--; // Adjust index after removal
                }
            }
        }

        return remaining == 0;
    }

    @Override
    public int getItemCount(Item item) {
        return inventory.stream()
                .filter(i -> i.getType() == item.getType())
                .mapToInt(Item::getQuantity)
                .sum();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean hasSpace(Item item) {
        // Check for stack space
        boolean hasStackSpace = inventory.stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getType() == item.getType())
                .anyMatch(i -> i.getQuantity() < i.getMaxStackSize());

        return hasStackSpace || hasAvailableSlot();
    }

    @Override
    public boolean canTakeItem(Item item, int quantity) {
        return getItemCount(item) >= quantity;
    }

    @Override
    public void setItems(List<Item> items) {
        this.inventory.clear();
        items.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getQuantity() > 0)
                .forEach(this::addItem);
    }

    // ==================================================================
    //  Interaction Methods
    // ==================================================================

    @Override
    public boolean onInteract(Tile tile) {
        System.out.printf("Interacted with Chest at (%d, %d). Opening inventory.%n", tileX, tileY);
        return true;
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        final AppConfig config = AppConfig.getInstance();

        // Calculate scaled dimensions
        final double halfWidth = config.graphics().getTileHalfWidth() * zoom;
        final double halfHeight = config.graphics().getTileHalfHeight() * zoom;

        // Calculate chest geometry
        final ChestGeometry geometry = calculateChestGeometry(screenX, screenY, zoom, halfWidth, halfHeight);

        // Render chest components
        renderChestBody(gc, geometry, zoom);
        renderChestLid(gc, geometry, zoom);
        renderChestLock(gc, geometry, zoom);
    }

    // ==================================================================
    //  Inventory Helper Methods
    // ==================================================================

    /**
     * Attempts to stack items into existing stacks.
     *
     * @param item Item to stack
     * @return Remaining quantity after stacking
     */
    private int stackIntoExistingItems(Item item) {
        int remaining = item.getQuantity();

        for (Item existing : inventory) {
            if (canStack(existing, item)) {
                int availableSpace = existing.getMaxStackSize() - existing.getQuantity();
                int toTransfer = Math.min(availableSpace, remaining);

                existing.addQuantity(toTransfer);
                remaining -= toTransfer;

                if (remaining <= 0) break;
            }
        }
        return remaining;
    }

    /**
     * Adds an item to a new slot.
     *
     * @param item Item to add
     * @return True if successfully added
     */
    private boolean addToNewSlot(Item item) {
        if (!hasAvailableSlot()) return false;
        if (item.getQuantity() <= 0) return false;

        int stackSize = Math.min(item.getQuantity(), item.getMaxStackSize());
        inventory.add(new Item(item.getType(), stackSize));
        return true;
    }

    /**
     * Checks if two items can be stacked together.
     */
    private boolean canStack(Item existing, Item newItem) {
        return existing != null &&
                newItem != null &&
                existing.getType() == newItem.getType() &&
                existing.getQuantity() < existing.getMaxStackSize();
    }

    /**
     * Checks if the chest has an available slot.
     */
    private boolean hasAvailableSlot() {
        return inventory.size() < capacity;
    }

    /**
     * Validates a slot index.
     */
    private boolean isValidSlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < capacity;
    }

    // ==================================================================
    //  Rendering Helper Methods
    // ==================================================================

    /**
     * Calculates the geometric points for chest rendering.
     */
    private ChestGeometry calculateChestGeometry(
            double screenX, double screenY, double zoom,
            double halfWidth, double halfHeight) {

        // Calculate chest dimensions
        final double chestHalfWidth = halfWidth * CHEST_SIZE_FACTOR;
        final double chestHalfHeight = halfHeight * CHEST_SIZE_FACTOR;

        // Calculate positioning
        final double elevationOffset = halfHeight * CHEST_ELEVATION_OFFSET_FACTOR;
        final double chestTopX = screenX;
        final double chestTopY = screenY - elevationOffset;
        final double chestBodyHeight = chestHalfHeight * CHEST_VISUAL_HEIGHT_FACTOR;

        // Lid points (top diamond)
        final double lidRightX = chestTopX + chestHalfWidth;
        final double lidRightY = chestTopY + chestHalfHeight;
        final double lidBottomX = chestTopX;
        final double lidBottomY = chestTopY + 2 * chestHalfHeight;
        final double lidLeftX = chestTopX - chestHalfWidth;
        final double lidLeftY = chestTopY + chestHalfHeight;

        // Body base points (bottom of chest)
        final double bodyBaseRightY = lidRightY + chestBodyHeight;
        final double bodyBaseBottomY = lidBottomY + chestBodyHeight;
        final double bodyBaseLeftY = lidLeftY + chestBodyHeight;

        // Lock dimensions
        final double lockWidth = chestHalfWidth * LOCK_SIZE_FACTOR;
        final double lockHeight = chestHalfHeight * LOCK_SIZE_FACTOR;
        final double lockX = chestTopX - lockWidth / 2;
        final double lockY = chestTopY + chestHalfHeight - lockHeight / 2;

        return new ChestGeometry(
                lidLeftX, lidLeftY, lidRightX, lidRightY, lidBottomX, lidBottomY,
                bodyBaseLeftY, bodyBaseRightY, bodyBaseBottomY,
                lockX, lockY, lockWidth, lockHeight
        );
    }

    /**
     * Renders the body of the chest.
     */
    private void renderChestBody(GraphicsContext gc, ChestGeometry g, double zoom) {
        gc.setStroke(OUTLINE_COLOR);
        gc.setLineWidth(STROKE_WIDTH * zoom);

        // Left face
        gc.setFill(BODY_LEFT_COLOR);
        gc.beginPath();
        gc.moveTo(g.lidLeftX, g.lidLeftY);
        gc.lineTo(g.lidLeftX, g.bodyBaseLeftY);
        gc.lineTo(g.lidBottomX, g.bodyBaseBottomY);
        gc.lineTo(g.lidBottomX, g.lidBottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // Right face
        gc.setFill(BODY_RIGHT_COLOR);
        gc.beginPath();
        gc.moveTo(g.lidRightX, g.lidRightY);
        gc.lineTo(g.lidRightX, g.bodyBaseRightY);
        gc.lineTo(g.lidBottomX, g.bodyBaseBottomY);
        gc.lineTo(g.lidBottomX, g.lidBottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();
    }

    /**
     * Renders the lid of the chest.
     */
    private void renderChestLid(GraphicsContext gc, ChestGeometry g, double zoom) {
        gc.setFill(LID_TOP_COLOR);
        gc.setStroke(OUTLINE_COLOR);
        gc.setLineWidth(STROKE_WIDTH * zoom);

        gc.beginPath();
        gc.moveTo(g.lidBottomX, g.lidTopY);
        gc.lineTo(g.lidRightX, g.lidRightY);
        gc.lineTo(g.lidBottomX, g.lidBottomY);
        gc.lineTo(g.lidLeftX, g.lidLeftY);
        gc.closePath();
        gc.fill();
        gc.stroke();
    }

    /**
     * Renders the chest lock.
     */
    private void renderChestLock(GraphicsContext gc, ChestGeometry g, double zoom) {
        gc.setFill(LOCK_COLOR);
        gc.fillOval(g.lockX, g.lockY, g.lockWidth, g.lockHeight);
    }

    // ==================================================================
    //  Geometry Data Container
    // ==================================================================

    /**
     * Container class for chest geometry points.
     */
    private static class ChestGeometry {
        final double lidLeftX, lidLeftY;
        final double lidRightX, lidRightY;
        final double lidBottomX, lidBottomY;
        final double lidTopY; // Derived from constructor parameters

        final double bodyBaseLeftY;
        final double bodyBaseRightY;
        final double bodyBaseBottomY;

        final double lockX, lockY;
        final double lockWidth, lockHeight;

        ChestGeometry(
                double lidLeftX, double lidLeftY,
                double lidRightX, double lidRightY,
                double lidBottomX, double lidBottomY,
                double bodyBaseLeftY, double bodyBaseRightY, double bodyBaseBottomY,
                double lockX, double lockY, double lockWidth, double lockHeight
        ) {
            this.lidLeftX = lidLeftX;
            this.lidLeftY = lidLeftY;
            this.lidRightX = lidRightX;
            this.lidRightY = lidRightY;
            this.lidBottomX = lidBottomX;
            this.lidBottomY = lidBottomY;
            this.lidTopY = lidBottomY - 2 * (lidRightY - lidLeftY); // Calculated

            this.bodyBaseLeftY = bodyBaseLeftY;
            this.bodyBaseRightY = bodyBaseRightY;
            this.bodyBaseBottomY = bodyBaseBottomY;

            this.lockX = lockX;
            this.lockY = lockY;
            this.lockWidth = lockWidth;
            this.lockHeight = lockHeight;
        }
    }

}