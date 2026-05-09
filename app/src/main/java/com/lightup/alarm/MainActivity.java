package com.lightup.alarm;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {
    private static final int REQUEST_POST_NOTIFICATIONS = 2301;
    private static final int REQUEST_RINGTONE = 2302;
    private static final long NO_EDITING_ALARM = -1L;

    private final int backgroundColor = Color.rgb(7, 9, 24);
    private final int surfaceColor = Color.rgb(14, 18, 45);
    private final int surfaceRaisedColor = Color.rgb(21, 26, 66);
    private final int textColor = Color.rgb(232, 248, 255);
    private final int mutedColor = Color.rgb(139, 159, 205);
    private final int neonCyan = Color.rgb(54, 244, 255);
    private final int neonMagenta = Color.rgb(255, 43, 214);
    private final int neonAmber = Color.rgb(255, 184, 0);
    private final int dangerColor = Color.rgb(255, 77, 109);

    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private SeekBar thresholdSeekBar;
    private Button[] dayButtons;
    private Button[] snoozeMinuteButtons;
    private Button[] maxSnoozeButtons;
    private Button[] volumeRampButtons;
    private Button[] vibrationButtons;
    private TextView thresholdValueText;
    private TextView currentLightText;
    private TextView calibrationStatusText;
    private TextView sensorStatusText;
    private TextView alarmStatusText;
    private TextView permissionStatusText;
    private TextView formTitleText;
    private TextView selectedRingtoneText;
    private Button exactAlarmSettingsButton;
    private Button fullScreenSettingsButton;
    private Button saveAlarmButton;
    private LinearLayout alarmListLayout;

    private SensorManager sensorManager;
    private Sensor lightSensor;
    private long editingAlarmId = NO_EDITING_ALARM;
    private int selectedRepeatDays = AlarmPreferences.DAYS_ALL;
    private int selectedSnoozeMinutes = AlarmConfig.DEFAULT_SNOOZE_MINUTES;
    private int selectedMaxSnoozes = AlarmConfig.DEFAULT_MAX_SNOOZES;
    private int selectedVolumeRampSeconds = AlarmConfig.DEFAULT_VOLUME_RAMP_SECONDS;
    private int selectedVibrationPattern = AlarmConfig.DEFAULT_VIBRATION_PATTERN;
    private String selectedRingtoneUri;
    private String selectedRingtoneName;
    private float latestLux = -1f;
    private Float darkLuxSample;
    private Float brightLuxSample;
    private MediaPlayer previewPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable stopPreviewRunnable = this::stopPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlarmRingingService.createAlarmChannel(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        setContentView(createContentView());
        loadDefaultForm();
        updateSensorStatus();
        updateAlarmList();
        updateAlarmStatus();
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerLightPreview();
        updateAlarmList();
        updateAlarmStatus();
        updatePermissionStatus();
    }

    @Override
    protected void onPause() {
        unregisterLightPreview();
        stopPreview();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_RINGTONE || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                // Some providers grant temporary read access only; MediaPlayer can still try the URI.
            }
        }

        selectedRingtoneUri = uri.toString();
        selectedRingtoneName = displayNameForUri(uri);
        updateSelectedRingtone();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT || event.values.length == 0) {
            return;
        }

        latestLux = event.values[0];
        currentLightText.setText(String.format(Locale.getDefault(), "%.0f lux", latestLux));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private View createContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(backgroundColor);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.app_icon);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(70), dp(70));
        iconParams.setMarginEnd(dp(14));
        header.addView(icon, iconParams);

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("LIGHT UP", 31, Typeface.BOLD, textColor);
        headerText.addView(title);
        TextView subtitle = text("Cyber alarm with light-sensor dismissal", 14, Typeface.NORMAL, mutedColor);
        subtitle.setPadding(0, dp(4), 0, 0);
        headerText.addView(subtitle);
        header.addView(headerText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        LinearLayout statusPanel = panel(neonCyan);
        statusPanel.addView(sectionTitle("System"));
        alarmStatusText = text("", 16, Typeface.BOLD, textColor);
        statusPanel.addView(alarmStatusText);
        addSpacer(statusPanel, 8);
        sensorStatusText = text("", 14, Typeface.NORMAL, mutedColor);
        statusPanel.addView(sensorStatusText);
        addSpacer(statusPanel, 8);
        permissionStatusText = text("", 14, Typeface.NORMAL, neonAmber);
        statusPanel.addView(permissionStatusText);
        exactAlarmSettingsButton = secondaryButton("Alarms & reminders");
        exactAlarmSettingsButton.setVisibility(View.GONE);
        exactAlarmSettingsButton.setOnClickListener(view -> openExactAlarmSettings());
        statusPanel.addView(exactAlarmSettingsButton, blockParams(12));
        fullScreenSettingsButton = secondaryButton("Alarm display settings");
        fullScreenSettingsButton.setVisibility(View.GONE);
        fullScreenSettingsButton.setOnClickListener(view -> openFullScreenAlarmSettings());
        statusPanel.addView(fullScreenSettingsButton, blockParams(10));
        root.addView(statusPanel, blockParams(18));

        LinearLayout formPanel = panel(neonMagenta);
        LinearLayout formHeader = new LinearLayout(this);
        formHeader.setOrientation(LinearLayout.HORIZONTAL);
        formHeader.setGravity(Gravity.CENTER_VERTICAL);
        formTitleText = sectionTitle("New alarm");
        formHeader.addView(formTitleText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button newButton = miniButton("New", neonCyan);
        newButton.setOnClickListener(view -> loadDefaultForm());
        formHeader.addView(newButton, new LinearLayout.LayoutParams(dp(92), dp(42)));
        formPanel.addView(formHeader);

        formPanel.addView(label("Time"));
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER);
        hourPicker = numberPicker(0, 23);
        minutePicker = numberPicker(0, 59);
        timeRow.addView(hourPicker, new LinearLayout.LayoutParams(0, dp(126), 1));
        TextView separator = text(":", 38, Typeface.BOLD, neonCyan);
        separator.setGravity(Gravity.CENTER);
        timeRow.addView(separator, new LinearLayout.LayoutParams(dp(30), dp(126)));
        timeRow.addView(minutePicker, new LinearLayout.LayoutParams(0, dp(126), 1));
        formPanel.addView(timeRow, blockParams(4));

        formPanel.addView(label("Repeat days"));
        LinearLayout dayRow = new LinearLayout(this);
        dayRow.setOrientation(LinearLayout.HORIZONTAL);
        dayButtons = new Button[AlarmPreferences.DAY_LABELS.length];
        for (int index = 0; index < AlarmPreferences.DAY_LABELS.length; index++) {
            final int dayIndex = index;
            Button dayButton = miniButton(AlarmPreferences.DAY_LABELS[index], neonCyan);
            dayButton.setOnClickListener(view -> toggleDay(dayIndex));
            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(0, dp(42), 1);
            if (index > 0) {
                dayParams.setMarginStart(dp(4));
            }
            dayRow.addView(dayButton, dayParams);
            dayButtons[index] = dayButton;
        }
        formPanel.addView(dayRow, blockParams(4));

        formPanel.addView(label("Ringtone"));
        LinearLayout ringtoneRow = new LinearLayout(this);
        ringtoneRow.setOrientation(LinearLayout.HORIZONTAL);
        ringtoneRow.setGravity(Gravity.CENTER_VERTICAL);
        selectedRingtoneText = text(AlarmConfig.DEFAULT_RINGTONE_LABEL, 14, Typeface.BOLD, textColor);
        ringtoneRow.addView(selectedRingtoneText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button previewRingtoneButton = miniButton("Play", neonCyan);
        previewRingtoneButton.setOnClickListener(view -> previewRingtone());
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(80), dp(42));
        previewParams.setMarginEnd(dp(8));
        ringtoneRow.addView(previewRingtoneButton, previewParams);
        Button chooseRingtoneButton = miniButton("Audio", neonAmber);
        chooseRingtoneButton.setOnClickListener(view -> chooseRingtone());
        ringtoneRow.addView(chooseRingtoneButton, new LinearLayout.LayoutParams(dp(96), dp(42)));
        formPanel.addView(ringtoneRow, blockParams(4));

        formPanel.addView(label("Snooze duration"));
        snoozeMinuteButtons = new Button[AlarmConfig.SNOOZE_MINUTE_OPTIONS.length];
        formPanel.addView(optionRow(
                snoozeMinuteButtons,
                AlarmConfig.SNOOZE_MINUTE_OPTIONS,
                new String[]{"Off", "5m", "10m", "15m"},
                value -> {
                    selectedSnoozeMinutes = value;
                    if (selectedSnoozeMinutes == 0) {
                        selectedMaxSnoozes = 0;
                    } else if (selectedMaxSnoozes == 0) {
                        selectedMaxSnoozes = AlarmConfig.DEFAULT_MAX_SNOOZES;
                    }
                    updateWakeOptionButtons();
                }
        ), blockParams(4));

        formPanel.addView(label("Snooze limit"));
        maxSnoozeButtons = new Button[AlarmConfig.MAX_SNOOZE_OPTIONS.length];
        formPanel.addView(optionRow(
                maxSnoozeButtons,
                AlarmConfig.MAX_SNOOZE_OPTIONS,
                new String[]{"Off", "1", "2", "3"},
                value -> {
                    selectedMaxSnoozes = value;
                    if (selectedMaxSnoozes == 0) {
                        selectedSnoozeMinutes = 0;
                    } else if (selectedSnoozeMinutes == 0) {
                        selectedSnoozeMinutes = AlarmConfig.DEFAULT_SNOOZE_MINUTES;
                    }
                    updateWakeOptionButtons();
                }
        ), blockParams(4));

        formPanel.addView(label("Volume ramp"));
        volumeRampButtons = new Button[AlarmConfig.VOLUME_RAMP_OPTIONS.length];
        formPanel.addView(optionRow(
                volumeRampButtons,
                AlarmConfig.VOLUME_RAMP_OPTIONS,
                new String[]{"Off", "15s", "30s", "60s"},
                value -> {
                    selectedVolumeRampSeconds = value;
                    updateWakeOptionButtons();
                }
        ), blockParams(4));

        formPanel.addView(label("Vibration"));
        vibrationButtons = new Button[AlarmConfig.VIBRATION_PATTERN_OPTIONS.length];
        formPanel.addView(optionRow(
                vibrationButtons,
                AlarmConfig.VIBRATION_PATTERN_OPTIONS,
                new String[]{"Off", "Pulse", "Steady", "Urgent"},
                value -> {
                    selectedVibrationPattern = value;
                    updateWakeOptionButtons();
                }
        ), blockParams(4));

        formPanel.addView(label("Light target"));
        thresholdValueText = text("", 28, Typeface.BOLD, neonCyan);
        thresholdValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        formPanel.addView(thresholdValueText, blockParams(2));

        thresholdSeekBar = new SeekBar(this);
        thresholdSeekBar.setMax((AlarmPreferences.MAX_THRESHOLD_LUX - AlarmPreferences.MIN_THRESHOLD_LUX)
                / AlarmPreferences.THRESHOLD_STEP_LUX);
        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateThresholdLabel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        formPanel.addView(thresholdSeekBar, blockParams(6));
        formPanel.addView(text("Range: 0-500 lux", 13, Typeface.NORMAL, mutedColor));

        formPanel.addView(label("Calibration"));
        LinearLayout calibrationRow = new LinearLayout(this);
        calibrationRow.setOrientation(LinearLayout.HORIZONTAL);
        Button darkButton = miniButton("Dark", neonMagenta);
        darkButton.setOnClickListener(view -> sampleDarkRoom());
        Button brightButton = miniButton("Lights", neonAmber);
        brightButton.setOnClickListener(view -> sampleBrightRoom());
        Button applyButton = miniButton("Apply", neonCyan);
        applyButton.setOnClickListener(view -> applyCalibration());
        LinearLayout.LayoutParams calibrationButtonParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        calibrationButtonParams.setMarginEnd(dp(6));
        calibrationRow.addView(darkButton, calibrationButtonParams);
        LinearLayout.LayoutParams brightParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        brightParams.setMarginStart(dp(3));
        brightParams.setMarginEnd(dp(3));
        calibrationRow.addView(brightButton, brightParams);
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        applyParams.setMarginStart(dp(6));
        calibrationRow.addView(applyButton, applyParams);
        formPanel.addView(calibrationRow, blockParams(4));
        calibrationStatusText = text("Sample dark room and lights-on readings.", 13, Typeface.NORMAL, mutedColor);
        formPanel.addView(calibrationStatusText, blockParams(6));

        currentLightText = text("-- lux", 22, Typeface.BOLD, neonAmber);
        currentLightText.setGravity(Gravity.CENTER_HORIZONTAL);
        currentLightText.setPadding(0, dp(12), 0, 0);
        formPanel.addView(currentLightText);
        TextView currentLabel = text("Current sensor reading", 13, Typeface.NORMAL, mutedColor);
        currentLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        formPanel.addView(currentLabel);

        saveAlarmButton = primaryButton("Add alarm");
        saveAlarmButton.setOnClickListener(view -> saveFormAlarm());
        formPanel.addView(saveAlarmButton, blockParams(16));
        root.addView(formPanel, blockParams(16));

        LinearLayout alarmsPanel = panel(neonCyan);
        alarmsPanel.addView(sectionTitle("Alarms"));
        alarmListLayout = new LinearLayout(this);
        alarmListLayout.setOrientation(LinearLayout.VERTICAL);
        alarmsPanel.addView(alarmListLayout);
        root.addView(alarmsPanel, blockParams(16));

        return scrollView;
    }

    private void loadDefaultForm() {
        editingAlarmId = NO_EDITING_ALARM;
        formTitleText.setText(R.string.form_new_alarm);
        updateSaveButtonText();
        hourPicker.setValue(AlarmPreferences.DEFAULT_HOUR);
        minutePicker.setValue(AlarmPreferences.DEFAULT_MINUTE);
        selectedRepeatDays = AlarmPreferences.DAYS_ALL;
        updateDayButtons();
        selectedSnoozeMinutes = AlarmConfig.DEFAULT_SNOOZE_MINUTES;
        selectedMaxSnoozes = AlarmConfig.DEFAULT_MAX_SNOOZES;
        selectedVolumeRampSeconds = AlarmConfig.DEFAULT_VOLUME_RAMP_SECONDS;
        selectedVibrationPattern = AlarmConfig.DEFAULT_VIBRATION_PATTERN;
        updateWakeOptionButtons();
        selectedRingtoneUri = null;
        selectedRingtoneName = null;
        updateSelectedRingtone();
        setThresholdLux(AlarmPreferences.DEFAULT_THRESHOLD_LUX);
    }

    private void loadAlarmIntoForm(AlarmConfig alarm) {
        editingAlarmId = alarm.id;
        formTitleText.setText(R.string.form_edit_alarm);
        updateSaveButtonText();
        hourPicker.setValue(alarm.hour);
        minutePicker.setValue(alarm.minute);
        selectedRepeatDays = alarm.repeatDays;
        updateDayButtons();
        selectedSnoozeMinutes = alarm.snoozeMinutes;
        selectedMaxSnoozes = alarm.maxSnoozes;
        selectedVolumeRampSeconds = alarm.volumeRampSeconds;
        selectedVibrationPattern = alarm.vibrationPattern;
        updateWakeOptionButtons();
        selectedRingtoneUri = alarm.ringtoneUri;
        selectedRingtoneName = alarm.ringtoneName;
        updateSelectedRingtone();
        setThresholdLux(alarm.thresholdLux);
    }

    private void saveFormAlarm() {
        boolean editingExistingAlarm = editingAlarmId != NO_EDITING_ALARM;
        AlarmConfig alarm = new AlarmConfig(
                editingExistingAlarm ? editingAlarmId : AlarmStore.newAlarmId(this),
                hourPicker.getValue(),
                minutePicker.getValue(),
                true,
                selectedRepeatDays,
                0L,
                currentThresholdLux(),
                selectedRingtoneUri,
                selectedRingtoneName,
                selectedSnoozeMinutes,
                selectedMaxSnoozes,
                0,
                0L,
                selectedVolumeRampSeconds,
                selectedVibrationPattern
        );

        if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            alarm.enabled = false;
            AlarmStore.saveAlarm(this, alarm);
            updateAlarmList();
            updateAlarmStatus();
            updatePermissionStatus();
            if (editingExistingAlarm) {
                loadAlarmIntoForm(alarm);
            } else {
                switchToNewAlarmMode();
            }
            Toast.makeText(this, "Allow Alarms & reminders, then enable the alarm.", Toast.LENGTH_LONG).show();
            openExactAlarmSettings();
            return;
        }

        if (!AlarmScheduler.schedule(this, alarm)) {
            alarm.enabled = false;
            AlarmStore.saveAlarm(this, alarm);
            Toast.makeText(this, "Alarm saved, but scheduling failed.", Toast.LENGTH_LONG).show();
        } else {
            AlarmStore.saveAlarm(this, alarm);
            Toast.makeText(this, "Alarm set for " + AlarmPreferences.formatDateTime(this, alarm.nextTriggerMillis()), Toast.LENGTH_LONG).show();
        }

        if (editingExistingAlarm) {
            loadAlarmIntoForm(alarm);
        } else {
            switchToNewAlarmMode();
        }
        updateAlarmList();
        updateAlarmStatus();
    }

    private void switchToNewAlarmMode() {
        editingAlarmId = NO_EDITING_ALARM;
        formTitleText.setText(R.string.form_new_alarm);
        updateSaveButtonText();
    }

    private void updateSaveButtonText() {
        if (saveAlarmButton == null) {
            return;
        }
        saveAlarmButton.setText(editingAlarmId == NO_EDITING_ALARM ? "Add alarm" : "Save changes");
    }

    private void updateAlarmList() {
        alarmListLayout.removeAllViews();
        List<AlarmConfig> alarms = AlarmStore.getAlarms(this);
        if (alarms.isEmpty()) {
            TextView emptyText = text("No alarms yet", 15, Typeface.NORMAL, mutedColor);
            emptyText.setPadding(0, dp(8), 0, 0);
            alarmListLayout.addView(emptyText);
            return;
        }

        for (AlarmConfig alarm : alarms) {
            alarmListLayout.addView(alarmCard(alarm), blockParams(10));
        }
    }

    private View alarmCard(AlarmConfig alarm) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(strokedBackground(surfaceRaisedColor, alarm.enabled ? neonCyan : mutedColor, 8, 1));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView timeText = text(AlarmPreferences.formatAlarmTime(this, alarm.hour, alarm.minute), 30, Typeface.BOLD, textColor);
        topRow.addView(timeText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch enabledSwitch = new Switch(this);
        enabledSwitch.setChecked(alarm.enabled);
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            alarm.enabled = isChecked;
            if (isChecked) {
                if (!AlarmScheduler.canScheduleExactAlarms(this)) {
                    alarm.enabled = false;
                    AlarmStore.saveAlarm(this, alarm);
                    buttonView.setChecked(false);
                    updatePermissionStatus();
                    Toast.makeText(this, "Allow Alarms & reminders, then enable the alarm.", Toast.LENGTH_LONG).show();
                    openExactAlarmSettings();
                    return;
                }
                if (!AlarmScheduler.schedule(this, alarm)) {
                    alarm.enabled = false;
                    AlarmStore.saveAlarm(this, alarm);
                    buttonView.setChecked(false);
                    Toast.makeText(this, "Could not schedule alarm.", Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                AlarmScheduler.cancel(this, alarm.id);
            }
            AlarmStore.saveAlarm(this, alarm);
            updateAlarmStatus();
            updateAlarmList();
        });
        topRow.addView(enabledSwitch);
        card.addView(topRow);

        String repeat = alarm.repeatLabel();
        String next = alarm.enabled ? AlarmPreferences.formatDateTime(this, alarm.nextTriggerMillis()) : "Off";
        card.addView(text(repeat + " / Next: " + next, 13, Typeface.NORMAL, mutedColor), blockParams(6));
        if (alarm.skipNextTriggerMillis > 0L) {
            card.addView(text("Skipped: " + AlarmPreferences.formatDateTime(this, alarm.skipNextTriggerMillis), 13, Typeface.NORMAL, neonAmber), blockParams(4));
        }
        if (alarm.snoozeUntilMillis > 0L) {
            card.addView(text("Snoozed until: " + AlarmPreferences.formatDateTime(this, alarm.snoozeUntilMillis), 13, Typeface.NORMAL, neonAmber), blockParams(4));
        }
        card.addView(text("Light target: " + alarm.thresholdLux + " lux", 13, Typeface.NORMAL, neonCyan), blockParams(4));
        card.addView(text("Tone: " + alarm.ringtoneLabel(), 13, Typeface.NORMAL, mutedColor), blockParams(4));
        card.addView(text("Snooze: " + alarm.snoozeLabel(), 13, Typeface.NORMAL, mutedColor), blockParams(4));
        card.addView(text("Ramp: " + alarm.volumeRampLabel() + " / Vibration: " + alarm.vibrationLabel(), 13, Typeface.NORMAL, mutedColor), blockParams(4));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button editButton = miniButton("Edit", neonAmber);
        editButton.setOnClickListener(view -> loadAlarmIntoForm(alarm));
        Button skipButton = miniButton("Skip", neonCyan);
        skipButton.setEnabled(alarm.enabled);
        skipButton.setAlpha(alarm.enabled ? 1.0f : 0.45f);
        skipButton.setOnClickListener(view -> skipNextAlarm(alarm));
        Button deleteButton = miniButton("Delete", dangerColor);
        deleteButton.setOnClickListener(view -> deleteAlarm(alarm));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        editParams.setMarginEnd(dp(6));
        actions.addView(editButton, editParams);
        LinearLayout.LayoutParams skipParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        skipParams.setMarginStart(dp(3));
        skipParams.setMarginEnd(dp(3));
        actions.addView(skipButton, skipParams);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        deleteParams.setMarginStart(dp(6));
        actions.addView(deleteButton, deleteParams);
        card.addView(actions, blockParams(12));

        return card;
    }

    private void deleteAlarm(AlarmConfig alarm) {
        AlarmScheduler.cancel(this, alarm.id);
        AlarmStore.deleteAlarm(this, alarm.id);
        if (editingAlarmId == alarm.id) {
            loadDefaultForm();
        }
        updateAlarmList();
        updateAlarmStatus();
        Toast.makeText(this, "Alarm deleted", Toast.LENGTH_SHORT).show();
    }

    private void skipNextAlarm(AlarmConfig alarm) {
        if (!alarm.enabled) {
            return;
        }

        long skippedTrigger = AlarmPreferences.nextTriggerMillis(alarm.hour, alarm.minute, alarm.repeatDays, 0L, System.currentTimeMillis());
        if (!alarm.isRepeating()) {
            AlarmScheduler.cancel(this, alarm.id);
            alarm.enabled = false;
            alarm.skipNextTriggerMillis = 0L;
            AlarmStore.saveAlarm(this, alarm);
            updateAlarmList();
            updateAlarmStatus();
            Toast.makeText(this, "One-time alarm skipped and turned off.", Toast.LENGTH_LONG).show();
            return;
        }

        alarm.skipNextTriggerMillis = skippedTrigger;
        long nextTrigger = alarm.nextTriggerMillis();
        if (AlarmScheduler.schedule(this, alarm, nextTrigger)) {
            AlarmStore.saveAlarm(this, alarm);
            updateAlarmList();
            updateAlarmStatus();
            Toast.makeText(this, "Skipped " + AlarmPreferences.formatDateTime(this, skippedTrigger), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Could not reschedule alarm.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateAlarmStatus() {
        List<AlarmConfig> alarms = AlarmStore.getAlarms(this);
        int enabledCount = 0;
        for (AlarmConfig alarm : alarms) {
            if (alarm.enabled) {
                enabledCount++;
            }
        }
        alarmStatusText.setText(getResources().getQuantityString(
                R.plurals.alarm_count_status,
                alarms.size(),
                alarms.size(),
                enabledCount
        ));
    }

    private void toggleDay(int dayIndex) {
        int dayBit = 1 << dayIndex;
        selectedRepeatDays ^= dayBit;
        selectedRepeatDays &= AlarmPreferences.DAYS_ALL;
        updateDayButtons();
    }

    private void updateDayButtons() {
        if (dayButtons == null) {
            return;
        }

        for (int index = 0; index < dayButtons.length; index++) {
            boolean selected = (selectedRepeatDays & (1 << index)) != 0;
            dayButtons[index].setTextColor(selected ? backgroundColor : textColor);
            dayButtons[index].setBackground(strokedBackground(
                    selected ? neonCyan : Color.TRANSPARENT,
                    selected ? neonMagenta : neonCyan,
                    8,
                    1
            ));
        }
    }

    private void updateWakeOptionButtons() {
        updateOptionButtons(snoozeMinuteButtons, AlarmConfig.SNOOZE_MINUTE_OPTIONS, selectedSnoozeMinutes);
        updateOptionButtons(maxSnoozeButtons, AlarmConfig.MAX_SNOOZE_OPTIONS, selectedMaxSnoozes);
        updateOptionButtons(volumeRampButtons, AlarmConfig.VOLUME_RAMP_OPTIONS, selectedVolumeRampSeconds);
        updateOptionButtons(vibrationButtons, AlarmConfig.VIBRATION_PATTERN_OPTIONS, selectedVibrationPattern);
    }

    private LinearLayout optionRow(Button[] buttons, int[] values, String[] labels, OptionSelectListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int index = 0; index < buttons.length; index++) {
            final int value = values[index];
            Button button = miniButton(labels[index], neonCyan);
            button.setOnClickListener(view -> listener.onSelected(value));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1);
            if (index > 0) {
                params.setMarginStart(dp(4));
            }
            row.addView(button, params);
            buttons[index] = button;
        }
        return row;
    }

    private void updateOptionButtons(Button[] buttons, int[] values, int selectedValue) {
        if (buttons == null) {
            return;
        }

        for (int index = 0; index < buttons.length; index++) {
            boolean selected = values[index] == selectedValue;
            buttons[index].setTextColor(selected ? backgroundColor : textColor);
            buttons[index].setBackground(strokedBackground(
                    selected ? neonCyan : Color.TRANSPARENT,
                    selected ? neonMagenta : neonCyan,
                    8,
                    1
            ));
        }
    }

    private void sampleDarkRoom() {
        if (!hasLiveLux()) {
            Toast.makeText(this, "Waiting for a light sensor reading.", Toast.LENGTH_SHORT).show();
            return;
        }
        darkLuxSample = latestLux;
        updateCalibrationStatus();
    }

    private void sampleBrightRoom() {
        if (!hasLiveLux()) {
            Toast.makeText(this, "Waiting for a light sensor reading.", Toast.LENGTH_SHORT).show();
            return;
        }
        brightLuxSample = latestLux;
        updateCalibrationStatus();
    }

    private void applyCalibration() {
        if (darkLuxSample == null || brightLuxSample == null) {
            Toast.makeText(this, "Sample both dark and lights-on readings first.", Toast.LENGTH_SHORT).show();
            return;
        }

        float low = Math.min(darkLuxSample, brightLuxSample);
        float high = Math.max(darkLuxSample, brightLuxSample);
        int recommended = AlarmPreferences.clampThreshold(Math.round(low + (high - low) * 0.55f));
        setThresholdLux(recommended);
        calibrationStatusText.setText(String.format(Locale.getDefault(), "Recommended target applied: %d lux", recommended));
    }

    private void updateCalibrationStatus() {
        String dark = darkLuxSample == null ? "--" : String.format(Locale.getDefault(), "%.0f", darkLuxSample);
        String bright = brightLuxSample == null ? "--" : String.format(Locale.getDefault(), "%.0f", brightLuxSample);
        calibrationStatusText.setText("Dark: " + dark + " lux / Lights: " + bright + " lux");
    }

    private boolean hasLiveLux() {
        return latestLux >= 0f;
    }

    private void updateSensorStatus() {
        if (lightSensor == null) {
            sensorStatusText.setText(R.string.sensor_missing);
            currentLightText.setText(R.string.sensor_unavailable);
        } else {
            sensorStatusText.setText(getString(R.string.sensor_name, lightSensor.getName()));
        }
    }

    private void updatePermissionStatus() {
        String notificationText = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationText = "Notifications are off, so alarms may not appear over the lock screen.";
        }

        boolean needsExactAlarmSettings = !AlarmScheduler.canScheduleExactAlarms(this);
        if (needsExactAlarmSettings) {
            notificationText = appendStatus(notificationText, "Alarms & reminders permission is off.");
        }

        boolean needsFullScreenSettings = false;
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            needsFullScreenSettings = notificationManager != null
                    && !notificationManager.canUseFullScreenIntent();
        }
        if (needsFullScreenSettings) {
            notificationText = appendStatus(notificationText, "Full-screen alarm display is disabled.");
        }

        permissionStatusText.setText(notificationText);
        permissionStatusText.setVisibility(notificationText.isEmpty() ? View.GONE : View.VISIBLE);
        exactAlarmSettingsButton.setVisibility(needsExactAlarmSettings ? View.VISIBLE : View.GONE);
        fullScreenSettingsButton.setVisibility(needsFullScreenSettings ? View.VISIBLE : View.GONE);
    }

    private void updateThresholdLabel() {
        if (thresholdValueText == null) {
            return;
        }

        int threshold = currentThresholdLux();
        thresholdValueText.setText(getResources().getQuantityString(
                R.plurals.threshold_lux,
                threshold,
                threshold
        ));
    }

    private void setThresholdLux(int thresholdLux) {
        int progress = (AlarmPreferences.clampThreshold(thresholdLux) - AlarmPreferences.MIN_THRESHOLD_LUX)
                / AlarmPreferences.THRESHOLD_STEP_LUX;
        thresholdSeekBar.setProgress(Math.max(0, Math.min(thresholdSeekBar.getMax(), progress)));
        updateThresholdLabel();
    }

    private int currentThresholdLux() {
        return AlarmPreferences.MIN_THRESHOLD_LUX
                + thresholdSeekBar.getProgress() * AlarmPreferences.THRESHOLD_STEP_LUX;
    }

    private void chooseRingtone() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_RINGTONE);
    }

    private void previewRingtone() {
        Uri previewUri = selectedRingtoneUri == null
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                : Uri.parse(selectedRingtoneUri);
        if (previewUri == null) {
            previewUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
        if (previewUri == null) {
            Toast.makeText(this, "No preview sound available.", Toast.LENGTH_SHORT).show();
            return;
        }

        stopPreview();
        try {
            previewPlayer = new MediaPlayer();
            previewPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            previewPlayer.setDataSource(this, previewUri);
            previewPlayer.setLooping(false);
            previewPlayer.prepare();
            previewPlayer.start();
            handler.postDelayed(stopPreviewRunnable, 12_000L);
        } catch (IOException | RuntimeException exception) {
            stopPreview();
            Toast.makeText(this, "Could not preview this audio file.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopPreview() {
        handler.removeCallbacks(stopPreviewRunnable);
        if (previewPlayer == null) {
            return;
        }
        try {
            if (previewPlayer.isPlaying()) {
                previewPlayer.stop();
            }
        } catch (IllegalStateException ignored) {
        }
        previewPlayer.release();
        previewPlayer = null;
    }

    private String displayNameForUri(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    String name = cursor.getString(nameIndex);
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }

        String fallback = uri.getLastPathSegment();
        return fallback == null || fallback.trim().isEmpty() ? "Custom audio" : fallback;
    }

    private void updateSelectedRingtone() {
        selectedRingtoneText.setText(selectedRingtoneName == null
                ? AlarmConfig.DEFAULT_RINGTONE_LABEL
                : selectedRingtoneName);
    }

    private void registerLightPreview() {
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void unregisterLightPreview() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
        }
    }

    private void openFullScreenAlarmSettings() {
        if (Build.VERSION.SDK_INT < 34) {
            return;
        }

        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }

        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private NumberPicker numberPicker(int min, int max) {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        String[] values = new String[max - min + 1];
        for (int index = 0; index < values.length; index++) {
            values[index] = String.format(Locale.getDefault(), "%02d", min + index);
        }
        picker.setDisplayedValues(values);
        picker.setWrapSelectorWheel(true);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        picker.setBackground(strokedBackground(Color.TRANSPARENT, neonCyan, 8, 1));
        for (int index = 0; index < picker.getChildCount(); index++) {
            View child = picker.getChildAt(index);
            if (child instanceof EditText) {
                EditText editText = (EditText) child;
                editText.setTextColor(textColor);
                editText.setTextSize(26);
                editText.setGravity(Gravity.CENTER);
                editText.setBackgroundColor(Color.TRANSPARENT);
            }
        }
        return picker;
    }

    private LinearLayout panel(int strokeColor) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(strokedBackground(surfaceColor, strokeColor, 8, 1));
        return panel;
    }

    private TextView sectionTitle(String value) {
        TextView textView = text(value, 18, Typeface.BOLD, textColor);
        textView.setPadding(0, 0, 0, dp(8));
        return textView;
    }

    private TextView label(String value) {
        TextView textView = text(value, 13, Typeface.BOLD, neonMagenta);
        textView.setPadding(0, dp(12), 0, 0);
        return textView;
    }

    private TextView text(String value, int sp, int style, int color) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setTextColor(color);
        textView.setLineSpacing(dp(2), 1.0f);
        return textView;
    }

    private Button primaryButton(String value) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(value);
        button.setTextColor(backgroundColor);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(strokedBackground(neonCyan, neonMagenta, 8, 1));
        return button;
    }

    private Button secondaryButton(String value) {
        return miniButton(value, neonCyan);
    }

    private Button miniButton(String value, int strokeColor) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(value);
        button.setTextColor(textColor);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(strokedBackground(Color.TRANSPARENT, strokeColor, 8, 1));
        return button;
    }

    private GradientDrawable strokedBackground(int fillColor, int strokeColor, int radiusDp, int strokeDp) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fillColor);
        background.setCornerRadius(dp(radiusDp));
        background.setStroke(dp(strokeDp), strokeColor);
        return background;
    }

    private LinearLayout.LayoutParams blockParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMarginDp);
        return params;
    }

    private void addSpacer(LinearLayout parent, int heightDp) {
        Space space = new Space(this);
        parent.addView(space, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private String appendStatus(String current, String addition) {
        return current.isEmpty() ? addition : current + " " + addition;
    }

    private interface OptionSelectListener {
        void onSelected(int value);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
