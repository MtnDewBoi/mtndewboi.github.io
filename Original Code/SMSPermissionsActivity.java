package com.example.cs360finalproject;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        initializeViews();
        setupSharedPreferences();
        loadPreferences();
        checkSmsPermissionAndSetUI(); // Initial check and UI setup
        setupListeners();
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
    }

    /**
     * Initializes the SharedPreferences object.
     */
    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    /**
     * Sets up click listeners for all interactive UI elements.
     */
    private void setupListeners() {
        smsPermissionToggle.setOnClickListener(v -> handleSmsToggle());
        requestPermissionButton.setOnClickListener(v -> requestSmsPermission());
        returnButton.setOnClickListener(v -> finish()); // Close this activity
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    /**
     * Checks the system SMS permission status and adjusts the UI elements accordingly.
     */
    private void checkSmsPermissionAndSetUI() {
        // Check for the system-level SEND_SMS permission
        final boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        // Check for the user's saved preference (the state of the toggle)
        final boolean isToggleOn = sharedPreferences.getBoolean(KEY_SMS_PERMISSION, false);

        updatePermissionStatus(hasPermission);

        if (hasPermission) {
            // If system permission is granted, hide the request button and enable the toggle
            requestPermissionButton.setVisibility(View.GONE);
            smsPermissionToggle.setEnabled(true);
            smsPermissionToggle.setChecked(isToggleOn); // Set toggle based on user preference
        } else {
            // If system permission is denied, show the request button and disable the toggle
            requestPermissionButton.setVisibility(View.VISIBLE);
            smsPermissionToggle.setEnabled(false);
            smsPermissionToggle.setChecked(false);
            // Crucial: If system permission is denied, ensure user preference is also disabled
            savePreferences(false);
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
            smsPermissionToggle.setChecked(false); // Revert the toggle state
            return;
        }

        // 2. If enabling, ensure system permission is granted
        if (isChecked && ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS Permission is required. Requesting now.", Toast.LENGTH_SHORT).show();
            smsPermissionToggle.setChecked(false); // Revert the toggle state
            requestSmsPermission(); // Request the system permission
            return;
        }

        // 3. Save the new preference state
        savePreferences(isChecked);
        Toast.makeText(this, "SMS Alerts " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
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
        sharedPreferences.edit().clear().apply();

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
        final String phoneNumber = sharedPreferences.getString(KEY_PHONE_NUMBER, "");
        // Default to false if no preference is saved
        final boolean smsPermission = sharedPreferences.getBoolean(KEY_SMS_PERMISSION, false);

        phoneNumberEditText.setText(phoneNumber);
        smsPermissionToggle.setChecked(smsPermission);
    }

    /**
     * Saves the current phone number and SMS permission state to SharedPreferences.
     * @param smsPermission The current state of the SMS toggle (true/false).
     */
    private void savePreferences(final boolean smsPermission) {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        // Save the trimmed phone number text
        editor.putString(KEY_PHONE_NUMBER, phoneNumberEditText.getText().toString().trim());
        // Save the boolean state of the SMS permission
        editor.putBoolean(KEY_SMS_PERMISSION, smsPermission);
        editor.apply(); // Use apply() for asynchronous write
    }

    /**
     * Updates the text and color of the permission status TextView.
     * @param isGranted True if the system permission is granted, false otherwise.
     */
    private void updatePermissionStatus(final boolean isGranted) {
        permissionStatusTextView.setText(isGranted ? "Status: Permission Granted" : "Status: Permission Denied");
        // Set text color to green for granted, red for denied
        permissionStatusTextView.setTextColor(ContextCompat.getColor(this, isGranted ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
    }

    /**
     * Validates if the entered phone number is not empty and matches a phone number pattern.
     * @return True if the phone number is valid, false otherwise.
     */
    private boolean isValidPhoneNumber() {
        final String phoneNumber = phoneNumberEditText.getText().toString().trim();
        return !TextUtils.isEmpty(phoneNumber) && android.util.Patterns.PHONE.matcher(phoneNumber).matches();
    }
}