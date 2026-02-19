package com.example.cs360finalproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Repository class for handling User-related database operations.
 */
public class UserRepository {

    private final DatabaseHelper dbHelper;
    private static final String TABLE_USERS = "users";

    public UserRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    /**
     * Registers a new user with a username and password.
     * @param username The user's chosen username.
     * @param password The user's chosen password.
     * @return True if registration was successful, false if the username already exists or a database error occurred.
     */
    public boolean registerUser(final String username, final String password) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_NAME, username);
        
        String salt = generateSalt();
        values.put(DatabaseHelper.COLUMN_USER_PASSWORD, salt + ":" + hashPassword(password, salt));

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
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        boolean isAuthenticated = false;
        try {
            final String query = "SELECT " + DatabaseHelper.COLUMN_USER_PASSWORD + " FROM " + TABLE_USERS + " WHERE " + DatabaseHelper.COLUMN_USER_NAME + " = ?";
            cursor = db.rawQuery(query, new String[]{username});
            
            if (cursor.moveToFirst()) {
                String storedValue = cursor.getString(0);
                String[] parts = storedValue.split(":");
                
                if (parts.length == 2) {
                    String salt = parts[0];
                    String storedHash = parts[1];
                    isAuthenticated = storedHash.equals(hashPassword(password, salt));
                } else {
                    // Legacy support for unsalted passwords
                    isAuthenticated = storedValue.equals(hashPassword(password, null));
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return isAuthenticated;
    }

    /**
     * Hashes a password using SHA-256 with an optional salt.
     * @param password The plain text password.
     * @param salt The salt string (hex), can be null.
     * @return The hashed password string (hex), or null if hashing fails.
     */
    private String hashPassword(final String password, final String salt) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (salt != null) {
                digest.update(salt.getBytes());
            }
            final byte[] encodedhash = digest.digest(password.getBytes());
            final StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        StringBuilder hexString = new StringBuilder();
        for (byte b : salt) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}