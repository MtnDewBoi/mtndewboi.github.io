package com.example.cs360finalproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog; // Added import for AlertDialog

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying inventory items in a RecyclerView.
 * Handles the display, quantity adjustments, and deletion of inventory items.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private final List<InventoryItem> itemList;
    private final DatabaseHelper databaseHelper;
    private final Context context;
    private boolean isSmsEnabled; // State from OverviewActivity/SharedPreferences

    /**
     * Constructor for the InventoryAdapter.
     */
    public InventoryAdapter(final List<InventoryItem> itemList, final Context context, final DatabaseHelper databaseHelper) {
        this.itemList = itemList;
        this.context = context;
        this.databaseHelper = databaseHelper;
    }

    /**
     * Updates the SMS enabled state and refreshes the list to apply any necessary UI changes (e.g., color indicators).
     * @param isEnabled The current SMS notification preference.
     */
    public void setSmsEnabled(final boolean isEnabled) {
        this.isSmsEnabled = isEnabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        // Inflate the layout for a single list item
        final View itemView = LayoutInflater.from(context).inflate(R.layout.activity_inventory_adjuster, parent, false);
        return new InventoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final InventoryViewHolder holder, final int position) {
        final InventoryItem item = itemList.get(position);

        // Set item data to TextViews
        holder.itemName.setText(item.getName());
        holder.itemQuantity.setText(String.valueOf(item.getQuantity()));
        holder.requiredInventory.setText(String.valueOf(item.getRequiredInventory()));

        setupQuantityButtons(holder, item);
        setupDeleteButton(holder, position);
        setupEditButton(holder, item);
        checkRequiredInventory(holder, item); // Highlight low inventory status
    }
    /**
     * Sets up listeners for the edit button..
     */
    private void setupEditButton(final InventoryViewHolder holder, final InventoryItem item) {
        holder.buttonEditItem.setOnClickListener(v -> {
            // Launch AddItemActivity and pass the item ID
            final Intent intent = new Intent(context, AddItemActivity.class);
            intent.putExtra("ITEM_ID", item.getId());
            context.startActivity(intent);
        });
    }

    /**
     * Sets up listeners for the add and subtract quantity buttons.
     */
    private void setupQuantityButtons(final InventoryViewHolder holder, final InventoryItem item) {
        // Increase quantity
        holder.buttonAddQuantity.setOnClickListener(v -> updateQuantity(holder, item, item.getQuantity() + 1));

        // Decrease quantity, ensuring it doesn't go below zero
        holder.buttonSubtractQuantity.setOnClickListener(v -> {
            final int newQuantity = item.getQuantity() - 1;
            if (newQuantity >= 0) {
                updateQuantity(holder, item, newQuantity);
            } else {
                Toast.makeText(context, "Quantity cannot be negative.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sets up a confirmation dialog for deleting an item.
     */
    private void setupDeleteButton(final InventoryViewHolder holder, final int position) {
        holder.buttonDeleteItem.setOnClickListener(v -> {
            // Show a confirmation dialog before deleting
            new AlertDialog.Builder(context)
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete '" + itemList.get(position).getName() + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteItem(position))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /**
     * Updates the item's quantity in the database and UI.
     * @param holder The ViewHolder to update.
     * @param item The InventoryItem object.
     * @param newQuantity The new quantity value.
     */
    private void updateQuantity(final InventoryViewHolder holder, final InventoryItem item, final int newQuantity) {
        item.setQuantity(newQuantity);
        final boolean success = databaseHelper.updateItemQuantity(item.getId(), newQuantity);

        if (success) {
            holder.itemQuantity.setText(String.valueOf(newQuantity));
            checkRequiredInventory(holder, item); // Update color after quantity change

            // SMS notification logic only for user feedback
            if (isSmsEnabled && newQuantity == 0) {
                final String phoneNumber = getPhoneNumber();
                if (!phoneNumber.isEmpty()) {
                    // The actual SMS sending logic is handled inside DatabaseHelper.updateItemQuantity()
                    // This toast just confirms to the user that the alert was *triggered*.
                    Toast.makeText(context, "Item reached zero. SMS restock alert triggered.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(context, "Failed to update quantity.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes the item from the database and removes it from the RecyclerView list.
     * @param position The position of the item in the list.
     */
    private void deleteItem(final int position) {
        final InventoryItem item = itemList.get(position);
        final boolean success = databaseHelper.deleteItem(item.getId());
        if (success) {
            itemList.remove(position);
            notifyItemRemoved(position); // Notify adapter of the removal
            notifyItemRangeChanged(position, itemList.size()); // Update subsequent items' positions
            Toast.makeText(context, "Item deleted.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Failed to delete item.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Highlights the item quantity text if it is below the required restock level.
     */
    private void checkRequiredInventory(final InventoryViewHolder holder, final InventoryItem item) {
        if (item.getQuantity() < item.getRequiredInventory()) {
            // Highlight in red for low stock
            holder.itemQuantity.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light));
        } else {
            // Reset to default text color (assuming black or primary text color)
            holder.itemQuantity.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        }
    }

    /**
     * Retrieves the stored phone number from SharedPreferences.
     */
    private String getPhoneNumber() {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(SMSPermissionsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(SMSPermissionsActivity.KEY_PHONE_NUMBER, "");
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    /**
     * ViewHolder class for holding item views.
     */
    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        final TextView itemName, itemQuantity, requiredInventory;
        final ImageButton buttonAddQuantity, buttonSubtractQuantity, buttonDeleteItem, buttonEditItem;

        public InventoryViewHolder(@NonNull final View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            itemQuantity = itemView.findViewById(R.id.itemQuantity);
            requiredInventory = itemView.findViewById(R.id.requiredInventory);
            buttonEditItem = itemView.findViewById(R.id.buttonEditItem);
            buttonAddQuantity = itemView.findViewById(R.id.buttonAddQuantity);
            buttonSubtractQuantity = itemView.findViewById(R.id.buttonSubtractQuantity);
            buttonDeleteItem = itemView.findViewById(R.id.buttonDeleteItem);
        }
    }
}