package com.example.cs360finalproject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Utility class to manage application theme preferences and application.
 */
public class ThemeUtils {

    public static final String PREFS_NAME = "ThemePrefs";
    public static final String KEY_THEME = "selected_theme";

    public static final int THEME_GRAY = 0;
    public static final int THEME_BLUE = 1;
    public static final int THEME_GREEN = 2;

    /**
     * Applies the saved theme to the activity. Must be called before setContentView().
     * @param activity The activity to apply the theme to.
     */
    public static void applyTheme(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_GRAY);

        switch (theme) {
            case THEME_BLUE:
                activity.setTheme(R.style.Theme_CS360FinalProject_Blue);
                break;
            case THEME_GREEN:
                activity.setTheme(R.style.Theme_CS360FinalProject_Green);
                break;
            default:
                activity.setTheme(R.style.Theme_CS360FinalProject);
                break;
        }
    }

    /**
     * Saves the selected theme preference.
     * @param context Application context.
     * @param theme The theme ID to save.
     */
    public static void saveTheme(Context context, int theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, theme).apply();
    }

    /**
     * Retrieves the currently selected theme ID.
     * @param context Application context.
     * @return The theme ID.
     */
    public static int getSelectedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_GRAY);
    }

    /**
     * Gets the primary color associated with the current theme.
     * @param context Application context.
     * @return The color integer.
     */
    public static int getPrimaryColor(Context context) {
        int theme = getSelectedTheme(context);
        switch (theme) {
            case THEME_BLUE: return 0xFF0288D1; // Light Blue
            case THEME_GREEN: return 0xFF2E7D32; // Green
            default: return 0xFF616161; // Gray
        }
    }

    /**
     * Gets the background color based on the theme and dark mode state.
     * @param context Application context.
     * @return The color integer.
     */
    public static int getBackgroundColor(Context context) {
        boolean isDark = isDarkMode(context);
        int theme = getSelectedTheme(context);
        switch (theme) {
            case THEME_BLUE: return isDark ? 0xFF001F2D : 0xFFE1F5FE; // Dark Navy vs Very Light Blue
            case THEME_GREEN: return isDark ? 0xFF001500 : 0xFFE8F5E9; // Very Dark Green vs Very Light Green
            default: return isDark ? 0xFF121212 : Color.WHITE; // Standard Dark vs White
        }
    }

    /**
     * Checks if the system is currently in dark mode.
     * @param context Application context.
     * @return true if in dark mode, false otherwise.
     */
    public static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Gets the appropriate text color for the current mode.
     * @param context Application context.
     * @return The color integer.
     */
    public static int getTextColor(Context context) {
        return isDarkMode(context) ? Color.WHITE : Color.BLACK;
    }

    /**
     * Gets the appropriate hint text color for the current mode.
     * @param context Application context.
     * @return The color integer.
     */
    public static int getHintColor(Context context) {
        return isDarkMode(context) ? Color.LTGRAY : Color.GRAY;
    }

    /**
     * Gets the background color for text boxes (EditText).
     * @param context Application context.
     * @return The color integer.
     */
    public static int getTextBoxBackgroundColor(Context context) {
        return isDarkMode(context) ? 0xFF2A2A2A : Color.WHITE;
    }

    /**
     * Recursively applies text and hint colors to all views in the hierarchy.
     * This ensures that labels and headers (which might not have IDs) are visible in Dark Mode.
     * @param view The root view to start traversing from.
     * @param context The context to retrieve colors.
     */
    public static void applyThemeToViews(View view, Context context) {
        int textColor = getTextColor(context);
        int hintColor = getHintColor(context);
        int textBoxColor = getTextBoxBackgroundColor(context);

        if (view instanceof android.widget.EditText) {
            ((android.widget.EditText) view).setTextColor(textColor);
            ((android.widget.EditText) view).setHintTextColor(hintColor);
            view.setBackgroundColor(textBoxColor);
        } else if (view instanceof TextView && !(view instanceof android.widget.Button)) {
            // Apply to TextViews (labels, headers) but skip Buttons as they have their own styling
            ((TextView) view).setTextColor(textColor);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyThemeToViews(group.getChildAt(i), context);
            }
        }
    }
}