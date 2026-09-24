package com.mid.timeboxing.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.mid.timeboxing.R;

/** FitSho-style progress ring: a light track, a dark arc, and a dot marking the leading edge. */
public class RingView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final float stroke;
    private final float dotRadius;
    private final int arcColor;
    private final int doneColor;

    private float progress;
    private boolean done;

    public RingView(Context context) {
        this(context, null);
    }

    public RingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        stroke = res.getDimension(R.dimen.ring_stroke);
        dotRadius = res.getDimension(R.dimen.ring_dot_radius);
        arcColor = ContextCompat.getColor(context, R.color.tb_button);
        doneColor = ContextCompat.getColor(context, R.color.tb_accent);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(stroke);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.tb_field));

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(stroke);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setColor(arcColor);

        dotPaint.setColor(ContextCompat.getColor(context, R.color.tb_ink));
    }

    /** @param fraction elapsed share of the block, clamped to 0..1 */
    public void setProgress(float fraction, boolean isDone) {
        float clamped = Math.max(0f, Math.min(1f, fraction));
        if (clamped == progress && isDone == done) {
            return;
        }
        progress = clamped;
        done = isDone;
        arcPaint.setColor(isDone ? doneColor : arcColor);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float inset = Math.max(stroke / 2f, dotRadius);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = size / 2f - inset;
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        canvas.drawCircle(cx, cy, radius, trackPaint);
        float sweep = 360f * (done ? 1f : progress);
        if (sweep > 0f) {
            canvas.drawArc(oval, -90f, sweep, false, arcPaint);
        }
        double angle = Math.toRadians(-90f + sweep);
        canvas.drawCircle(cx + (float) (radius * Math.cos(angle)),
                cy + (float) (radius * Math.sin(angle)), dotRadius, dotPaint);
    }
}
