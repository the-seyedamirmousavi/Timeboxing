package com.mid.timeboxing;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.mid.timeboxing.data.TimeboxStore;
import com.mid.timeboxing.model.DayKey;
import com.mid.timeboxing.model.DayPlan;
import com.mid.timeboxing.model.TimeBox;
import com.mid.timeboxing.model.TimeMath;
import com.mid.timeboxing.ui.BoxEditorSheet;
import com.mid.timeboxing.ui.DayGridView;
import com.mid.timeboxing.ui.RingView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements DayGridView.Listener, BoxEditorSheet.Callback {

    private static final long TICK_MS = 1000L;
    private static final int DEFAULT_PLANNING_START = 9 * 60;
    private static final int NAV_BAR_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b);

    private enum NowAction { MARK_DONE, UNDO_DONE, BOX_THIS_SLOT }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            onTick();
            handler.postDelayed(this, TICK_MS - System.currentTimeMillis() % TICK_MS);
        }
    };

    private TimeboxStore store;
    private int dayKey;
    private int lastSeenToday;
    private int lastRenderedMinute = -1;
    private DayPlan plan;
    private NowAction nowAction;
    private boolean bindingPriorities;

    private TextView dateLabel;
    private View nowCard;
    private RingView nowRing;
    private TextView nowCountdown;
    private TextView nowTitle;
    private View nowStat1;
    private View nowStat2;
    private View nowStat3;
    private MaterialButton nowActionButton;
    private final EditText[] priorityInputs = new EditText[TimeboxStore.PRIORITY_COUNT];
    private View statBoxed;
    private View statBlocks;
    private View statDone;
    private View statOpen;
    private DayGridView dayGrid;
    private LinearLayout scheduleRows;
    private View scheduleEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The UI is light in every system mode, so the bar icons stay dark. Devices that
        // cannot draw dark navigation icons (API < 26) get a scrim instead.
        EdgeToEdge.enable(this,
                SystemBarStyle.Companion.light(Color.TRANSPARENT, Color.TRANSPARENT),
                SystemBarStyle.Companion.light(Color.TRANSPARENT, NAV_BAR_SCRIM));
        setContentView(R.layout.activity_main);
        applyInsets();

        store = new TimeboxStore(this);
        bindViews();

        lastSeenToday = DayKey.today();
        loadDay(lastSeenToday);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void applyInsets() {
        View header = findViewById(R.id.header);
        View content = findViewById(R.id.content);
        int headerTop = header.getPaddingTop();
        int contentBottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, 0, bars.right, 0);
            header.setPadding(header.getPaddingLeft(), headerTop + bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),
                    content.getPaddingRight(), contentBottom + bars.bottom);
            return insets;
        });
    }

    private void bindViews() {
        dateLabel = findViewById(R.id.dateLabel);
        findViewById(R.id.prevDay).setOnClickListener(v -> loadDay(DayKey.shift(dayKey, -1)));
        findViewById(R.id.nextDay).setOnClickListener(v -> loadDay(DayKey.shift(dayKey, 1)));
        dateLabel.setOnClickListener(v -> loadDay(DayKey.today()));

        nowCard = findViewById(R.id.nowCard);
        nowRing = findViewById(R.id.nowRing);
        nowCountdown = findViewById(R.id.nowCountdown);
        nowTitle = findViewById(R.id.nowTitle);
        nowStat1 = findViewById(R.id.nowStat1);
        nowStat2 = findViewById(R.id.nowStat2);
        nowStat3 = findViewById(R.id.nowStat3);
        nowActionButton = findViewById(R.id.nowAction);
        nowActionButton.setOnClickListener(v -> onNowAction());

        int[] priorityIds = {R.id.priority1, R.id.priority2, R.id.priority3};
        for (int i = 0; i < priorityIds.length; i++) {
            int index = i;
            priorityInputs[i] = findViewById(priorityIds[i]);
            priorityInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!bindingPriorities) {
                        store.setPriority(dayKey, index, s.toString());
                    }
                }
            });
        }

        statBoxed = findViewById(R.id.statBoxed);
        statBlocks = findViewById(R.id.statBlocks);
        statDone = findViewById(R.id.statDone);
        statOpen = findViewById(R.id.statOpen);

        dayGrid = findViewById(R.id.dayGrid);
        dayGrid.setListener(this);

        scheduleRows = findViewById(R.id.scheduleRows);
        scheduleEmpty = findViewById(R.id.scheduleEmpty);
        findViewById(R.id.addBox).setOnClickListener(v -> openEditor(null, defaultStart()));
    }

    private void loadDay(int key) {
        View focused = getCurrentFocus();
        if (focused != null) {
            focused.clearFocus();
        }
        dayKey = key;
        plan = store.load(key);

        bindingPriorities = true;
        for (int i = 0; i < priorityInputs.length; i++) {
            priorityInputs[i].setText(store.priority(key, i));
        }
        bindingPriorities = false;

        renderAll();
    }

    private void onTick() {
        int today = DayKey.today();
        if (today != lastSeenToday) {
            // Midnight passed: follow the calendar if the user was looking at "today".
            boolean wasViewingToday = dayKey == lastSeenToday;
            lastSeenToday = today;
            if (wasViewingToday) {
                loadDay(today);
                return;
            }
        }
        renderNow();
        int nowMinute = isToday() ? secondsOfDay() / 60 : -1;
        if (nowMinute != lastRenderedMinute) {
            renderDay();
        }
    }

    private boolean isToday() {
        return dayKey == DayKey.today();
    }

    private static int secondsOfDay() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND);
    }

    // region Rendering

    private void renderAll() {
        renderHeader();
        renderNow();
        renderDay();
    }

    private void renderHeader() {
        int today = DayKey.today();
        String date = DateFormat.format("EEE, d MMM", DayKey.toCalendar(dayKey)).toString();
        String relative = null;
        if (dayKey == today) {
            relative = getString(R.string.today);
        } else if (dayKey == DayKey.shift(today, 1)) {
            relative = getString(R.string.tomorrow);
        } else if (dayKey == DayKey.shift(today, -1)) {
            relative = getString(R.string.yesterday);
        }
        dateLabel.setText(relative != null ? getString(R.string.date_with_relative, relative, date) : date);
    }

    /** Stats, grid and schedule: everything that changes at most once a minute. */
    private void renderDay() {
        int nowMinute = isToday() ? secondsOfDay() / 60 : -1;
        lastRenderedMinute = nowMinute;

        setStat(statBoxed, getString(R.string.hours_value, TimeMath.hours(plan.boxedMinutes())),
                R.string.stat_boxed);
        setStat(statBlocks, String.valueOf(plan.boxedMinutes() / TimeMath.SLOT_MINUTES), R.string.stat_blocks);
        setStat(statDone, getString(R.string.percent_value, plan.completionPercent()), R.string.stat_done);
        setStat(statOpen, String.valueOf(plan.openCount()), R.string.stat_open);

        dayGrid.setPlan(plan, nowMinute);
        renderSchedule(nowMinute);
    }

    private void renderNow() {
        if (!isToday()) {
            nowCard.setVisibility(View.GONE);
            return;
        }
        nowCard.setVisibility(View.VISIBLE);

        int seconds = secondsOfDay();
        int minute = seconds / 60;
        TimeBox current = plan.boxAt(minute);
        TimeBox next = plan.nextAfter(minute);
        String nextLabel = next != null ? TimeMath.clock(next.start) : getString(R.string.stat_none);

        if (current != null) {
            nowCountdown.setText(TimeMath.countdown(current.end() * 60L - seconds));
            nowTitle.setText(current.title);
            nowRing.setProgress((seconds - current.start * 60f) / (current.duration * 60f), current.done);
            setStat(nowStat1, TimeMath.clock(current.start), R.string.stat_started);
            setStat(nowStat2, TimeMath.clock(current.end()), R.string.stat_ends);
            setStat(nowStat3, nextLabel, R.string.stat_next);
            setNowAction(current.done ? NowAction.UNDO_DONE : NowAction.MARK_DONE);
        } else {
            if (next != null) {
                nowCountdown.setText(TimeMath.countdown(next.start * 60L - seconds));
                nowTitle.setText(getString(R.string.now_next_up, next.title));
            } else {
                nowCountdown.setText(R.string.now_empty_countdown);
                nowTitle.setText(R.string.now_no_box);
            }
            nowRing.setProgress(0f, false);
            setStat(nowStat1, TimeMath.clock(minute), R.string.stat_now);
            setStat(nowStat2, nextLabel, R.string.stat_next);
            setStat(nowStat3, TimeMath.duration(plan.freeMinutesFrom(minute)), R.string.stat_free);
            setNowAction(NowAction.BOX_THIS_SLOT);
        }
    }

    private void setNowAction(NowAction action) {
        if (action == nowAction) {
            return;
        }
        nowAction = action;
        switch (action) {
            case MARK_DONE:
                nowActionButton.setText(R.string.action_mark_done);
                nowActionButton.setIconResource(R.drawable.ic_check);
                break;
            case UNDO_DONE:
                nowActionButton.setText(R.string.action_undo_done);
                nowActionButton.setIconResource(R.drawable.ic_check);
                break;
            case BOX_THIS_SLOT:
                nowActionButton.setText(R.string.action_box_this_slot);
                nowActionButton.setIconResource(R.drawable.ic_add);
                break;
        }
    }

    private void renderSchedule(int nowMinute) {
        scheduleRows.removeAllViews();
        scheduleEmpty.setVisibility(plan.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = getLayoutInflater();
        for (TimeBox box : plan.boxes()) {
            View row = inflater.inflate(R.layout.item_box, scheduleRows, false);
            TextView title = row.findViewById(R.id.boxTitle);
            ((TextView) row.findViewById(R.id.boxStart)).setText(TimeMath.clock(box.start));
            ((TextView) row.findViewById(R.id.boxEnd)).setText(TimeMath.clock(box.end()));
            ((TextView) row.findViewById(R.id.boxMinutes)).setText(String.valueOf(box.duration));
            title.setText(box.title);
            row.findViewById(R.id.boxMarker).setBackgroundResource(
                    box.done ? R.drawable.bg_block_done : R.drawable.bg_block_boxed);

            if (box.done) {
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                title.setTextColor(getColor(R.color.tb_text_secondary));
            } else if (box.contains(nowMinute)) {
                title.setTypeface(title.getTypeface(), Typeface.BOLD);
            }

            row.setOnClickListener(v -> openEditor(box, box.start));
            row.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                toggleDone(box);
                return true;
            });
            scheduleRows.addView(row);
        }
    }

    private static void setStat(View stat, String value, int labelRes) {
        ((TextView) stat.findViewById(R.id.statValue)).setText(value);
        ((TextView) stat.findViewById(R.id.statLabel)).setText(labelRes);
    }

    // endregion

    // region Actions

    private void onNowAction() {
        int minute = secondsOfDay() / 60;
        TimeBox current = plan.boxAt(minute);
        if (current != null) {
            nowActionButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            toggleDone(current);
        } else {
            openEditor(null, TimeMath.snapDown(minute));
        }
    }

    private void toggleDone(TimeBox box) {
        plan.upsert(box.withDone(!box.done));
        persistAndRender();
    }

    /** Where a new box should start: the next free slot from now, or after the last box. */
    private int defaultStart() {
        if (isToday()) {
            int now = TimeMath.snapDown(secondsOfDay() / 60);
            int free = plan.firstFreeSlot(now);
            return free >= 0 ? free : now;
        }
        if (plan.isEmpty()) {
            return DEFAULT_PLANNING_START;
        }
        int free = plan.firstFreeSlot(plan.lastEnd());
        return free >= 0 ? free : DEFAULT_PLANNING_START;
    }

    private void openEditor(TimeBox box, int start) {
        BoxEditorSheet.show(this, plan, box, start, this);
    }

    @Override
    public void onSave(TimeBox box) {
        plan.upsert(box);
        persistAndRender();
    }

    @Override
    public void onDelete(TimeBox box) {
        plan.remove(box.id);
        persistAndRender();
    }

    @Override
    public void onSlotTapped(int minute) {
        openEditor(null, minute);
    }

    @Override
    public void onBoxTapped(TimeBox box) {
        openEditor(box, box.start);
    }

    private void persistAndRender() {
        store.save(dayKey, plan);
        renderNow();
        renderDay();
    }

    // endregion
}
