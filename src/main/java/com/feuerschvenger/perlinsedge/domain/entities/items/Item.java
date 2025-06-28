package com.feuerschvenger.perlinsedge.domain.entities.items;

import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import javafx.scene.image.Image;

import java.util.Objects;

public class Item {
    private final ItemType type;
    private int quantity;

    public Item(ItemType type, int quantity) {
        this.type = Objects.requireNonNull(type, "ItemType cannot be null.");
        if (quantity <= 0) {
            throw new IllegalArgumentException("The quantity of the item must be positive.");
        }
        this.quantity = quantity;
    }

    public ItemType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMaxStackSize() {
        return type.getMaxStackSize();
    }

    public void addQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add a negative quantity. Use removeQuantity.");
        }
        this.quantity += amount;
    }

    public String getDisplayName() {
        return type.getDisplayName();
    }

    public String getDescription() {
        return type.getDescription();
    }

    public void removeQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot remove negative quantity. Use addQuantity.");
        }
        this.quantity = Math.max(0, this.quantity - amount);
    }

    public boolean isEmpty() {
        return this.quantity <= 0;
    }

    public Image getSprite() {
        return SpriteManager.getInstance().getItemSprite(type).getImage();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return type == item.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return quantity + "x " + type.name();
    }

}