package com.example.cs360finalproject;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for new user registration. Handles input, validation, and database storage.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createaccount);

        databaseHelper = new DatabaseHelper(this);

        // Initialize UI elements
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        final Button registerButton = findViewById(R.id.createAccountButton);
        final Button goBackButton = findViewById(R.id.goBackButton);

        // Set up listeners
        registerButton.setOnClickListener(v -> registerUser());
        goBackButton.setOnClickListener(v -> finish()); // Simply close this activity to return to login
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
        final boolean isSuccess = databaseHelper.registerUser(username, password);

        if (isSuccess) {
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
            finish(); // Close this activity and return to the login screen
        } else {
            // This usually means the username already exists due to the UNIQUE constraint in the DB
            Toast.makeText(this, "Registration failed. Username may already be taken.", Toast.LENGTH_LONG).show();
        }
    }
}