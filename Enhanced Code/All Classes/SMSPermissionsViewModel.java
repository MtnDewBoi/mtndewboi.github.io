package com.example.cs360finalproject;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

/**
 * ViewModel for managing SMS permission preferences and phone number storage.
 */
public class SMSPermissionsViewModel extends AndroidViewModel {

    private final SharedPreferences sharedPreferences;

    public SMSPermissionsViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(SMSPermissionsActivity.PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Retrieves the stored phone number.
     * @return The phone number string.
     */
    public String getPhoneNumber() {
        return sharedPreferences.getString(SMSPermissionsActivity.KEY_PHONE_NUMBER, "");
    }

    /**
     * Checks if SMS permissions are enabled in preferences.
     * @return True if enabled, false otherwise.
     */
    public boolean isSmsPermissionEnabled() {
        return sharedPreferences.getBoolean(SMSPermissionsActivity.KEY_SMS_PERMISSION, false);
    }

    /**
     * Saves the phone number and SMS permission preference.
     * @param phoneNumber The phone number to save.
     * @param smsPermission The permission state to save.
     */
    public void savePreferences(String phoneNumber, boolean smsPermission) {
        sharedPreferences.edit()
                .putString(SMSPermissionsActivity.KEY_PHONE_NUMBER, phoneNumber)
                .putBoolean(SMSPermissionsActivity.KEY_SMS_PERMISSION, smsPermission)
                .apply();
    }

    /**
     * Clears all stored preferences.
     */
    public void clearPreferences() {
        sharedPreferences.edit().clear().apply();
    }
}