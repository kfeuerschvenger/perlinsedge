package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a Chest building.
 * Chests are non-solid (for player movement, though they block building on their tile)
 * and act as containers for items.
 */
public class Chest extends Building implements Container {
    // Factor de tamaño del cofre en relación con las dimensiones de medio tile
    private static final double CHEST_SIZE_FACTOR = 0.6;
    // Factor para la altura visual del cofre, en relación con la altura isométrico 'medio' del tile
    private static final double CHEST_VISUAL_HEIGHT_FACTOR = 1.2; // Para que parezca un cubo o ligeramente más alto
    // Factor para elevar el cofre por encima del tile base
    private static final double CHEST_ELEVATION_OFFSET_FACTOR = 0.3; // Eleva el cofre ligeramente del suelo

    private List<Item> inventory;
    private int capacity; // Max number of unique item stacks or total items

    public Chest(int tileX, int tileY, int capacity) {
        super(tileX, tileY, BuildingType.CHEST, true, true);
        this.inventory = new ArrayList<>();
        this.capacity = capacity; // e.g., 9 or 18 slots
    }

    @Override
    public List<Item> getItems() {
        return new ArrayList<>(inventory); // Return a copy to prevent external modification
    }

    @Override
    public Item getItemAt(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= inventory.size()) return null;
        return inventory.get(slotIndex);
    }

    @Override
    public void setItemAt(int slotIndex, Item item) {
        if (slotIndex < 0) return;

        if (slotIndex < inventory.size()) {
            inventory.set(slotIndex, item);
        } else if (slotIndex == inventory.size() && inventory.size() < capacity) {
            inventory.add(item);
        }
    }

    @Override
    public boolean addItem(Item item) {
        if (item == null || item.getQuantity() <= 0) return false;

        // Try to stack with existing items
        for (Item existingItem : inventory) {
            if (existingItem.getType() == item.getType() && existingItem.getQuantity() < existingItem.getMaxStackSize()) {
                int canAdd = existingItem.getMaxStackSize() - existingItem.getQuantity();
                int toAdd = Math.min(canAdd, item.getQuantity());
                existingItem.setQuantity(existingItem.getQuantity() + toAdd);
                item.setQuantity(item.getQuantity() - toAdd);
                if (item.getQuantity() == 0) return true; // All items added
            }
        }

        // Add to new slot if space available
        if (inventory.size() < capacity) {
            int toAdd = Math.min(item.getQuantity(), item.getMaxStackSize());
            inventory.add(new Item(item.getType(), toAdd)); // Add a new stack
            item.setQuantity(item.getQuantity() - toAdd);
            return true; // Item added (partially or fully)
        }
        return false; // No space
    }

    @Override
    public boolean removeItem(Item itemToRemove, int quantity) {
        if (itemToRemove == null || quantity <= 0) return false;

        Item foundItem = null;
        for (Item item : inventory) {
            if (item.getType() == itemToRemove.getType()) {
                foundItem = item;
                break;
            }
        }

        if (foundItem != null && foundItem.getQuantity() >= quantity) {
            foundItem.setQuantity(foundItem.getQuantity() - quantity);
            if (foundItem.getQuantity() <= 0) {
                inventory.remove(foundItem);
            }
            return true;
        }
        return false; // Item not found or not enough quantity
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
        // Check if there's space in existing stacks
        for (Item existingItem : inventory) {
            if (existingItem.getType() == item.getType() && existingItem.getQuantity() < existingItem.getMaxStackSize()) {
                return true;
            }
        }
        // Check if there's an empty slot
        return inventory.size() < capacity;
    }

    @Override
    public boolean canTakeItem(Item item, int quantity) {
        // Check if there's enough of the item to remove
        return getItemCount(item) >= quantity;
    }

    @Override
    public void setItems(List<Item> items) {
        this.inventory = items.stream()
                .filter(item -> item != null && item.getQuantity() > 0)
                .collect(Collectors.toList());
        // Optionally enforce capacity here if loading from save data
    }


    @Override
    public boolean onInteract(Tile tile) {
        System.out.println("Interacted with Chest at (" + tileX + ", " + tileY + "). Opening inventory.");
        // This interaction will be handled by BuildingManager/ContainerManager
        return true;
    }

    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        final AppConfig config = AppConfig.getInstance();
        double halfW = config.graphics().getTileHalfWidth() * zoom;
        double halfH = config.graphics().getTileHalfHeight() * zoom;

        // Calcular las dimensiones del cofre basándose en el factor de tamaño
        double chestHalfW = halfW * CHEST_SIZE_FACTOR;
        double chestHalfH = halfH * CHEST_SIZE_FACTOR;

        // Calcular el desplazamiento de elevación para que el cofre flote sobre el tile
        // screenY es el punto superior del diamante del tile.
        double elevationOffset = halfH * CHEST_ELEVATION_OFFSET_FACTOR;

        // Ajustar el punto superior del cofre hacia arriba. Este será el punto "top" del diamante superior del cofre.
        double chestTopX = screenX;
        double chestTopY = screenY - elevationOffset;

        // Altura visual del cuerpo del cofre (sin la tapa)
        double chestBodyHeight = chestHalfH * CHEST_VISUAL_HEIGHT_FACTOR; // Ajusta para que sea un cubo proporcional

        // --- Calcular los puntos de polígono para el diamante superior (tapa del cofre) ---
        final double lidRightX = chestTopX + chestHalfW;
        final double lidRightY = chestTopY + chestHalfH;
        final double lidBottomX = chestTopX;
        final double lidBottomY = chestTopY + 2 * chestHalfH;
        final double lidLeftX = chestTopX - chestHalfW;
        final double lidLeftY = chestTopY + chestHalfH;

        // --- Calcular los puntos base para los bordes inferiores del cuerpo del cofre ---
        // Estos se proyectan hacia abajo desde los puntos del diamante superior (lid) por chestBodyHeight
        final double bodyBaseRightY = lidRightY + chestBodyHeight;
        final double bodyBaseBottomY = lidBottomY + chestBodyHeight;
        final double bodyBaseLeftY = lidLeftY + chestBodyHeight;

        // Colores del cofre
        Color chestBrown = Color.rgb(139, 69, 19); // SaddleBrown
        Color lidTopColor = chestBrown.deriveColor(0, 1, 1.2, 1); // Tapa más clara
        Color bodyRightColor = chestBrown.deriveColor(0, 1, 0.8, 1); // Cuerpo derecho más oscuro
        Color bodyLeftColor = chestBrown.deriveColor(0, 1, 0.9, 1); // Cuerpo izquierdo con tono medio

        // Contorno
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0 * zoom); // Grosor del contorno que escala con el zoom

        // 1. Dibujar Cara Izquierda del cuerpo del cofre
        gc.setFill(bodyLeftColor);
        gc.beginPath();
        gc.moveTo(lidLeftX, lidLeftY);
        gc.lineTo(lidLeftX, bodyBaseLeftY);
        gc.lineTo(lidBottomX, bodyBaseBottomY);
        gc.lineTo(lidBottomX, lidBottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // 2. Dibujar Cara Derecha del cuerpo del cofre
        gc.setFill(bodyRightColor);
        gc.beginPath();
        gc.moveTo(lidRightX, lidRightY);
        gc.lineTo(lidRightX, bodyBaseRightY);
        gc.lineTo(lidBottomX, bodyBaseBottomY);
        gc.lineTo(lidBottomX, lidBottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // 3. Dibujar Cara Superior (tapa del cofre)
        gc.setFill(lidTopColor);
        gc.beginPath();
        gc.moveTo(chestTopX, chestTopY);
        gc.lineTo(lidRightX, lidRightY);
        gc.lineTo(lidBottomX, lidBottomY);
        gc.lineTo(lidLeftX, lidLeftY);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // Opcional: Añadir detalles como el cierre o una franja en la tapa
        // Esto sería un rectángulo más pequeño en la cara superior
        double lockWidth = chestHalfW * 0.4;
        double lockHeight = chestHalfH * 0.4;
        gc.setFill(Color.DARKGRAY); // Color del cierre
        gc.fillOval(chestTopX - lockWidth / 2, chestTopY + chestHalfH - lockHeight / 2, lockWidth, lockHeight); // Centro del diamante
    }

}
