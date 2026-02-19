package com.example.cs360finalproject;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

/**
 * ViewModel for handling user registration logic.
 */
public class RegisterViewModel extends AndroidViewModel {

    private final UserRepository userRepository;

    public RegisterViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    /**
     * Registers a new user.
     * @param username The username.
     * @param password The password.
     * @return true if registration is successful.
     */
    public boolean registerUser(String username, String password) {
        return userRepository.registerUser(username, password);
    }
}