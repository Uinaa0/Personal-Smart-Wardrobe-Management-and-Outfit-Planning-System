package com.aiman.smartwardrobe.ui.analytics;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * PieChartView — Custom Canvas Donut Chart for Wardrobe Statistics
 * ============================================================================
 *
 * <p>Draws a highly polished animated donut chart representing wardrobe category
 * distributions directly onto an Android Canvas.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class PieChartView extends View {

    public static class Slice {
        private final String label;
        private final float value;
        private final int color;

        public Slice(String label, float value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }

        public String getLabel() { return label; }
        public float getValue() { return value; }
        public int getColor() { return color; }
    }

    private final List<Slice> slices = new ArrayList<>();
    private float totalValue = 0f;
    private float animationProgress = 0f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * Updates the chart data and triggers a premium entry animation.
     */
    public void setData(List<Slice> newSlices) {
        this.slices.clear();
        this.totalValue = 0f;
        
        if (newSlices != null) {
            this.slices.addAll(newSlices);
            for (Slice slice : slices) {
                totalValue += slice.value;
            }
        }

        animateEntry();
    }

    private void animateEntry() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        // Determine center and radius
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        float minDim = Math.min(centerX, centerY);
        float strokeWidth = minDim * 0.28f;
        float radius = minDim - strokeWidth;

        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);

        float currentAngle = -90f; // Start drawing from 12 o'clock

        // Draw background base track ring
        paint.setColor(Color.parseColor("#1B2336"));
        canvas.drawCircle(centerX, centerY, radius, paint);

        if (totalValue > 0f) {
            // Draw each slice as an arc segment
            for (Slice slice : slices) {
                float sweepAngle = (slice.value / totalValue) * 360f;
                paint.setColor(slice.color);

                // Animate individual arc segments
                float animatedSweep = sweepAngle * animationProgress;

                // Reduce arc bounds slightly to leave gap spaces between slices
                canvas.drawArc(rectF, currentAngle + 2f, animatedSweep - 4f, false, paint);
                currentAngle += sweepAngle;
            }
        }

        // Draw Center Info text (Donut Hole Center overlay)
        paint.setStyle(Paint.Style.FILL);
        
        // Main Total Number Text
        paint.setColor(Color.WHITE);
        paint.setTextSize(minDim * 0.25f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        String totalText = String.valueOf((int) totalValue);
        canvas.drawText(totalText, centerX, centerY + (paint.getTextSize() * 0.15f), paint);

        // Subtext Label (e.g. "Items")
        paint.setColor(Color.parseColor("#8A9AA4"));
        paint.setTextSize(minDim * 0.11f);
        paint.setFakeBoldText(false);
        canvas.drawText("ITEMS", centerX, centerY + (minDim * 0.25f), paint);
    }
}
