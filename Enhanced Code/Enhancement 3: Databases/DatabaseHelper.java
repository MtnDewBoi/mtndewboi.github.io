package com.example.cs360finalproject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class for managing database creation and version management.
 * Handles all CRUD operations for the inventory and user tables.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "inventoryDB";
    private static final int DATABASE_VERSION = 2;

    // Table Names
    private static final String TABLE_INVENTORY = "inventory";
    private static final String TABLE_USERS = "users";
    public static final String TABLE_USAGE_LOG = "usage_log";

    // Constants for inventory table columns
    public static final String COLUMN_ITEM_ID = "id";
    public static final String COLUMN_ITEM_NAME = "name";
    public static final String COLUMN_ITEM_QUANTITY = "quantity";
    public static final String COLUMN_ITEM_REQUIREDINVENTORY = "requiredInventory";

    // Constants for users table columns
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USER_NAME = "username";
    public static final String COLUMN_USER_PASSWORD = "password";

    // Constants for usage log table columns
    public static final String COLUMN_LOG_ID = "log_id";
    public static final String COLUMN_LOG_ITEM_ID = "item_id";
    public static final String COLUMN_LOG_CHANGE = "change_amount";
    public static final String COLUMN_LOG_TIMESTAMP = "timestamp";

    private static final String TAG = "DatabaseHelper";

    // SQL statement for creating the Inventory table
    private static final String CREATE_INVENTORY_TABLE = "CREATE TABLE " + TABLE_INVENTORY + "(" +
            COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_ITEM_NAME + " TEXT NOT NULL, " +
            COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL, " +
            COLUMN_ITEM_REQUIREDINVENTORY + " INTEGER NOT NULL" + ")";

    // SQL statement for creating the Users table (for login/registration)
    private static final String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "(" +
            COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USER_NAME + " TEXT NOT NULL UNIQUE, " + // UNIQUE constraint on username
            COLUMN_USER_PASSWORD + " TEXT NOT NULL" + ")";

    // SQL statement for creating the Usage Log table
    private static final String CREATE_USAGE_LOG_TABLE = "CREATE TABLE " + TABLE_USAGE_LOG + "(" +
            COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_LOG_ITEM_ID + " INTEGER NOT NULL, " +
            COLUMN_LOG_CHANGE + " INTEGER NOT NULL, " +
            COLUMN_LOG_TIMESTAMP + " INTEGER NOT NULL" + ")";

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
        db.execSQL(CREATE_INVENTORY_TABLE);
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_USAGE_LOG_TABLE);
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
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USAGE_LOG);
        onCreate(db);
    }

    /**
     * Deletes all data and recreates the tables (for testing/reset).
     */
    public void clearDatabase() {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USAGE_LOG);
        onCreate(db);
    }
}