package com.example.cs360finalproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying inventory items in a RecyclerView.
 * Handles the display, quantity adjustments, and deletion of inventory items.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private final List<InventoryItem> itemList;
    private final Context context;
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onUpdateQuantity(InventoryItem item, long newQuantity);
        void onEditItem(InventoryItem item);
    }

    /**
     * Constructor for the InventoryAdapter.
     */
    public InventoryAdapter(final List<InventoryItem> itemList, final Context context, final OnItemActionListener listener) {
        this.itemList = itemList;
        this.context = context;
        this.listener = listener;
    }

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new InventoryViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        // Inflate the layout for a single list item
        final View itemView = LayoutInflater.from(context).inflate(R.layout.activity_inventory_adjuster, parent, false);

        // Ensure the item view does not take up the full screen height
        ViewGroup.LayoutParams lp = itemView.getLayoutParams();
        if (lp != null) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            itemView.setLayoutParams(lp);
        }

        return new InventoryViewHolder(itemView);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * @param holder The ViewHolder which should be updated to represent the contents of the item.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull final InventoryViewHolder holder, final int position) {
        final InventoryItem item = itemList.get(position);

        // Set item data to TextViews
        holder.itemName.setText(item.getName());
        holder.itemQuantity.setText(formatCompactNumber(item.getQuantity()));
        holder.requiredInventory.setText(formatCompactNumber(item.getRequiredInventory()));

        // Ensure text is visible in both light and dark modes
        int textColor = ThemeUtils.getTextColor(context);
        holder.itemName.setTextColor(textColor);
        holder.requiredInventory.setTextColor(textColor);

        setupQuantityButtons(holder, item);
        checkRequiredInventory(holder, item); // Highlight low inventory status
        
        // Apply theme colors to action buttons
        int primaryColor = ThemeUtils.getPrimaryColor(context);
        holder.buttonAddQuantity.setColorFilter(primaryColor);
        holder.buttonSubtractQuantity.setColorFilter(primaryColor);

        // Click on item name to open edit screen
        holder.itemName.setOnClickListener(v -> listener.onEditItem(item));
    }

    /**
     * Sets up listeners for the add and subtract quantity buttons.
     */
    private void setupQuantityButtons(final InventoryViewHolder holder, final InventoryItem item) {
        // Increase quantity
        holder.buttonAddQuantity.setOnClickListener(v -> updateQuantity(holder, item, item.getQuantity() + 1));

        // Decrease quantity, ensuring it doesn't go below zero
        holder.buttonSubtractQuantity.setOnClickListener(v -> {
            final long newQuantity = item.getQuantity() - 1;
            if (newQuantity >= 0) {
                updateQuantity(holder, item, newQuantity);
            } else {
                Toast.makeText(context, "Quantity cannot be negative.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates the item's quantity via the listener.
     */
    private void updateQuantity(final InventoryViewHolder holder, final InventoryItem item, final long newQuantity) {
        listener.onUpdateQuantity(item, newQuantity);
    }

    /**
     * Highlights the item quantity text if it is below the required restock level.
     */
    void checkRequiredInventory(final InventoryViewHolder holder, final InventoryItem item) {
        if (item.getQuantity() < item.getRequiredInventory()) {
            // Highlight in red for low stock
            holder.itemQuantity.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light));
        } else {
            // Reset to default text color (assuming black or primary text color)
            holder.itemQuantity.setTextColor(ThemeUtils.getTextColor(context));
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items.
     */
    @Override
    public int getItemCount() {
        return itemList.size();
    }

    /**
     * Formats a number into a compact string (e.g., 1.2k, 1.5M) to save space.
     */
    private String formatCompactNumber(long count) {
        if (Math.abs(count) < 10000) return String.format(Locale.getDefault(), "%,d", count);
        int exp = (int) (Math.log(Math.abs(count)) / Math.log(1000));
        if (exp == 0) return String.format(Locale.getDefault(), "%,d", count);
        char pre = "kMGTPE".charAt(exp - 1);
        return String.format(Locale.getDefault(), "%.1f%c", count / Math.pow(1000, exp), pre);
    }

    /**
     * ViewHolder class for holding item views.
     */
    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        final TextView itemName, itemQuantity, requiredInventory;
        final ImageButton buttonAddQuantity, buttonSubtractQuantity;

        public InventoryViewHolder(@NonNull final View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            // Allow item name to wrap to multiple lines to avoid scrolling or small text
            itemName.setSingleLine(false);
            itemName.setMaxLines(2);
            itemName.setEllipsize(android.text.TextUtils.TruncateAt.END);

            itemQuantity = itemView.findViewById(R.id.itemQuantity);
            requiredInventory = itemView.findViewById(R.id.requiredInventory);
            buttonAddQuantity = itemView.findViewById(R.id.buttonAddQuantity);
            buttonSubtractQuantity = itemView.findViewById(R.id.buttonSubtractQuantity);
        }
    }
}