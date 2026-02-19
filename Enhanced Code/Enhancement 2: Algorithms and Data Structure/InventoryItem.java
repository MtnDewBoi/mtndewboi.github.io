package com.example.cs360finalproject;

/**
 * Represents an inventory item with properties like ID, name, quantity, and required restock level.
 * This is a simple POJO (Plain Old Java Object) for data transfer.
 */
public class InventoryItem {

    private final int id; // Unique identifier for the item (immutable)
    private String name; // Name of the item
    private long quantity; // Quantity of the item in inventory (can be modified)
    private long requiredInventory; // The minimum quantity required in stock (restock threshold) (can be modified)

    /**
     * Constructor for creating an InventoryItem object.
     *
     * @param id Unique identifier for the item
     * @param name Name of the item
     * @param quantity Initial quantity of the item
     * @param requiredInventory The minimum quantity required in stock.
     */
    public InventoryItem(final int id, final String name, final long quantity, final long requiredInventory) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.requiredInventory = requiredInventory;
    }

    /**
     * Gets the unique identifier of the item.
     * @return The item's ID
     */
    public int getId() { return id; }

    /**
     * Gets the name of the item.
     * @return The item's name
     */
    public String getName() { return name; }

    /**
     * Gets the quantity of the item.
     * @return The item's quantity
     */
    public long getQuantity() { return quantity; }

    /**
     * Gets the required restock quantity.
     * @return The item's required quantity
     */
    public long getRequiredInventory() { return requiredInventory; }

    /**
     * Updates the name of the item.
     * @param name The new name to set for the item
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Updates the quantity of the item.
     * @param quantity The new quantity to set for the item
     */
    public void setQuantity(final long quantity) { this.quantity = quantity; }

    /**
     * Updates the required inventory level.
     * @param requiredInventory The new required inventory level
     */
    public void setRequiredInventory(final long requiredInventory) {
        this.requiredInventory = requiredInventory;
    }
}