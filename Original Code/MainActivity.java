package com.example.cs360finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * The main login activity for the application. Handles user authentication
 * and navigation to the inventory overview or registration screen.
 */
public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db; // Database access object
    private EditText usernameEditText;
    private EditText passwordEditText;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DatabaseHelper(this); // Initialize the database helper

        // Initialize UI elements
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        final Button loginButton = findViewById(R.id.loginButton);
        final Button createAccountButton = findViewById(R.id.createAccountButton);

        // Set up listeners for the buttons
        loginButton.setOnClickListener(v -> handleLogin());

        createAccountButton.setOnClickListener(v -> {
            // Navigate to the user registration screen
            final Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
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
        if (db.authenticateUser(username, password)) {
            // Login successful: Navigate to the main inventory overview
            final Intent intent = new Intent(MainActivity.this, OverviewActivity.class);
            startActivity(intent);
            finish(); // Finish MainActivity so the user can't press 'Back' to return to the login screen
        } else {
            // Login failed
            Toast.makeText(MainActivity.this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
        }
    }
}