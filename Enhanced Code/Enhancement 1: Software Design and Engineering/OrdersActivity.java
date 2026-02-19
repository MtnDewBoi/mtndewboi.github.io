package com.example.cs360finalproject;

import android.os.Bundle;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Activity to display order recommendations or history.
 * Currently a placeholder for future functionality.
 * Uses predictive logic based on hourly usage rates.
 */
public class OrdersActivity extends AppCompatActivity {

    private InventoryViewModel inventoryViewModel;
    private LinearLayout graphContainer;
    private UsageGraphView currentGraphView;

    /**
     * Called when the activity is first created.
     * Initializes the UI and applies the theme.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);

        inventoryViewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // Create Layout Programmatically to ensure display works without XML
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setPadding(32, 32, 32, 32);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText("Predictive Order Recommendations");
        titleView.setTextSize(20);
        titleView.setPadding(0, 32, 0, 16);
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.addView(titleView);

        // Graph Options Spinner
        Spinner graphOptionSpinner = new Spinner(this);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        spinnerParams.setMargins(0, 0, 0, 16);
        graphOptionSpinner.setLayoutParams(spinnerParams);

        List<GraphOption> options = new ArrayList<>();
        options.add(new GraphOption("1 Hr History", 60 * 60 * 1000L, 60 * 1000L, false));
        options.add(new GraphOption("3 Hrs History", 3 * 60 * 60 * 1000L, 5 * 60 * 1000L, false));
        options.add(new GraphOption("24 Hrs History", 24 * 60 * 60 * 1000L, 60 * 60 * 1000L, false));
        options.add(new GraphOption("7 Days History", 7 * 24 * 60 * 60 * 1000L, 4 * 60 * 60 * 1000L, false));
        options.add(new GraphOption("30 Days History", 30 * 24 * 60 * 60 * 1000L, 24 * 60 * 60 * 1000L, false));
        options.add(new GraphOption("1 Hr Prediction", 60 * 60 * 1000L, 60 * 1000L, true));
        options.add(new GraphOption("3 Hrs Prediction", 3 * 60 * 60 * 1000L, 5 * 60 * 1000L, true));
        options.add(new GraphOption("24 Hrs Prediction", 24 * 60 * 60 * 1000L, 60 * 60 * 1000L, true));
        options.add(new GraphOption("1 Week Prediction", 7 * 24 * 60 * 60 * 1000L, 6 * 60 * 60 * 1000L, true));
        options.add(new GraphOption("2 Weeks Prediction", 14 * 24 * 60 * 60 * 1000L, 12 * 60 * 60 * 1000L, true));

        ArrayAdapter<GraphOption> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        graphOptionSpinner.setAdapter(adapter);

        graphOptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GraphOption selected = (GraphOption) parent.getItemAtPosition(position);
                Map<String, List<Integer>> data = selected.isPrediction ? 
                        inventoryViewModel.getPredictedUsage(selected.duration, selected.step) : 
                        inventoryViewModel.getUsageHistory(selected.duration, selected.step);
                long startTime = selected.isPrediction ? System.currentTimeMillis() : System.currentTimeMillis() - selected.duration;
                updateGraph(data, startTime, selected.step);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Default to 24 Hrs Prediction (Index 7)
        graphOptionSpinner.setSelection(7);

        rootLayout.addView(graphOptionSpinner);

        // Graph Container
        graphContainer = new LinearLayout(this);
        graphContainer.setOrientation(LinearLayout.VERTICAL);
        graphContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400)); // Fixed height
        rootLayout.addView(graphContainer);

        // ScrollView for the list
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f)); // Use weight to fill space
        
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);

        // Fetch and display recommendations
        List<String> recommendations = inventoryViewModel.getOrderRecommendations();
        for (String rec : recommendations) {
            TextView recView = new TextView(this);
            recView.setText(rec);
            recView.setTextSize(16);
            recView.setPadding(0, 16, 0, 16);
            // Add a separator line visual
            recView.setBackgroundResource(android.R.drawable.list_selector_background);
            listContainer.addView(recView);
        }

        rootLayout.addView(scrollView);

        // Back Button (Moved to bottom)
        Button backButton = new Button(this);
        backButton.setText("Back");
        backButton.setId(View.generateViewId());
        backButton.setOnClickListener(v -> finish());
        rootLayout.addView(backButton);

        setContentView(rootLayout);

        applyThemeColors(rootLayout, backButton);
    }

    /**
     * Applies the current theme colors to the UI elements.
     */
    private void applyThemeColors(View rootView, Button backButton) {
        int primaryColor = ThemeUtils.getPrimaryColor(this);
        int backgroundColor = ThemeUtils.getBackgroundColor(this);

        rootView.setBackgroundColor(backgroundColor);
        ThemeUtils.applyThemeToViews(rootView, this);
        
        if (backButton != null) {
            backButton.setBackgroundColor(primaryColor);
        }
    }

    /**
     * Updates the graph view with new data.
     */
    private void updateGraph(Map<String, List<Integer>> data, long startTime, long stepTime) {
        graphContainer.removeAllViews();
        currentGraphView = new UsageGraphView(this, data, ThemeUtils.getPrimaryColor(this), ThemeUtils.getTextColor(this), startTime, stepTime);
        currentGraphView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        graphContainer.addView(currentGraphView);
    }

    private static class GraphOption {
        String label;
        long duration;
        long step;
        boolean isPrediction;

        GraphOption(String label, long duration, long step, boolean isPrediction) {
            this.label = label;
            this.duration = duration;
            this.step = step;
            this.isPrediction = isPrediction;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}