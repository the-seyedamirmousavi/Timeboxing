package com.mid.timeboxing.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.mid.timeboxing.R;
import com.mid.timeboxing.model.DayPlan;
import com.mid.timeboxing.model.TimeBox;
import com.mid.timeboxing.model.TimeMath;

import java.util.UUID;

/** Bottom sheet for creating or editing a box. Rejects overlaps: one thing at a time. */
public final class BoxEditorSheet {

    public interface Callback {
        void onSave(TimeBox box);

        void onDelete(TimeBox box);
    }

    private static final int DEFAULT_DURATION = 30;

    private final AppCompatActivity activity;
    private final DayPlan plan;
    @Nullable
    private final TimeBox existing;
    private final Callback callback;
    private final BottomSheetDialog dialog;

    private final EditText titleInput;
    private final TextView startField;
    private final TextView endField;
    private final TextView durationField;
    private final TextView errorText;
    private final TextView[] chips;
    private final int[] chipMinutes = {5, 15, 30, 60, 90};

    private int start;
    private int duration;

    private BoxEditorSheet(AppCompatActivity activity, DayPlan plan, @Nullable TimeBox existing,
            int defaultStart, Callback callback) {
        this.activity = activity;
        this.plan = plan;
        this.existing = existing;
        this.callback = callback;

        dialog = new BottomSheetDialog(activity);
        // The dialog supplies the parent; setContentView attaches the view to it.
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(activity).inflate(R.layout.sheet_box, null, false);
        dialog.setContentView(view);

        titleInput = view.findViewById(R.id.boxTitle);
        startField = view.findViewById(R.id.boxStart);
        endField = view.findViewById(R.id.boxEnd);
        durationField = view.findViewById(R.id.boxDuration);
        errorText = view.findViewById(R.id.boxError);
        chips = new TextView[]{
                view.findViewById(R.id.chip5),
                view.findViewById(R.id.chip15),
                view.findViewById(R.id.chip30),
                view.findViewById(R.id.chip60),
                view.findViewById(R.id.chip90),
        };

        TextView sheetTitle = view.findViewById(R.id.sheetTitle);
        View deleteButton = view.findViewById(R.id.boxDelete);
        if (existing != null) {
            sheetTitle.setText(R.string.sheet_edit_box);
            titleInput.setText(existing.title);
            start = existing.start;
            duration = existing.duration;
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> {
                callback.onDelete(existing);
                dialog.dismiss();
            });
        } else {
            sheetTitle.setText(R.string.sheet_new_box);
            start = TimeMath.snapDown(defaultStart);
            duration = Math.min(DEFAULT_DURATION, TimeMath.MINUTES_PER_DAY - start);
        }

        startField.setOnClickListener(v -> pickStart());
        view.findViewById(R.id.durationMinus).setOnClickListener(
                v -> setDuration(duration - TimeMath.SLOT_MINUTES));
        view.findViewById(R.id.durationPlus).setOnClickListener(
                v -> setDuration(duration + TimeMath.SLOT_MINUTES));
        for (int i = 0; i < chips.length; i++) {
            int minutes = chipMinutes[i];
            chips[i].setOnClickListener(v -> setDuration(minutes));
        }
        view.findViewById(R.id.boxSave).setOnClickListener(v -> save());

        dialog.getBehavior().setSkipCollapsed(true);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        if (existing == null && dialog.getWindow() != null) {
            titleInput.requestFocus();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        render();
    }

    public static void show(AppCompatActivity activity, DayPlan plan, @Nullable TimeBox existing,
            int defaultStart, Callback callback) {
        new BoxEditorSheet(activity, plan, existing, defaultStart, callback).dialog.show();
    }

    private void pickStart() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(start / 60)
                .setMinute(start % 60)
                .setTitleText(R.string.pick_start_time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            start = TimeMath.snapDown(picker.getHour() * 60 + picker.getMinute());
            setDuration(duration);
        });
        picker.show(activity.getSupportFragmentManager(), "start_time");
    }

    private void setDuration(int minutes) {
        int max = TimeMath.MINUTES_PER_DAY - start;
        duration = Math.max(TimeMath.SLOT_MINUTES, Math.min(max, minutes));
        render();
    }

    private void render() {
        startField.setText(TimeMath.clock(start));
        endField.setText(TimeMath.clock(start + duration));
        durationField.setText(activity.getString(R.string.minutes_value, duration));
        for (int i = 0; i < chips.length; i++) {
            chips[i].setSelected(chipMinutes[i] == duration);
        }
        errorText.setVisibility(View.GONE);
    }

    private void save() {
        String title = titleInput.getText().toString().trim();
        if (title.isEmpty()) {
            showError(activity.getString(R.string.error_title_required));
            return;
        }
        String id = existing != null ? existing.id : UUID.randomUUID().toString();
        TimeBox clash = plan.conflict(start, duration, id);
        if (clash != null) {
            showError(activity.getString(R.string.error_overlap, clash.title,
                    TimeMath.clock(clash.start), TimeMath.clock(clash.end())));
            return;
        }
        callback.onSave(new TimeBox(id, title, start, duration, existing != null && existing.done));
        dialog.dismiss();
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
}
