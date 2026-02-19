package com.example.cs360finalproject;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

/**
 * ViewModel for handling user authentication logic.
 */
public class LoginViewModel extends AndroidViewModel {

    private final UserRepository userRepository;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    /**
     * Authenticates a user.
     * @param username The username.
     * @param password The password.
     * @return true if authentication is successful.
     */
    public boolean authenticateUser(String username, String password) {
        return userRepository.authenticateUser(username, password);
    }
}