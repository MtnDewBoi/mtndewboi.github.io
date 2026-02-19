package com.example.cs360finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AppCompatActivity;

/**
 * The main login activity for the application. Handles user authentication
 * and navigation to the inventory overview or registration screen.
 */
public class MainActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private EditText usernameEditText;
    private EditText passwordEditText;

    /**
     * Called when the activity is first created.
     * Initializes UI elements and sets up listeners for login and registration.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this); // Apply theme before setContentView
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Initialize UI elements
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        final Button loginButton = findViewById(R.id.loginButton);
        final Button createAccountButton = findViewById(R.id.createAccountButton);

        // Fix for tablet layout: restrict width of input fields and buttons
        if (getResources().getConfiguration().screenWidthDp >= 600) {
            int widthPixels = (int) (400 * getResources().getDisplayMetrics().density);
            adjustViewWidth(usernameEditText, widthPixels);
            adjustViewWidth(passwordEditText, widthPixels);
            adjustViewWidth(loginButton, widthPixels);
            adjustViewWidth(createAccountButton, widthPixels);
        }

        // Set up listeners for the buttons
        loginButton.setOnClickListener(v -> handleLogin());

        createAccountButton.setOnClickListener(v -> {
            // Navigate to the user registration screen
            final Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        
        applyThemeColors(loginButton, createAccountButton);
    }

    /**
     * Handles the user login process: input validation and database authentication.
     */
    private void handleLogin() {
        final String username = usernameEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();

        // Check if fields are empty
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter both username and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Authenticate user against the database
        if (loginViewModel.authenticateUser(username, password)) {
            // Login successful: Navigate to the main inventory overview
            final Intent intent = new Intent(MainActivity.this, OverviewActivity.class);
            startActivity(intent);
            finish(); // Finish MainActivity so the user can't press 'Back' to return to the login screen
        } else {
            // Login failed
            Toast.makeText(MainActivity.this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
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

    /**
     * Applies the current theme colors to the UI elements.
     */
    private void applyThemeColors(Button loginButton, Button createAccountButton) {
        findViewById(android.R.id.content).setBackgroundColor(ThemeUtils.getBackgroundColor(this));
        loginButton.setBackgroundColor(ThemeUtils.getPrimaryColor(this));
        createAccountButton.setBackgroundColor(ThemeUtils.getPrimaryColor(this));

        int textColor = ThemeUtils.getTextColor(this);
        int hintColor = ThemeUtils.getHintColor(this);
        int textBoxColor = ThemeUtils.getTextBoxBackgroundColor(this);
        usernameEditText.setTextColor(textColor);
        usernameEditText.setHintTextColor(hintColor);
        usernameEditText.setBackgroundColor(textBoxColor);
        passwordEditText.setTextColor(textColor);
        passwordEditText.setHintTextColor(hintColor);
        passwordEditText.setBackgroundColor(textBoxColor);
    }
}