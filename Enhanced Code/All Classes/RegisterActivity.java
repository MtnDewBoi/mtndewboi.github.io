package com.example.cs360finalproject;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for new user registration. Handles input, validation, and database storage.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private RegisterViewModel registerViewModel;

    /**
     * Called when the activity is first created.
     * Initializes UI elements and sets up listeners.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        // Apply theme before setContentView
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createaccount);

        registerViewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        // Initialize UI elements
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        final Button registerButton = findViewById(R.id.createAccountButton);
        final Button goBackButton = findViewById(R.id.goBackButton);

        // Fix for tablet layout: restrict width of input fields and buttons
        if (getResources().getConfiguration().screenWidthDp >= 600) {
            int widthPixels = (int) (400 * getResources().getDisplayMetrics().density);
            adjustViewWidth(usernameEditText, widthPixels);
            adjustViewWidth(passwordEditText, widthPixels);
            adjustViewWidth(registerButton, widthPixels);
            adjustViewWidth(goBackButton, widthPixels);
        }

        // Set up listeners
        registerButton.setOnClickListener(v -> registerUser());
        goBackButton.setOnClickListener(v -> finish());
        
        // Apply Theme
        int textColor = ThemeUtils.getTextColor(this);
        int hintColor = ThemeUtils.getHintColor(this);
        int textBoxColor = ThemeUtils.getTextBoxBackgroundColor(this);
        findViewById(android.R.id.content).setBackgroundColor(ThemeUtils.getBackgroundColor(this));
        registerButton.setBackgroundColor(ThemeUtils.getPrimaryColor(this));
        goBackButton.setBackgroundColor(ThemeUtils.getPrimaryColor(this));
        usernameEditText.setTextColor(textColor);
        usernameEditText.setHintTextColor(hintColor);
        usernameEditText.setBackgroundColor(textBoxColor);
        passwordEditText.setTextColor(textColor);
        passwordEditText.setHintTextColor(hintColor);
        passwordEditText.setBackgroundColor(textBoxColor);
    }

    /**
     * Handles the user registration process.
     */
    private void registerUser() {
        final String username = usernameEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();

        // Basic input validation
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Attempt to register the user in the database
        final boolean isSuccess = registerViewModel.registerUser(username, password);

        if (isSuccess) {
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
            // Close this activity and return to the login screen
            finish();
        } else {
            // This usually means the username already exists due to the UNIQUE constraint in the DB
            Toast.makeText(this, "Registration failed. Username may already be taken.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Helper method to adjust view width and center it for tablets.
     */
    private void adjustViewWidth(View view, int width) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.width = width;
            if (params instanceof android.widget.LinearLayout.LayoutParams) {
                ((android.widget.LinearLayout.LayoutParams) params).gravity = android.view.Gravity.CENTER_HORIZONTAL;
            }
            view.setLayoutParams(params);
        }
    }
}