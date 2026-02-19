package com.example.cs360finalproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Custom View to draw a simple bar chart of usage history.
 */
public class UsageGraphView extends View {
    private final Map<String, List<Integer>> data;
    private final Paint paint = new Paint();
    private final int[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.DKGRAY};
    private final int barColor;
    private final int textColor;
    private final long startTime;
    private final long stepTime;
    private float touchX = -1;
    private boolean isTouching = false;
    
    // Optimization: Reuse formatters and date objects to avoid allocation in onDraw
    private final SimpleDateFormat axisTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat tooltipTimeFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
    private final Date dateBuffer = new Date();

    public UsageGraphView(Context context, Map<String, List<Integer>> data, int barColor, int textColor, long startTime, long stepTime) {
        super(context);
        this.data = data;
        this.barColor = barColor;
        this.textColor = textColor;
        this.startTime = startTime;
        this.stepTime = stepTime;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                touchX = event.getX();
                isTouching = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouching = false;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int padding = 100;
        int graphHeight = height - (padding * 2);
        int graphWidth = width - (padding * 2);

        // Draw axes
        paint.setColor(textColor);
        paint.setStrokeWidth(2);
        canvas.drawLine(padding, height - padding, width - padding, height - padding, paint); // X axis
        canvas.drawLine(padding, height - padding, padding, padding, paint); // Y axis

        if (data == null || data.isEmpty()) {
            paint.setTextSize(40);
            canvas.drawText("No Data", width / 2f - 50, height / 2f, paint);
            return;
        }

        // Find max value across all lists
        int maxVal = 0;
        for (List<Integer> list : data.values()) {
            for (int val : list) {
                if (val > maxVal) maxVal = val;
            }
        }
        if (maxVal == 0) maxVal = 1;

        // Draw Y-axis labels
        paint.setTextSize(30);
        for (int i = 0; i <= 5; i++) {
            int val = (int) (maxVal * ((float) i / 5));
            float y = (height - padding) - ((float) val / maxVal) * graphHeight;

            paint.setColor(Color.LTGRAY);
            paint.setStrokeWidth(1);
            canvas.drawLine(padding, y, width - padding, y, paint);

            paint.setColor(textColor);
            String label = String.valueOf(val);
            float textWidth = paint.measureText(label);
            canvas.drawText(label, padding - textWidth - 10, y + 10, paint);
        }

        int colorIndex = 0;
        paint.setStrokeWidth(5);
        paint.setTextSize(30);

        int numPoints = 0;
        if (!data.isEmpty()) {
            numPoints = data.values().iterator().next().size();
        }
        if (numPoints < 2) numPoints = 2; // Prevent division by zero
        float xStep = (float) graphWidth / (numPoints - 1);

        for (Map.Entry<String, List<Integer>> entry : data.entrySet()) {
            List<Integer> points = entry.getValue();
            paint.setColor(colors[colorIndex % colors.length]);

            for (int i = 0; i < points.size() - 1; i++) {
                float startX = padding + (i * xStep);
                float startY = (height - padding) - ((float) points.get(i) / maxVal) * graphHeight;
                float endX = padding + ((i + 1) * xStep);
                float endY = (height - padding) - ((float) points.get(i + 1) / maxVal) * graphHeight;
                canvas.drawLine(startX, startY, endX, endY, paint);
            }
            
            // Draw label at the end of the line
            if (!points.isEmpty()) {
                 float lastX = padding + ((points.size() - 1) * xStep);
                 float lastY = (height - padding) - ((float) points.get(points.size() - 1) / maxVal) * graphHeight;
                 // Offset labels slightly to avoid overlap when values are identical (e.g. 0)
                 canvas.drawText(entry.getKey(), lastX - 20, lastY - 10 - ((colorIndex % 4) * 30), paint);
            }

            colorIndex++;
        }

        // Draw X-axis time labels
        paint.setColor(textColor);
        paint.setTextSize(30);
        if (numPoints > 1) {
            long totalDuration = numPoints * stepTime;
            String formatString = "HH:mm";
            if (totalDuration > 48 * 60 * 60 * 1000L) { // If > 48 hours, show Date
                formatString = "MM/dd";
            }
            axisTimeFormat.applyPattern(formatString);
            
            // Calculate skip to avoid overlap (approx 120px per label)
            int labelSkip = (int) (120 / xStep);
            if (labelSkip < 1) labelSkip = 1;

            for (int i = 0; i < numPoints; i += labelSkip) {
                float x = padding + (i * xStep);
                long time = startTime + (i * stepTime);
                dateBuffer.setTime(time);
                String label = axisTimeFormat.format(dateBuffer);
                canvas.drawText(label, x - 30, height - 15, paint);
            }
        }

        // Draw interactive tooltip
        if (isTouching && numPoints > 1) {
            int index = Math.round((touchX - padding) / xStep);
            if (index < 0) index = 0;
            if (index >= numPoints) index = numPoints - 1;

            float pointX = padding + (index * xStep);

            // Draw vertical indicator line
            paint.setColor(Color.GRAY);
            paint.setStrokeWidth(2);
            canvas.drawLine(pointX, padding, pointX, height - padding, paint);

            // Draw points and tooltip
            int yOffset = 0;
            for (Map.Entry<String, List<Integer>> entry : data.entrySet()) {
                List<Integer> points = entry.getValue();
                if (index < points.size()) {
                    int val = points.get(index);
                    float pointY = (height - padding) - ((float) val / maxVal) * graphHeight;

                    // Draw circle
                    paint.setColor(textColor);
                    canvas.drawCircle(pointX, pointY, 10, paint);
                    paint.setColor(barColor);
                    canvas.drawCircle(pointX, pointY, 6, paint);

                    // Draw Value Text
                    paint.setColor(textColor);
                    paint.setTextSize(35);
                    String text = entry.getKey() + ": " + val;
                    canvas.drawText(text, pointX + 15, pointY - 15 - yOffset, paint);
                }
            }

            // Draw Time at top
            long time = startTime + (index * stepTime);
            dateBuffer.setTime(time);
            String timeStr = tooltipTimeFormat.format(dateBuffer);
            paint.setColor(textColor);
            paint.setTextSize(30);
            canvas.drawText(timeStr, padding, padding - 10, paint);
        }
    }
}