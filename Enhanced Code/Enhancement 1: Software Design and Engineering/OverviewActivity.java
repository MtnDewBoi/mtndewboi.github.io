package com.example.cs360finalproject;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;
import android.text.TextWatcher;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

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
    private InventoryViewModel inventoryViewModel;
    private final List<InventoryItem> itemList = new ArrayList<>();
    private Button addItemButton;
    private ImageButton settingsButton, ordersButton, filterLowStockButton;
    private TextView headerName, headerQuantity, headerRequired;
    private ImageButton searchIconButton;
    private String originalNameText, originalQuantityText, originalRequiredText;
    private EditText searchBar;

    /**
     * Called when the activity is starting.
     * Sets up the UI, ViewModel, and listeners.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this); // Apply theme before setContentView
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        initializeViews();
        setupViewModelAndAdapter();
        setupListeners();
        applyThemeColors();
    }

    /**
     * Initializes all UI components.
     */
    private void initializeViews() {
        inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView);
        addItemButton = findViewById(R.id.addItemButton);
        settingsButton = findViewById(R.id.editButton);
        ordersButton = findViewById(R.id.ordersButton);
        filterLowStockButton = findViewById(R.id.filterLowStockButton);
        searchIconButton = findViewById(R.id.searchIconButton);
        searchBar = findViewById(R.id.searchBar);

        // Find headers by ID directly
        headerName = findViewById(R.id.headerName);
        headerQuantity = findViewById(R.id.headerQuantity);
        headerRequired = findViewById(R.id.headerRequired);

        // Save original header text for resetting later
        if (headerName != null) originalNameText = headerName.getText().toString();
        if (headerQuantity != null) originalQuantityText = headerQuantity.getText().toString();
        if (headerRequired != null) originalRequiredText = headerRequired.getText().toString();
    }

    /**
     * Sets up the DatabaseHelper, InventoryAdapter, and RecyclerView.
     */
    private void setupViewModelAndAdapter() {
        inventoryViewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        inventoryAdapter = new InventoryAdapter(itemList, this, new InventoryAdapter.OnItemActionListener() {
            @Override
            public void onUpdateQuantity(InventoryItem item, long newQuantity) {
                if (inventoryViewModel.updateItemQuantity(item.getId(), newQuantity)) {
                    // The LiveData observer will update the list
                    if (newQuantity == 0) {
                        SharedPreferences prefs = getSharedPreferences(SMSPermissionsActivity.PREFS_NAME, MODE_PRIVATE);
                        if (prefs.getBoolean(SMSPermissionsActivity.KEY_SMS_PERMISSION, false)) {
                            Toast.makeText(OverviewActivity.this, "Item reached zero. SMS restock alert triggered.", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(OverviewActivity.this, "Failed to update quantity.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onEditItem(InventoryItem item) {
                final Intent intent = new Intent(OverviewActivity.this, AddItemActivity.class);
                intent.putExtra("ITEM_ID", item.getId());
                startActivity(intent);
            }
        });

        inventoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        inventoryRecyclerView.setAdapter(inventoryAdapter);
        
        // Observe changes in the inventory list
        inventoryViewModel.getItems().observe(this, items -> {
            itemList.clear();
            if (items != null) {
                itemList.addAll(items);
            }
            inventoryAdapter.notifyDataSetChanged();
        });
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

        // Navigate to the Orders activity
        if (ordersButton != null) {
            ordersButton.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(OverviewActivity.this, OrdersActivity.class));
                } catch (Exception e) {
                    Toast.makeText(OverviewActivity.this, "Error opening Orders: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        // Toggle Low Stock Filter
        if (filterLowStockButton != null) {
            filterLowStockButton.setOnClickListener(v -> {
                inventoryViewModel.setShowLowStockOnly(!inventoryViewModel.isShowLowStockOnly());
                // Re-apply colors to update the filter button state
                applyThemeColors();
            });
        }

        // Listener for the new search icon button
        if (searchIconButton != null) {
            searchIconButton.setOnClickListener(v -> {
                if (searchBar.getVisibility() == View.GONE) {
                    searchBar.setVisibility(View.VISIBLE);
                    searchBar.requestFocus();
                    // Show keyboard
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.showSoftInput(searchBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                } else {
                    searchBar.setText(""); // Clear search text
                    searchBar.setVisibility(View.GONE);
                    // Hide keyboard
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                }
            });
        }

        if (searchBar != null) {
            searchBar.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (inventoryViewModel != null) inventoryViewModel.performSearch(s.toString());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (headerName != null) {
            headerName.setOnClickListener(v -> {
                inventoryViewModel.sortItems(InventoryViewModel.SortCriteria.NAME);
                updateSortIndicators();
            });
        }
        if (headerQuantity != null) {
            headerQuantity.setOnClickListener(v -> {
                inventoryViewModel.sortItems(InventoryViewModel.SortCriteria.QUANTITY);
                updateSortIndicators();
            });
        }
        if (headerRequired != null) {
            headerRequired.setOnClickListener(v -> {
                inventoryViewModel.sortItems(InventoryViewModel.SortCriteria.REQUIRED);
                updateSortIndicators();
            });
        }
    }

    /**
     * Updates the header text to show arrows indicating sort direction.
     */
    private void updateSortIndicators() {
        InventoryViewModel.SortCriteria criteria = inventoryViewModel.getCurrentSortCriteria();
        InventoryViewModel.SortOrder order = inventoryViewModel.getCurrentSortOrder();

        // Reset all headers to original text
        if (headerName != null) headerName.setText(originalNameText);
        if (headerQuantity != null) headerQuantity.setText(originalQuantityText);
        if (headerRequired != null) headerRequired.setText(originalRequiredText);

        if (order == InventoryViewModel.SortOrder.NONE) return;

        String arrow = (order == InventoryViewModel.SortOrder.ASC) ? " ▲" : " ▼";
        TextView targetHeader = null;

        if (criteria == InventoryViewModel.SortCriteria.NAME) targetHeader = headerName;
        else if (criteria == InventoryViewModel.SortCriteria.QUANTITY) targetHeader = headerQuantity;
        else if (criteria == InventoryViewModel.SortCriteria.REQUIRED) targetHeader = headerRequired;

        if (targetHeader != null) targetHeader.setText(targetHeader.getText() + arrow);
    }

    /**
     * Applies the current theme colors to dynamic UI elements.
     */
    private void applyThemeColors() {
        int primaryColor = ThemeUtils.getPrimaryColor(this);
        int backgroundColor = ThemeUtils.getBackgroundColor(this);

        ViewGroup rootContent = findViewById(android.R.id.content);
        rootContent.setBackgroundColor(backgroundColor);

        if (rootContent.getChildCount() > 0) {
            View rootView = rootContent.getChildAt(0);
            rootView.setBackgroundColor(backgroundColor);
            // Recursively update text colors (this will fix the column headers)
            ThemeUtils.applyThemeToViews(rootView, this);

            // Apply theme to the search icon
            if (searchIconButton != null) {
                searchIconButton.setColorFilter(primaryColor);
            }

            // Apply theme to the orders icon
            if (ordersButton != null) {
                ordersButton.setColorFilter(primaryColor);
            }

            // Apply theme to the filter icon (Red if active, Primary if inactive)
            if (filterLowStockButton != null) {
                int filterColor = inventoryViewModel.isShowLowStockOnly() ? android.graphics.Color.RED : primaryColor;
                filterLowStockButton.setColorFilter(filterColor);
            }

            // Apply theme to the new search bar
            if (searchBar != null) {
                searchBar.setTextColor(ThemeUtils.getTextColor(this));
                searchBar.setHintTextColor(ThemeUtils.getHintColor(this));
                searchBar.setBackgroundColor(ThemeUtils.getTextBoxBackgroundColor(this));
            }

            // Fix for header background: Apply background color to the header container
            if (rootView instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) rootView;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    // If the child is a ViewGroup (likely the header row) and not the RecyclerView
                    if (child instanceof ViewGroup && child != inventoryRecyclerView) {
                        child.setBackgroundColor(backgroundColor);
                    }
                }
            }
        }

        addItemButton.setBackgroundColor(primaryColor);
        settingsButton.setColorFilter(primaryColor); // Tint the settings icon
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        inventoryViewModel.loadItems(); // Refresh inventory list every time the activity comes to the foreground
        checkSmsPermissionState(); // Check and prompt for SMS permission state
        applyThemeColors(); // Apply theme colors when returning from Settings
    }

    /**
     * Checks the state of SMS permissions and user preferences, then shows relevant dialogs.
     */
    private void checkSmsPermissionState() {
        final SharedPreferences sharedPreferences = getSharedPreferences(SMSPermissionsActivity.PREFS_NAME, MODE_PRIVATE);
        final boolean isSmsEnabled = sharedPreferences.getBoolean(SMSPermissionsActivity.KEY_SMS_PERMISSION, false);
        // Check if the application has the system-level SEND_SMS permission
        final boolean hasSystemPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;

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