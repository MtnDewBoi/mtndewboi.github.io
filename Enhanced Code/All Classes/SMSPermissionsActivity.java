package com.example.cs360finalproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;

/**
 * Activity for managing SMS permissions and preferences (phone number, enable/disable toggle).
 */
public class SMSPermissionsActivity extends AppCompatActivity {

    private static final int REQUEST_SMS_PERMISSION = 123;

    // Public constants for SharedPreferences file and keys - Centralized for project use
    public static final String PREFS_NAME = "SMSPrefs";
    public static final String KEY_PHONE_NUMBER = "phone_number";
    public static final String KEY_SMS_PERMISSION = "sms_permission";

    // UI elements
    private EditText phoneNumberEditText;
    private ToggleButton smsPermissionToggle;
    private TextView permissionStatusTextView;
    private Button returnButton;
    private Button logoutButton;
    private Button requestPermissionButton;
    private Button themeButton;

    private SMSPermissionsViewModel viewModel;

    /**
     * Called when the activity is first created.
     * Initializes UI, ViewModel, and loads preferences.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this); // Apply theme before setContentView
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        initializeViews();
        setupViewModel();
        loadPreferences();
        checkSmsPermissionAndSetUI(); // Initial check and UI setup
        setupListeners();
        applyThemeColors();
    }

    /**
     * Links member variables to their corresponding UI components.
     */
    private void initializeViews() {
        phoneNumberEditText = findViewById(R.id.editTextPhoneNumber);
        smsPermissionToggle = findViewById(R.id.smsPermissionToggle);
        permissionStatusTextView = findViewById(R.id.textViewPermissionStatus);
        returnButton = findViewById(R.id.returnButton);
        logoutButton = findViewById(R.id.logoutButton);
        requestPermissionButton = findViewById(R.id.requestPermissionButton);
        themeButton = findViewById(R.id.themeButton);
    }

    /**
     * Initializes the ViewModel.
     */
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(SMSPermissionsViewModel.class);
    }

    /**
     * Sets up click listeners for all interactive UI elements.
     */
    private void setupListeners() {
        smsPermissionToggle.setOnClickListener(v -> handleSmsToggle());
        requestPermissionButton.setOnClickListener(v -> requestSmsPermission());
        returnButton.setOnClickListener(v -> finish()); // Close this activity
        logoutButton.setOnClickListener(v -> handleLogout());
        themeButton.setOnClickListener(v -> showThemeSelectionDialog());
    }

    /**
     * Shows a dialog to select the UI theme.
     */
    private void showThemeSelectionDialog() {
        final String[] themes = {"Gray", "Blue", "Green"};
        final int[] themeValues = {ThemeUtils.THEME_GRAY, ThemeUtils.THEME_BLUE, ThemeUtils.THEME_GREEN};
        int currentTheme = ThemeUtils.getSelectedTheme(this);

        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
                    ThemeUtils.saveTheme(this, themeValues[which]);
                    dialog.dismiss();
                    recreate(); // Restart activity to apply theme
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Ensure UI reflects current permission state
        checkSmsPermissionAndSetUI();
        // Re-apply colors in case of system theme change
        applyThemeColors();
    }

    /**
     * Applies the current theme colors to the UI elements.
     */
    private void applyThemeColors() {
        int primaryColor = ThemeUtils.getPrimaryColor(this);
        int backgroundColor = ThemeUtils.getBackgroundColor(this);

        // Apply background to the root container
        ViewGroup rootContent = findViewById(android.R.id.content);
        rootContent.setBackgroundColor(backgroundColor);
        
        // Also apply to the inflated layout root to ensure it covers any XML defaults
        if (rootContent.getChildCount() > 0) {
            View rootView = rootContent.getChildAt(0);
            rootView.setBackgroundColor(backgroundColor);
            // Update all text views (labels) to be visible
            ThemeUtils.applyThemeToViews(rootView, this);
        }
        
        returnButton.setBackgroundColor(primaryColor);
        logoutButton.setBackgroundColor(primaryColor);
        requestPermissionButton.setBackgroundColor(primaryColor);
        themeButton.setBackgroundColor(primaryColor);
    }

    /**
     * Checks the system SMS permission status and adjusts the UI elements accordingly.
     */
    private void checkSmsPermissionAndSetUI() {
        // Check for the system-level SEND_SMS permission
        final boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        // Check for the user's saved preference (the state of the toggle)
        final boolean isToggleOn = viewModel.isSmsPermissionEnabled();

        updatePermissionStatus(hasPermission);

        if (hasPermission) {
            // If system permission is granted, hide the request button and enable the toggle
            requestPermissionButton.setVisibility(View.GONE);
            smsPermissionToggle.setEnabled(true);
            smsPermissionToggle.setChecked(isToggleOn); // Set toggle based on user preference
            updateToggleVisuals(isToggleOn);
        } else {
            // If system permission is denied, show the request button and disable the toggle
            requestPermissionButton.setVisibility(View.VISIBLE);
            smsPermissionToggle.setEnabled(false);
            smsPermissionToggle.setChecked(false);
            // Crucial: If system permission is denied, ensure user preference is also disabled
            savePreferences(false);
            updateToggleVisuals(false);
        }
    }

    /**
     * Handles the click event for the SMS permission toggle button.
     */
    private void handleSmsToggle() {
        final boolean isChecked = smsPermissionToggle.isChecked();

        // 1. If enabling, check for valid phone number
        if (isChecked && !isValidPhoneNumber()) {
            Toast.makeText(this, "Please enter a valid phone number before enabling SMS alerts.", Toast.LENGTH_LONG).show();
            smsPermissionToggle.setChecked(false);
            updateToggleVisuals(false);
            return;
        }

        // 2. If enabling, ensure system permission is granted
        if (isChecked && ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS Permission is required. Requesting now.", Toast.LENGTH_SHORT).show();
            smsPermissionToggle.setChecked(false);
            updateToggleVisuals(false);
            requestSmsPermission();
            return;
        }

        // 3. Save the new preference state
        savePreferences(isChecked);
        updateToggleVisuals(isChecked);
        Toast.makeText(this, "SMS Alerts " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
    }

    /**
     * Updates the visual state of the toggle button (background color) to reflect enabled/disabled status.
     */
    private void updateToggleVisuals(boolean isChecked) {
        int colorId;
        if (smsPermissionToggle.isEnabled()) {
            colorId = isChecked ? android.R.color.holo_green_dark : android.R.color.holo_red_dark;
        } else {
            colorId = android.R.color.darker_gray;
        }
        smsPermissionToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, colorId)));
    }

    /**
     * Initiates the system request for the SEND_SMS permission.
     */
    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
    }

    /**
     * Callback for the result of the permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS Permission Granted! Please enable notifications using the toggle.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "SMS Permission Denied. Restock alerts will be disabled.", Toast.LENGTH_LONG).show();
            }
            // Re-run the UI setup to reflect the new system permission state
            checkSmsPermissionAndSetUI();
        }
    }

    /**
     * Handles the user logout process.
     */
    private void handleLogout() {
        // Clear all SharedPreferences data (including SMS settings and any prompt counts)
        viewModel.clearPreferences();

        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();

        // Navigate to MainActivity (Login screen) and clear the activity stack
        final Intent intent = new Intent(SMSPermissionsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Loads the saved phone number and SMS permission preference from SharedPreferences.
     */
    private void loadPreferences() {
        final String phoneNumber = viewModel.getPhoneNumber();
        // Default to false if no preference is saved
        final boolean smsPermission = viewModel.isSmsPermissionEnabled();

        phoneNumberEditText.setText(phoneNumber);
        smsPermissionToggle.setChecked(smsPermission);
    }

    /**
     * Saves the current phone number and SMS permission state to SharedPreferences.
     * @param smsPermission The current state of the SMS toggle (true/false).
     */
    private void savePreferences(final boolean smsPermission) {
        viewModel.savePreferences(phoneNumberEditText.getText().toString().trim(), smsPermission);
    }

    /**
     * Updates the text and color of the permission status TextView.
     * @param isGranted True if the system permission is granted, false otherwise.
     */
    private void updatePermissionStatus(final boolean isGranted) {
        permissionStatusTextView.setText(isGranted ? "Status: Permission Granted" : "Status: Permission Denied");
        // Set text color to green for granted, red for denied
        // Use light variants for better visibility in Dark Mode, or stick to dark if in Light Mode
        boolean isDark = ThemeUtils.isDarkMode(this);
        int green = ContextCompat.getColor(this, isDark ? android.R.color.holo_green_light : android.R.color.holo_green_dark);
        int red = ContextCompat.getColor(this, isDark ? android.R.color.holo_red_light : android.R.color.holo_red_dark);
        permissionStatusTextView.setTextColor(isGranted ? green : red);
    }

    /**
     * Validates if the entered phone number is not empty and matches a phone number pattern.
     * @return True if the phone number is valid, false otherwise.
     */
    private boolean isValidPhoneNumber() {
        final String phoneNumber = phoneNumberEditText.getText().toString().trim();
        return SMSHelper.isValidPhoneNumber(phoneNumber);
    }
}