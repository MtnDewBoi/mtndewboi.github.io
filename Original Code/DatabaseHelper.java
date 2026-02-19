package com.example.cs360finalproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for managing database creation and version management.
 * Handles all CRUD operations for the inventory and user tables.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "inventoryDB";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    private static final String TABLE_INVENTORY = "inventory";
    private static final String TABLE_USERS = "users";

    // Constants for inventory table columns
    public static final String COLUMN_ITEM_ID = "id";
    public static final String COLUMN_ITEM_NAME = "name";
    public static final String COLUMN_ITEM_QUANTITY = "quantity";
    public static final String COLUMN_ITEM_REQUIREDINVENTORY = "requiredInventory";

    // Constants for users table columns
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_USER_NAME = "username";
    private static final String COLUMN_USER_PASSWORD = "password";

    private static final String TAG = "DatabaseHelper";

    private final Context context;

    /**
     * Constructor for DatabaseHelper.
     * @param context The application context.
     */
    public DatabaseHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    /**
     * Called when the database is created for the first time.
     * @param db The database.
     */
    @Override
    public void onCreate(final SQLiteDatabase db) {
        // SQL statement for creating the Inventory table
        final String CREATE_INVENTORY_TABLE = "CREATE TABLE " + TABLE_INVENTORY + "(" +
                COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ITEM_NAME + " TEXT NOT NULL, " +
                COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL, " +
                COLUMN_ITEM_REQUIREDINVENTORY + " INTEGER NOT NULL" + ")";

        // SQL statement for creating the Users table (for login/registration)
        final String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "(" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USER_NAME + " TEXT NOT NULL UNIQUE, " + // UNIQUE constraint on username
                COLUMN_USER_PASSWORD + " TEXT NOT NULL" + ")";

        db.execSQL(CREATE_INVENTORY_TABLE);
        db.execSQL(CREATE_USERS_TABLE);
    }

    /**
     * Called when the database needs to be upgraded.
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        // Drop older tables if they exist and create new ones
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    /**
     * Deletes all data and recreates the tables (for testing/reset).
     */
    public void clearDatabase() {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    /**
     * Adds a new item to the inventory table.
     * @param name The name of the item.
     * @param quantity The initial quantity.
     * @param requiredInventory The restock threshold.
     * @return True if the item was added successfully, false otherwise.
     */
    public boolean addItem(final String name, final int quantity, final int requiredInventory) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_NAME, name);
        values.put(COLUMN_ITEM_QUANTITY, quantity);
        values.put(COLUMN_ITEM_REQUIREDINVENTORY, requiredInventory);

        // Inserting Row
        final long result = db.insert(TABLE_INVENTORY, null, values);
        // db.close(); // SQLiteOpenHelper manages closing, but generally good practice
        return result != -1; // -1 indicates an error
    }

    /**
     * Updates the quantity of an existing item.
     * @param itemId The ID of the item to update.
     * @param newQuantity The new quantity value.
     * @return True if the quantity was updated successfully, false otherwise.
     */
    public boolean updateItemQuantity(final int itemId, final int newQuantity) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_QUANTITY, newQuantity);

        // Update row, specifying the ID to target
        final int rowsAffected = db.update(TABLE_INVENTORY, values, COLUMN_ITEM_ID + "=?", new String[]{String.valueOf(itemId)});

        // Check for restock condition (quantity reached zero) and notify via SMS
        if (rowsAffected > 0 && newQuantity == 0) {
            notifyZeroQuantity(itemId);
        }
        return rowsAffected > 0;
    }

    /**
     * Deletes an item from the inventory table.
     * @param itemId The ID of the item to delete.
     * @return True if the item was deleted successfully, false otherwise.
     */
    public boolean deleteItem(final int itemId) {
        final SQLiteDatabase db = this.getWritableDatabase();
        // Delete row, specifying the ID to target
        final int rows = db.delete(TABLE_INVENTORY, COLUMN_ITEM_ID + " = ?", new String[]{String.valueOf(itemId)});
        return rows > 0;
    }

    /**
     * Fetches all items from the inventory table. Ensures the Cursor is always closed.
     * @return A list of InventoryItem objects.
     */
    public List<InventoryItem> getAllItems() {
        final List<InventoryItem> itemList = new ArrayList<>();
        final SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            // Query all rows from the inventory table
            cursor = db.query(TABLE_INVENTORY, null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Read data from the cursor and create a new InventoryItem
                    final int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_ID));
                    final String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_NAME));
                    final int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_QUANTITY));
                    final int requiredInventory = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_REQUIREDINVENTORY));

                    itemList.add(new InventoryItem(id, name, quantity, requiredInventory));
                } while (cursor.moveToNext());
            }
        } catch (final Exception e) {
            Log.e(TAG, "Error while getting all items: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close(); // Critical: Ensure cursor is closed
            }
        }
        return itemList;
    }

    /**
     * Fetches a single item by its name. Ensures the Cursor is always closed.
     * @param name The name of the item to retrieve.
     * @return The InventoryItem object or null if not found.
     */
    public InventoryItem getItemByName(final String name) {
        final SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        InventoryItem item = null;
        try {
            final String selection = COLUMN_ITEM_NAME + " = ?";
            final String[] selectionArgs = {name};

            // Query the table for a specific item name
            cursor = db.query(TABLE_INVENTORY, null, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                // Item found, extract data
                final int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_ID));
                final String itemName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_NAME));
                final int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_QUANTITY));
                final int requiredInventory = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_REQUIREDINVENTORY));

                item = new InventoryItem(id, itemName, quantity, requiredInventory);
            }
        } catch (final Exception e) {
            Log.e(TAG, "Error while getting item by name: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return item;
    }

    /**
     * Checks if SMS notifications are enabled and sends an alert for zero quantity.
     * This logic is triggered from updateItemQuantity when the new quantity is 0.
     * @param itemId The ID of the item that reached zero quantity.
     */
    private void notifyZeroQuantity(final int itemId) {
        // Retrieve SMS preferences from SharedPreferences
        final SharedPreferences sharedPreferences = context.getSharedPreferences(SMSPermissionsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        final boolean smsEnabled = sharedPreferences.getBoolean(SMSPermissionsActivity.KEY_SMS_PERMISSION, false);
        final String phoneNumber = sharedPreferences.getString(SMSPermissionsActivity.KEY_PHONE_NUMBER, "");

        if (smsEnabled && !phoneNumber.isEmpty()) {
            final String itemName = String.valueOf(getItemById(itemId)); // Get the item name for the message
            if (itemName != null) {
                // Use the SMSManager utility to send the message
                SMSManager.sendSMS(context, phoneNumber, "Inventory Alert: Item '" + itemName + "' has reached zero quantity.");
            }
        }
    }

    /**
     * Retrieves a single InventoryItem by its unique ID.
     * @param id The ID of the item to retrieve.
     * @return The InventoryItem object, or null if not found.
     */
    public InventoryItem getItemById(final int id) {
        final SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        InventoryItem item = null;

        try {
            cursor = db.query(
                    TABLE_INVENTORY,
                    new String[]{COLUMN_ITEM_ID, COLUMN_ITEM_NAME, COLUMN_ITEM_QUANTITY, COLUMN_ITEM_REQUIREDINVENTORY},
                    COLUMN_ITEM_ID + "=?",
                    new String[]{String.valueOf(id)},
                    null, null, null, null
            ); //

            if (cursor != null && cursor.moveToFirst()) {
                final int itemId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_ID));
                final String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_NAME));
                final int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_QUANTITY));
                final int required = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_REQUIREDINVENTORY));

                item = new InventoryItem(itemId, name, quantity, required); //
            }
        } catch (final Exception e) {
            Log.e(TAG, "Error while getting item by ID: " + e.getMessage(), e); //
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return item;
    }

    /**
     * Updates an existing inventory item's name, current quantity, and required restock level.
     * @param id The ID of the item to update.
     * @param name The new name of the item.
     * @param quantity The new quantity of the item.
     * @param requiredInventory The new required restock level.
     * @return true if update was successful, false otherwise.
     */
    public boolean updateItemDetails(final int id, final String name, final int quantity, final int requiredInventory) {
        final SQLiteDatabase db = this.getWritableDatabase(); //
        final ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_NAME, name);
        values.put(COLUMN_ITEM_QUANTITY, quantity);
        values.put(COLUMN_ITEM_REQUIREDINVENTORY, requiredInventory);

        final int rowsAffected = db.update(TABLE_INVENTORY, values, COLUMN_ITEM_ID + "=?", new String[]{String.valueOf(id)});
        return rowsAffected > 0;
    }

    /* *******************************************************************
     * User Management Methods
     * *******************************************************************/

    /**
     * Registers a new user with a username and password.
     * @param username The user's chosen username.
     * @param password The user's chosen password.
     * @return True if registration was successful, false if the username already exists or a database error occurred.
     */
    public boolean registerUser(final String username, final String password) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, username);
        values.put(COLUMN_USER_PASSWORD, password); // Note: For a real app, passwords should be hashed!

        // Inserting Row. Fails if username is not unique (due to UNIQUE constraint)
        final long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    /**
     * Authenticates a user by checking their username and password against the database.
     * @param username The username to check.
     * @param password The password to check.
     * @return True if a matching user is found, false otherwise.
     */
    public boolean authenticateUser(final String username, final String password) {
        final SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean isAuthenticated = false;
        try {
            // Query to find a user with both matching username AND password
            final String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_USER_NAME + " = ? AND " + COLUMN_USER_PASSWORD + " = ?";
            cursor = db.rawQuery(query, new String[]{username, password});
            isAuthenticated = cursor.moveToFirst(); // True if a matching row is found
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return isAuthenticated;
    }
}