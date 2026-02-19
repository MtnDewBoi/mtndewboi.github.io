package com.example.cs360finalproject;

import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * Utility class for sending SMS messages.
 * Requires the SEND_SMS permission to be granted by the user.
 */
public class SMSHelper {

    private static final String TAG = "SMSManager";

    /**
     * Sends an SMS message to the specified phone number.
     * @param context The application context.
     * @param phoneNumber The recipient's phone number.
     * @param message The message to send.
     * @return true if SMS was sent successfully, false otherwise.
     */
    public static boolean sendSMS(final Context context, final String phoneNumber, final String message) {
        // Basic input validation
        if (!isValidPhoneNumber(phoneNumber) || message == null || message.isEmpty()) {
            Log.e(TAG, "Invalid phone number or empty message for SMS.");
            return false;
        }

        try {
            // Get the default SmsManager instance
            final SmsManager smsManager = context.getSystemService(SmsManager.class);
            // Send the text message
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.d(TAG, "SMS sent successfully to " + phoneNumber);
            return true;
        } catch (final Exception e) {
            // Catch security exceptions if permission is revoked or other SMS errors
            Log.e(TAG, "Failed to send SMS: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates the phone number format using Android's built-in pattern matcher.
     * @param phoneNumber The phone number string.
     * @return true if the format is valid, false otherwise.
     */
    public static boolean isValidPhoneNumber(final String phoneNumber) {
        return phoneNumber != null && android.util.Patterns.PHONE.matcher(phoneNumber).matches();
    }
}