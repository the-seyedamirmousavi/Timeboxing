package com.mid.timeboxing.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.mid.timeboxing.R;
import com.mid.timeboxing.model.DayPlan;
import com.mid.timeboxing.model.TimeBox;
import com.mid.timeboxing.model.TimeMath;

import java.util.Locale;

/**
 * The whole day as rows of hours, each split into twelve 5-minute cells.
 * Cells are grouped in quarter hours so the eye can read the time without labels.
 */
public class DayGridView extends View {

    public interface Listener {
        void onSlotTapped(int minute);

        void onBoxTapped(TimeBox box);
    }

    private static final int CELLS_PER_HOUR = 60 / TimeMath.SLOT_MINUTES;
    private static final int CELLS_PER_QUARTER = 3;
    private static final int DEFAULT_FIRST_HOUR = 6;

    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final float labelWidth;
    private final float rowHeight;
    private final float cellGap;
    private final float quarterGap;
    private final float radius;
    private final float nowStroke;
    private final int colorEmpty;
    private final int colorPast;
    private final int colorBoxed;
    private final int colorDone;
    private final int colorAccent;
    private final int touchSlop;

    @Nullable
    private DayPlan plan;
    @Nullable
    private Listener listener;
    private int nowMinute = -1;
    private int firstHour = DEFAULT_FIRST_HOUR;
    private float downX;
    private float downY;

    public DayGridView(Context context) {
        this(context, null);
    }

    public DayGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        labelWidth = res.getDimension(R.dimen.grid_label_width);
        rowHeight = res.getDimension(R.dimen.grid_row_height);
        cellGap = res.getDimension(R.dimen.grid_cell_gap);
        quarterGap = res.getDimension(R.dimen.grid_quarter_gap);
        radius = res.getDimension(R.dimen.grid_cell_radius);
        nowStroke = res.getDimension(R.dimen.grid_now_stroke);
        colorEmpty = ContextCompat.getColor(context, R.color.tb_field);
        colorPast = ContextCompat.getColor(context, R.color.tb_block_past);
        colorBoxed = ContextCompat.getColor(context, R.color.tb_button);
        colorDone = ContextCompat.getColor(context, R.color.tb_accent);
        colorAccent = ContextCompat.getColor(context, R.color.tb_accent);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        nowPaint.setStyle(Paint.Style.STROKE);
        nowPaint.setStrokeWidth(nowStroke);
        labelPaint.setColor(ContextCompat.getColor(context, R.color.tb_text_secondary));
        labelPaint.setTextSize(res.getDimension(R.dimen.grid_label_text));
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    /** @param now minute of day to highlight, or -1 when the plan is not for today */
    public void setPlan(DayPlan dayPlan, int now) {
        plan = dayPlan;
        nowMinute = now;
        int first = DEFAULT_FIRST_HOUR;
        if (dayPlan.firstStart() >= 0) {
            first = Math.min(first, dayPlan.firstStart() / 60);
        }
        if (now >= 0) {
            first = Math.min(first, now / 60);
        }
        if (first != firstHour) {
            firstHour = first;
            requestLayout();
        }
        int count = dayPlan.boxes().size();
        setContentDescription(getResources().getQuantityString(R.plurals.grid_description, count, count));
        invalidate();
    }

    private int rows() {
        return 24 - firstHour;
    }

    private float gridWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight() - labelWidth;
    }

    private float cellWidth() {
        int quarterGaps = CELLS_PER_HOUR / CELLS_PER_QUARTER - 1;
        int cellGaps = CELLS_PER_HOUR - 1 - quarterGaps;
        return (gridWidth() - cellGaps * cellGap - quarterGaps * quarterGap) / CELLS_PER_HOUR;
    }

    private float cellLeft(int cell, float width) {
        int quarterGapsBefore = cell / CELLS_PER_QUARTER;
        int cellGapsBefore = cell - quarterGapsBefore;
        return getPaddingLeft() + labelWidth + cell * width
                + cellGapsBefore * cellGap + quarterGapsBefore * quarterGap;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int desiredHeight = (int) Math.ceil(rows() * rowHeight - cellGap)
                + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = cellWidth();
        float cellHeight = rowHeight - cellGap;
        Paint.FontMetrics fm = labelPaint.getFontMetrics();
        float labelOffset = cellHeight / 2f - (fm.ascent + fm.descent) / 2f;

        for (int row = 0; row < rows(); row++) {
            int hour = firstHour + row;
            float top = getPaddingTop() + row * rowHeight;
            canvas.drawText(String.format(Locale.US, "%02d", hour),
                    getPaddingLeft(), top + labelOffset, labelPaint);

            for (int cell = 0; cell < CELLS_PER_HOUR; cell++) {
                int minute = hour * 60 + cell * TimeMath.SLOT_MINUTES;
                TimeBox box = plan != null ? plan.boxAt(minute) : null;
                float left = cellLeft(cell, width);
                rect.set(left, top, left + width, top + cellHeight);

                if (box != null) {
                    cellPaint.setColor(box.done ? colorDone : colorBoxed);
                } else if (nowMinute >= 0 && minute + TimeMath.SLOT_MINUTES <= nowMinute) {
                    cellPaint.setColor(colorPast);
                } else {
                    cellPaint.setColor(colorEmpty);
                }
                canvas.drawRoundRect(rect, radius, radius, cellPaint);

                boolean isNow = nowMinute >= minute && nowMinute < minute + TimeMath.SLOT_MINUTES;
                if (isNow) {
                    nowPaint.setColor(box != null && box.done ? colorBoxed : colorAccent);
                    float inset = nowStroke / 2f;
                    rect.inset(inset, inset);
                    canvas.drawRoundRect(rect, radius, radius, nowPaint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                if (Math.abs(event.getX() - downX) < touchSlop
                        && Math.abs(event.getY() - downY) < touchSlop) {
                    performClick();
                    handleTap(event.getX(), event.getY());
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void handleTap(float x, float y) {
        if (plan == null || listener == null) {
            return;
        }
        int row = (int) ((y - getPaddingTop()) / rowHeight);
        if (row < 0 || row >= rows()) {
            return;
        }
        float relative = x - getPaddingLeft() - labelWidth;
        int cell = relative <= 0 ? 0
                : Math.min(CELLS_PER_HOUR - 1, (int) (relative / (gridWidth() / CELLS_PER_HOUR)));
        int minute = (firstHour + row) * 60 + cell * TimeMath.SLOT_MINUTES;
        TimeBox box = plan.boxAt(minute);
        if (box != null) {
            listener.onBoxTapped(box);
        } else {
            listener.onSlotTapped(minute);
        }
    }
}
