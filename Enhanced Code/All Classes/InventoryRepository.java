package com.example.cs360finalproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Repository class for handling Inventory-related database operations.
 */
public class InventoryRepository {

    private static final String TAG = "InventoryRepository";
    private static final String TABLE_INVENTORY = "inventory";

    private final DatabaseHelper dbHelper;

    public InventoryRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    /**
     * Adds a new item to the inventory.
     * @param name Name of the item.
     * @param quantity Initial quantity.
     * @param requiredInventory Required stock level.
     * @return true if successful, false otherwise.
     */
    public boolean addItem(final String name, final long quantity, final long requiredInventory) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ITEM_NAME, name);
        values.put(DatabaseHelper.COLUMN_ITEM_QUANTITY, quantity);
        values.put(DatabaseHelper.COLUMN_ITEM_REQUIREDINVENTORY, requiredInventory);

        final long result = db.insert(TABLE_INVENTORY, null, values);
        return result != -1;
    }

    /**
     * Updates the quantity of a specific item.
     * @param itemId The ID of the item.
     * @param newQuantity The new quantity.
     * @return true if successful, false otherwise.
     */
    public boolean updateItemQuantity(final int itemId, final long newQuantity) {
        // Fetch old quantity to calculate change
        InventoryItem item = getItemById(itemId);
        long oldQuantity = (item != null) ? item.getQuantity() : 0;
        long diff = newQuantity - oldQuantity;

        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ITEM_QUANTITY, newQuantity);

        final int rowsAffected = db.update(TABLE_INVENTORY, values, DatabaseHelper.COLUMN_ITEM_ID + "=?", new String[]{String.valueOf(itemId)});
        
        if (rowsAffected > 0 && diff != 0) {
            logUsage(itemId, diff);
        }
        return rowsAffected > 0;
    }

    /**
     * Deletes an item from the inventory.
     * @param itemId The ID of the item to delete.
     * @return true if successful, false otherwise.
     */
    public boolean deleteItem(final int itemId) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int rows = db.delete(TABLE_INVENTORY, DatabaseHelper.COLUMN_ITEM_ID + " = ?", new String[]{String.valueOf(itemId)});
        return rows > 0;
    }

    /**
     * Retrieves all items from the inventory.
     * @return A list of InventoryItem objects.
     */
    public List<InventoryItem> getAllItems() {
        final List<InventoryItem> itemList = new ArrayList<>();
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_INVENTORY, null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    final int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_ID));
                    final String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_NAME));
                    final long quantity = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_QUANTITY));
                    final long requiredInventory = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_REQUIREDINVENTORY));
                    itemList.add(new InventoryItem(id, name, quantity, requiredInventory));
                } while (cursor.moveToNext());
            }
        } catch (final Exception e) {
            Log.e(TAG, "Error while getting all items: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return itemList;
    }

    /**
     * Retrieves a single item by its name.
     * @param name The name of the item.
     * @return The InventoryItem, or null if not found.
     */
    public InventoryItem getItemByName(final String name) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        InventoryItem item = null;
        try {
            final String selection = DatabaseHelper.COLUMN_ITEM_NAME + " = ?";
            final String[] selectionArgs = {name};
            cursor = db.query(TABLE_INVENTORY, null, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                final int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_ID));
                final String itemName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_NAME));
                final long quantity = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_QUANTITY));
                final long requiredInventory = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_REQUIREDINVENTORY));
                item = new InventoryItem(id, itemName, quantity, requiredInventory);
            }
        } catch (final Exception e) {
            Log.e(TAG, "Error while getting item by name: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return item;
    }

    /**
     * Retrieves a single item by its ID.
     * @param id The ID of the item.
     * @return The InventoryItem, or null if not found.
     */
    public InventoryItem getItemById(final int id) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        InventoryItem item = null;
        try {
            cursor = db.query(TABLE_INVENTORY, null, DatabaseHelper.COLUMN_ITEM_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int itemId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_ID));
                final String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_NAME));
                final long quantity = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_QUANTITY));
                final long required = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ITEM_REQUIREDINVENTORY));
                item = new InventoryItem(itemId, name, quantity, required);
            }
        } catch (final Exception e) {
            Log.e(TAG, "Error while getting item by ID: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return item;
    }

    /**
     * Updates the details of an existing item.
     * @param id The ID of the item.
     * @param name The new name.
     * @param quantity The new quantity.
     * @param requiredInventory The new required inventory level.
     * @return true if successful, false otherwise.
     */
    public boolean updateItemDetails(final int id, final String name, final long quantity, final long requiredInventory) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ITEM_NAME, name);
        values.put(DatabaseHelper.COLUMN_ITEM_QUANTITY, quantity);
        values.put(DatabaseHelper.COLUMN_ITEM_REQUIREDINVENTORY, requiredInventory);

        final int rowsAffected = db.update(TABLE_INVENTORY, values, DatabaseHelper.COLUMN_ITEM_ID + "=?", new String[]{String.valueOf(id)});
        
        return rowsAffected > 0;
    }

    /**
     * Logs a change in inventory quantity.
     * @param itemId The ID of the item.
     * @param change The amount changed (positive or negative).
     */
    private void logUsage(int itemId, long change) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_LOG_ITEM_ID, itemId);
        values.put(DatabaseHelper.COLUMN_LOG_CHANGE, change);
        values.put(DatabaseHelper.COLUMN_LOG_TIMESTAMP, System.currentTimeMillis());
        db.insert(DatabaseHelper.TABLE_USAGE_LOG, null, values);
    }

    /**
     * Generates a list of recommended orders based on hourly usage rates.
     * @return A list of recommendation strings.
     */
    public List<String> getOrderRecommendations() {
        List<String> recommendations = new ArrayList<>();
        List<InventoryItem> items = getAllItems();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // Analyze last 24 hours for a more stable usage rate
        long analysisDuration = 24 * 60 * 60 * 1000L;
        long timeThreshold = System.currentTimeMillis() - analysisDuration;

        // Pre-fetch usage data to avoid N+1 queries
        Map<Integer, Long> usageMap = new HashMap<>();
        String usageQuery = "SELECT " + DatabaseHelper.COLUMN_LOG_ITEM_ID + ", SUM(" + DatabaseHelper.COLUMN_LOG_CHANGE + ") FROM " + DatabaseHelper.TABLE_USAGE_LOG +
                " WHERE " + DatabaseHelper.COLUMN_LOG_CHANGE + " < 0 AND " +
                DatabaseHelper.COLUMN_LOG_TIMESTAMP + " > ? GROUP BY " + DatabaseHelper.COLUMN_LOG_ITEM_ID;
        
        Cursor usageCursor = null;
        try {
            usageCursor = db.rawQuery(usageQuery, new String[]{String.valueOf(timeThreshold)});
            if (usageCursor.moveToFirst()) {
                do {
                    usageMap.put(usageCursor.getInt(0), Math.abs(usageCursor.getLong(1)));
                } while (usageCursor.moveToNext());
            }
        } finally {
            if (usageCursor != null) usageCursor.close();
        }

        for (InventoryItem item : items) {
            boolean recommendationAdded = false;

            // Check 1: Is item below required inventory?
            if (item.getQuantity() < item.getRequiredInventory()) {
                long deficit = item.getRequiredInventory() - item.getQuantity();
                recommendations.add(String.format(java.util.Locale.getDefault(),
                        "Item: %s\n   Reason: Below Required Level\n   Current: %d / Required: %d\n   Recommended Order: %d",
                        item.getName(), item.getQuantity(), item.getRequiredInventory(), deficit));
                recommendationAdded = true;
            }

            // Check 2: Predictive Analysis (only if not already flagged for low stock)
            if (!recommendationAdded) {
                long totalConsumed = usageMap.containsKey(item.getId()) ? usageMap.get(item.getId()) : 0;
                // Calculate rate per minute based on the 24-hour window
                double minuteRate = totalConsumed / (double) (24 * 60);

                // If we have usage, check if stock is critically low based on rate
                if (minuteRate > 0) {
                    double minutesOfStockLeft = (item.getQuantity() > 0) ? item.getQuantity() / minuteRate : 0;

                    // If less than 24 hours (1440 mins) of stock remaining, recommend ordering
                    if (minutesOfStockLeft < 1440.0) {
                        long suggestedOrder = (long) ((minuteRate * 2880) - item.getQuantity()); // Target 48 hours stock
                        if (suggestedOrder <= 0) suggestedOrder = (long) (minuteRate * 1440); // Ensure positive order

                        recommendations.add(String.format(java.util.Locale.getDefault(),
                                "Item: %s\n   Rate: %.2f /min\n   Stock: %d (%.1f hours left)\n   Recommended Order: %d",
                                item.getName(), minuteRate, item.getQuantity(), minutesOfStockLeft / 60.0, suggestedOrder));
                    }
                }
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("No immediate restocking needs detected.");
        }
        return recommendations;
    }

    /**
     * Retrieves usage history for the last 60 minutes per item.
     * @return A map of Item Name -> List of minute consumption.
     */
    public Map<String, List<Integer>> getPerItemUsageHistory() {
        // Delegate to the optimized generic method: 1 hour duration, 1 minute steps
        return getUsageHistory(60 * 60 * 1000L, 60 * 1000L);
    }

    /**
     * Retrieves usage history for a specified duration and step interval.
     * @param durationMillis The total duration to look back (e.g., 24 hours).
     * @param stepMillis The size of each data point bucket (e.g., 1 hour).
     * @return A map of Item Name -> List of usage counts per step.
     */
    public Map<String, List<Integer>> getUsageHistory(long durationMillis, long stepMillis) {
        Map<String, List<Integer>> result = new HashMap<>();
        List<InventoryItem> items = getAllItems();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long now = System.currentTimeMillis();
        long startTime = now - durationMillis;
        int numSteps = (int) (durationMillis / stepMillis);

        // Optimization: Map ID to Name for O(1) lookup during cursor iteration
        Map<Integer, String> idToNameMap = new HashMap<>();

        for (InventoryItem item : items) {
            List<Integer> points = new ArrayList<>(Collections.nCopies(numSteps, 0));
            idToNameMap.put(item.getId(), item.getName());
            result.put(item.getName(), points);
        }

        // Optimized query: Get all relevant logs in one go
        String query = "SELECT " + DatabaseHelper.COLUMN_LOG_ITEM_ID + ", " +
                DatabaseHelper.COLUMN_LOG_CHANGE + ", " +
                DatabaseHelper.COLUMN_LOG_TIMESTAMP +
                " FROM " + DatabaseHelper.TABLE_USAGE_LOG +
                " WHERE " + DatabaseHelper.COLUMN_LOG_TIMESTAMP + " >= ? AND " +
                DatabaseHelper.COLUMN_LOG_CHANGE + " < 0";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{String.valueOf(startTime)});
            if (cursor.moveToFirst()) {
                do {
                    int itemId = cursor.getInt(0);
                    long change = Math.abs(cursor.getLong(1));
                    long timestamp = cursor.getLong(2);

                    String itemName = idToNameMap.get(itemId);

                    if (itemName != null) {
                        long timeFromStart = timestamp - startTime;
                        int index = (int) (timeFromStart / stepMillis);
                        if (index >= 0 && index < numSteps) {
                            List<Integer> points = result.get(itemName);
                            if (points != null) {
                                points.set(index, points.get(index) + (int) change);
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching usage history", e);
        } finally {
            if (cursor != null) cursor.close();
        }

        return result;
    }

    /**
     * Generates predicted usage data based on recent consumption rates.
     */
    public Map<String, List<Integer>> getPredictedUsage(long durationMillis, long stepMillis) {
        Map<String, List<Integer>> result = new HashMap<>();
        List<InventoryItem> items = getAllItems();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int numSteps = (int) (durationMillis / stepMillis);
        
        // Calculate rate based on last 24 hours for better stability
        long historyDuration = 24 * 60 * 60 * 1000L;
        long timeThreshold = System.currentTimeMillis() - historyDuration;

        // Pre-fetch usage data
        Map<Integer, Long> usageMap = new HashMap<>();
        String usageQuery = "SELECT " + DatabaseHelper.COLUMN_LOG_ITEM_ID + ", SUM(" + DatabaseHelper.COLUMN_LOG_CHANGE + ") FROM " + DatabaseHelper.TABLE_USAGE_LOG +
                " WHERE " + DatabaseHelper.COLUMN_LOG_CHANGE + " < 0 AND " +
                DatabaseHelper.COLUMN_LOG_TIMESTAMP + " > ? GROUP BY " + DatabaseHelper.COLUMN_LOG_ITEM_ID;

        Cursor usageCursor = null;
        try {
            usageCursor = db.rawQuery(usageQuery, new String[]{String.valueOf(timeThreshold)});
            if (usageCursor.moveToFirst()) {
                do {
                    usageMap.put(usageCursor.getInt(0), Math.abs(usageCursor.getLong(1)));
                } while (usageCursor.moveToNext());
            }
        } finally {
            if (usageCursor != null) usageCursor.close();
        }

        for (InventoryItem item : items) {
            long totalConsumed = usageMap.containsKey(item.getId()) ? usageMap.get(item.getId()) : 0;
            double ratePerMillis = totalConsumed / (double) historyDuration;
            int predictedStepUsage = (int) Math.ceil(ratePerMillis * stepMillis);
            
            // Create a flat line prediction
            List<Integer> points = new ArrayList<>(Collections.nCopies(numSteps, predictedStepUsage));
            result.put(item.getName(), points);
        }
        return result;
    }
}