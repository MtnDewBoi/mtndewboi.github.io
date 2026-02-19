package com.example.cs360finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for adding a new inventory item or updating an existing one.
 */
public class AddItemActivity extends AppCompatActivity {

    private EditText itemNameEditText, itemQuantityEditText, requiredItemQuantityEditText;
    private DatabaseHelper dbHelper;
    private int editingItemId = -1;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additem);

        dbHelper = new DatabaseHelper(this); // Initialize DatabaseHelper once

        // Initialize UI elements
        itemNameEditText = findViewById(R.id.itemNameEditText);
        itemQuantityEditText = findViewById(R.id.itemQuantityEditText);
        requiredItemQuantityEditText = findViewById(R.id.requiredItemQuantityEditText);
        final Button saveItemButton = findViewById(R.id.saveItemButton);
        final Button goBackButton = findViewById(R.id.goBackButton);

        final Intent intent = getIntent();
        if (intent.hasExtra("ITEM_ID")) {
            editingItemId = intent.getIntExtra("ITEM_ID", -1);
            loadItemData(editingItemId);
            // Update the button text to reflect editing mode
            saveItemButton.setText("Save Changes");
        } else {
            // New Item Mode
            saveItemButton.setText("Add New Item");
        }

        saveItemButton.setOnClickListener(v -> addItemToInventory());
        goBackButton.setOnClickListener(v -> finish());
    }

    /**
     * Handles the process of loading items from the database.
     */
    private void loadItemData(final int itemId) {
        final InventoryItem item = dbHelper.getItemById(itemId);

        if (item != null) {
            itemNameEditText.setText(item.getName());
            itemQuantityEditText.setText(String.valueOf(item.getQuantity()));
            requiredItemQuantityEditText.setText(String.valueOf(item.getRequiredInventory()));
            // Name should not be editable in edit mode to prevent duplicates
            itemNameEditText.setEnabled(false);
        } else {
            Toast.makeText(this, "Error: Item not found for editing.", Toast.LENGTH_LONG).show();
            // Fall back to Add mode or just finish
            editingItemId = -1;
        }
    }

    /**
     * Handles the process of adding or updating an item in the database.
     */
    private void addItemToInventory() {
        final String itemName = itemNameEditText.getText().toString().trim();
        final String itemQuantityStr = itemQuantityEditText.getText().toString().trim();
        final String requiredItemQuantityStr = requiredItemQuantityEditText.getText().toString().trim();

        if (itemName.isEmpty() || itemQuantityStr.isEmpty() || requiredItemQuantityStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int itemQuantity;
        int requiredInventory;
        try {
            itemQuantity = Integer.parseInt(itemQuantityStr);
            requiredInventory = Integer.parseInt(requiredItemQuantityStr);
            if (itemQuantity < 0 || requiredInventory < 0) {
                Toast.makeText(this, "Quantity and Required Inventory cannot be negative.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (final NumberFormatException e) {
            Toast.makeText(this, "Quantity and Required Inventory must be valid numbers.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = false;
        String toastMessage = "";

        if (editingItemId != -1) {
            // **EDIT MODE** - Update existing item details
            success = dbHelper.updateItemDetails(editingItemId, itemName, itemQuantity, requiredInventory);
            toastMessage = "Item '" + itemName + "' updated successfully.";
        } else {
            // **ADD MODE** - Add a new item
            final InventoryItem existingItem = dbHelper.getItemByName(itemName);
            if (existingItem != null) {
                // If the item exists, perform update logic (as per original intent, though updateItemDetails is better)
                // However, based on the original code, we will prevent adding a duplicate name
                // in 'Add' mode and prompt the user to use the 'Edit' feature instead.
                Toast.makeText(this, "Item name already exists. Please use the Edit button on the inventory screen.", Toast.LENGTH_LONG).show();
                return;
            } else {
                success = dbHelper.addItem(itemName, itemQuantity, requiredInventory);
                toastMessage = "New item '" + itemName + "' added successfully.";
            }
        }

        if (success) {
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
            // Navigate back to the OverviewActivity and refresh the list
            final Intent intent = new Intent(AddItemActivity.this, OverviewActivity.class);
            // These flags ensure the OverviewActivity is at the top and will refresh its data in onResume
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Operation failed. Database error.", Toast.LENGTH_LONG).show();
        }
    }
}