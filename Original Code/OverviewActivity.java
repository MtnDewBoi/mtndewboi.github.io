package com.example.cs360finalproject;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * The main activity displaying the inventory list using a RecyclerView.
 * It also handles SMS permission checking and prompting the user.
 */
public class OverviewActivity extends AppCompatActivity {

    private static final int REQUEST_SMS_PERMISSION = 123;
    private static final int MAX_PROMPT_COUNT = 3;
    private static final String KEY_PROMPT_COUNT = "sms_prompt_count"; // Key to track how many times the user has been prompted

    private RecyclerView inventoryRecyclerView;
    private InventoryAdapter inventoryAdapter;
    private DatabaseHelper databaseHelper;
    private final List<InventoryItem> itemList = new ArrayList<>();
    private Button addItemButton;
    private ImageButton settingsButton;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        initializeViews();
        setupDatabaseAndAdapter();
        setupListeners();
    }

    /**
     * Initializes all UI components.
     */
    private void initializeViews() {
        inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView);
        addItemButton = findViewById(R.id.addItemButton);
        settingsButton = findViewById(R.id.editButton); // Assuming 'editButton' is the settings/preferences button
    }

    /**
     * Sets up the DatabaseHelper, InventoryAdapter, and RecyclerView.
     */
    private void setupDatabaseAndAdapter() {
        databaseHelper = new DatabaseHelper(this);
        // Pass the list, context, and database helper to the adapter
        inventoryAdapter = new InventoryAdapter(itemList, this, databaseHelper);
        inventoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        inventoryRecyclerView.setAdapter(inventoryAdapter);
    }

    /**
     * Sets up click listeners for the Add Item and Settings buttons.
     */
    private void setupListeners() {
        // Navigate to the activity for adding a new item
        addItemButton.setOnClickListener(v -> {
            final Intent intent = new Intent(OverviewActivity.this, AddItemActivity.class);
            startActivity(intent);
        });

        // Navigate to the SMS permissions/settings activity
        settingsButton.setOnClickListener(v -> {
            final Intent intent = new Intent(OverviewActivity.this, SMSPermissionsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems(); // Refresh inventory list every time the activity comes to the foreground
        checkSmsPermissionState(); // Check and prompt for SMS permission state
    }

    /**
     * Checks the state of SMS permissions and user preferences, then shows relevant dialogs.
     */
    private void checkSmsPermissionState() {
        final SharedPreferences sharedPreferences = getSharedPreferences(SMSPermissionsActivity.PREFS_NAME, MODE_PRIVATE);
        final boolean isSmsEnabled = sharedPreferences.getBoolean(SMSPermissionsActivity.KEY_SMS_PERMISSION, false);
        // Check if the application has the system-level SEND_SMS permission
        final boolean hasSystemPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;

        // Update the adapter with the current SMS preference state (used for logic in the adapter)
        inventoryAdapter.setSmsEnabled(isSmsEnabled);

        if (isSmsEnabled && !hasSystemPermission) {
            // Case 1: User has enabled SMS in settings but system permission is revoked
            showMissingSystemPermissionDialog(sharedPreferences);
        } else if (!isSmsEnabled) {
            // Case 2: User has NOT enabled SMS alerts in settings (initial setup or manually disabled)

            // Get current prompt count to limit nagging
            final int promptCount = sharedPreferences.getInt(KEY_PROMPT_COUNT, 0);

            if (promptCount < MAX_PROMPT_COUNT) {
                // Show the feature disabled prompt and increment the count
                showFeatureDisabledDialog(sharedPreferences);
                sharedPreferences.edit().putInt(KEY_PROMPT_COUNT, promptCount + 1).apply();
            }
            // If promptCount >= MAX_PROMPT_COUNT, the dialog is skipped.
        }
    }

    /**
     * Loads all inventory items from the database and updates the RecyclerView.
     */
    private void loadItems() {
        itemList.clear();
        itemList.addAll(databaseHelper.getAllItems()); // Fetch all items
        inventoryAdapter.notifyDataSetChanged(); // Notify adapter to redraw the list
    }

    /**
     * Shows a dialog when user preference is ON but system permission is OFF.
     * @param sharedPreferences The SharedPreferences object for storing the prompt count.
     */
    private void showMissingSystemPermissionDialog(final SharedPreferences sharedPreferences) {
        new AlertDialog.Builder(this)
                .setTitle("SMS Permission Revoked")
                .setMessage("SMS notifications are enabled in your settings, but the required SEND_SMS permission is currently denied. Please grant the permission.")
                .setPositiveButton("Grant Permission", (dialog, which) -> requestSmsPermission())
                .setNegativeButton("Go to Settings", (dialog, which) -> {
                    // Navigate to the dedicated SMS settings screen
                    final Intent intent = new Intent(OverviewActivity.this, SMSPermissionsActivity.class);
                    startActivity(intent);
                })
                .show();
    }

    /**
     * Shows a dialog prompting the user to enable SMS notifications if they are currently disabled.
     * The prompt is limited by MAX_PROMPT_COUNT.
     */
    private void showFeatureDisabledDialog(final SharedPreferences sharedPreferences) {
        new AlertDialog.Builder(this)
                .setTitle("SMS Notifications Disabled")
                .setMessage("SMS restock alerts are currently disabled in settings. Would you like to enable them?")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    // Navigate to the dedicated SMS settings screen
                    final Intent intent = new Intent(OverviewActivity.this, SMSPermissionsActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Requests the system-level SEND_SMS permission from the user.
     */
    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
    }

    /**
     * Callback for the result of requesting permissions.
     */
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS Permission Granted! Now you can enable alerts in Settings.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "SMS Permission Denied. Restock alerts will remain disabled.", Toast.LENGTH_LONG).show();
            }
            checkSmsPermissionState(); // Re-evaluate the state (especially important for UI updates)
        }
    }
}