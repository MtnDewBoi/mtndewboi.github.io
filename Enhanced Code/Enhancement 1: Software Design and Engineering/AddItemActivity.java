package com.example.cs360finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Activity for adding a new inventory item or updating an existing one.
 */
public class AddItemActivity extends AppCompatActivity {

    private EditText itemNameEditText, itemQuantityEditText, requiredItemQuantityEditText;
    private InventoryViewModel inventoryViewModel;
    private LinearLayout graphContainer;
    private int editingItemId = -1;

    /**
     * Called when the activity is first created.
     * Initializes the UI, ViewModel, and handles intent data for editing vs adding.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this); // Apply theme before setContentView
        super.onCreate(savedInstanceState);

        // Create a wrapper layout to add the graph below the existing XML layout
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout rootContainer = new LinearLayout(this);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(rootContainer);

        View originalContent = getLayoutInflater().inflate(R.layout.activity_additem, rootContainer, false);
        // Ensure the inflated layout doesn't force match_parent height, which can conflict inside a ScrollView
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        originalContent.setLayoutParams(layoutParams);
        rootContainer.addView(originalContent);

        graphContainer = new LinearLayout(this);
        graphContainer.setOrientation(LinearLayout.VERTICAL);
        rootContainer.addView(graphContainer);

        setContentView(scrollView);

        inventoryViewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // Initialize UI elements
        itemNameEditText = findViewById(R.id.itemNameEditText);
        itemQuantityEditText = findViewById(R.id.itemQuantityEditText);
        requiredItemQuantityEditText = findViewById(R.id.requiredItemQuantityEditText);

        // Add TextWatchers to format numbers with commas
        itemQuantityEditText.addTextChangedListener(new NumberTextWatcher(itemQuantityEditText));
        requiredItemQuantityEditText.addTextChangedListener(new NumberTextWatcher(requiredItemQuantityEditText));

        final Button saveItemButton = findViewById(R.id.saveItemButton);
        final Button goBackButton = findViewById(R.id.goBackButton);
        final Button deleteItemButton = findViewById(R.id.deleteItemButton);

        // Fix for tablet layout: restrict width of input fields and buttons
        if (getResources().getConfiguration().screenWidthDp >= 600) {
            int widthPixels = (int) (400 * getResources().getDisplayMetrics().density);
            adjustViewWidth(itemNameEditText, widthPixels);
            adjustViewWidth(itemQuantityEditText, widthPixels);
            adjustViewWidth(requiredItemQuantityEditText, widthPixels);
            adjustViewWidth(saveItemButton, widthPixels);
            adjustViewWidth(goBackButton, widthPixels);
            adjustViewWidth(deleteItemButton, widthPixels);
        }

        final Intent intent = getIntent();
        if (intent.hasExtra("ITEM_ID")) {
            editingItemId = intent.getIntExtra("ITEM_ID", -1);
            loadItemData(editingItemId);
            // Update the button text to reflect editing mode
            saveItemButton.setText("Save Changes");
            deleteItemButton.setVisibility(View.VISIBLE);
        } else {
            // New Item Mode
            saveItemButton.setText("Add New Item");
        }

        saveItemButton.setOnClickListener(v -> addItemToInventory());
        goBackButton.setOnClickListener(v -> finish());
        deleteItemButton.setOnClickListener(v -> confirmDelete());
        
        // Apply Theme
        int textColor = ThemeUtils.getTextColor(this);
        int hintColor = ThemeUtils.getHintColor(this);
        int textBoxColor = ThemeUtils.getTextBoxBackgroundColor(this);
        findViewById(android.R.id.content).setBackgroundColor(ThemeUtils.getBackgroundColor(this));
        saveItemButton.setBackgroundColor(ThemeUtils.getPrimaryColor(this));
        goBackButton.setBackgroundColor(ThemeUtils.getPrimaryColor(this));
        itemNameEditText.setTextColor(textColor);
        itemNameEditText.setHintTextColor(hintColor);
        itemNameEditText.setBackgroundColor(textBoxColor);
        itemQuantityEditText.setTextColor(textColor);
        itemQuantityEditText.setHintTextColor(hintColor);
        itemQuantityEditText.setBackgroundColor(textBoxColor);
        requiredItemQuantityEditText.setTextColor(textColor);
        requiredItemQuantityEditText.setHintTextColor(hintColor);
        requiredItemQuantityEditText.setBackgroundColor(textBoxColor);
    }

    /**
     * Handles the process of loading items from the database.
     */
    private void loadItemData(final int itemId) {
        final InventoryItem item = inventoryViewModel.getItemById(itemId);

        if (item != null) {
            itemNameEditText.setText(item.getName());
            itemQuantityEditText.setText(String.valueOf(item.getQuantity()));
            requiredItemQuantityEditText.setText(String.valueOf(item.getRequiredInventory()));
            loadUsageGraph(item);
        } else {
            Toast.makeText(this, "Error: Item not found for editing.", Toast.LENGTH_LONG).show();
            // Fall back to Add mode or just finish
            editingItemId = -1;
        }
    }

    /**
     * Loads and displays the usage graph for the specific item.
     */
    private void loadUsageGraph(InventoryItem item) {
        TextView title = new TextView(this);
        title.setText("2-Week Usage History");
        title.setTextSize(18);
        title.setPadding(32, 16, 32, 16);
        title.setTextColor(ThemeUtils.getTextColor(this));
        // Center title if tablet
        if (getResources().getConfiguration().screenWidthDp >= 600) {
            title.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        }
        graphContainer.addView(title);

        long duration = 14 * 24 * 60 * 60 * 1000L; // 2 weeks
        long step = 12 * 60 * 60 * 1000L; // 12 hour steps
        Map<String, List<Integer>> history = inventoryViewModel.getUsageHistory(duration, step);
        List<Integer> itemData = history.get(item.getName());

        Map<String, List<Integer>> graphData = new HashMap<>();
        if (itemData != null) {
            graphData.put(item.getName(), itemData);
        } else {
            // Handle case where data might be missing (e.g. new item not yet in history map)
            graphData.put(item.getName(), java.util.Collections.nCopies(28, 0));
        }

        UsageGraphView graph = new UsageGraphView(this, graphData, ThemeUtils.getPrimaryColor(this), ThemeUtils.getTextColor(this), System.currentTimeMillis() - duration, step);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500);
        params.setMargins(32, 0, 32, 32);
        graph.setLayoutParams(params);
        graphContainer.addView(graph);
    }

    /**
     * Handles the process of adding or updating an item in the database.
     */
    private void addItemToInventory() {
        final String itemName = itemNameEditText.getText().toString().trim();
        final String itemQuantityStr = itemQuantityEditText.getText().toString().trim().replace(",", "");
        final String requiredItemQuantityStr = requiredItemQuantityEditText.getText().toString().trim().replace(",", "");

        if (itemName.isEmpty() || itemQuantityStr.isEmpty() || requiredItemQuantityStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        long itemQuantity;
        long requiredInventory;
        try {
            itemQuantity = Long.parseLong(itemQuantityStr);
            requiredInventory = Long.parseLong(requiredItemQuantityStr);
            if (itemQuantity < 0 || requiredInventory < 0) {
                Toast.makeText(this, "Quantity and Required Inventory cannot be negative.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (final NumberFormatException e) {
            Toast.makeText(this, "Quantity and Required Inventory must be valid numbers.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for duplicate name
        final InventoryItem existingItem = inventoryViewModel.getItemByName(itemName);
        if (existingItem != null) {
            if (editingItemId == -1) {
                // Adding new item, name taken
                Toast.makeText(this, "Item name already exists. Please use the Edit button on the inventory screen.", Toast.LENGTH_LONG).show();
                return;
            } else if (existingItem.getId() != editingItemId) {
                // Editing item, name taken by another item
                Toast.makeText(this, "Item name already exists.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        boolean success = false;
        String toastMessage = "";

        if (editingItemId != -1) {
            // **EDIT MODE** - Update existing item details
            success = inventoryViewModel.updateItemDetails(editingItemId, itemName, itemQuantity, requiredInventory);
            toastMessage = "Item '" + itemName + "' updated successfully.";
        } else {
            // **ADD MODE** - Add a new item
            success = inventoryViewModel.addItem(itemName, itemQuantity, requiredInventory);
            toastMessage = "New item '" + itemName + "' added successfully.";
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

    /**
     * Helper method to adjust view width and center it for tablets.
     */
    private void adjustViewWidth(View view, int width) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.width = width;
            if (params instanceof android.widget.LinearLayout.LayoutParams) {
                ((android.widget.LinearLayout.LayoutParams) params).gravity = android.view.Gravity.CENTER_HORIZONTAL;
            }
            else if (params instanceof android.widget.FrameLayout.LayoutParams) {
                ((android.widget.FrameLayout.LayoutParams) params).gravity = android.view.Gravity.CENTER_HORIZONTAL;
            }
            else if (params instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams constraintParams = (ConstraintLayout.LayoutParams) params;
                constraintParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                constraintParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            }
            view.setLayoutParams(params);
        }
    }

    /**
     * Shows a confirmation dialog before deleting the item.
     */
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (inventoryViewModel.deleteItem(editingItemId)) {
                        Toast.makeText(this, "Item deleted.", Toast.LENGTH_SHORT).show();
                        // Navigate back to Overview
                        Intent intent = new Intent(AddItemActivity.this, OverviewActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete item.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * TextWatcher to format numbers with commas as the user types.
     */
    private static class NumberTextWatcher implements android.text.TextWatcher {
        private final java.lang.ref.WeakReference<EditText> editTextWeakReference;

        public NumberTextWatcher(EditText editText) {
            this.editTextWeakReference = new java.lang.ref.WeakReference<>(editText);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(android.text.Editable s) {
            EditText editText = editTextWeakReference.get();
            if (editText == null) return;
            editText.removeTextChangedListener(this);

            try {
                String originalString = s.toString();
                if (!originalString.isEmpty()) {
                    String cleanString = originalString.replace(",", "");
                    long parsed = Long.parseLong(cleanString);
                    String formattedString = String.format(java.util.Locale.US, "%,d", parsed);

                    if (!formattedString.equals(originalString)) {
                        editText.setText(formattedString);
                        editText.setSelection(formattedString.length());
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore errors
            }

            editText.addTextChangedListener(this);
        }
    }
}