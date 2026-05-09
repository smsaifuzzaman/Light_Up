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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
    private Switch repeatSwitch;
    private TextView thresholdValueText;
    private TextView currentLightText;
    private TextView sensorStatusText;
    private TextView alarmStatusText;
    private TextView permissionStatusText;
    private TextView formTitleText;
    private TextView selectedRingtoneText;
    private Button exactAlarmSettingsButton;
    private Button fullScreenSettingsButton;
    private LinearLayout alarmListLayout;

    private SensorManager sensorManager;
    private Sensor lightSensor;
    private long editingAlarmId = NO_EDITING_ALARM;
    private String selectedRingtoneUri;
    private String selectedRingtoneName;

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

        currentLightText.setText(String.format(Locale.getDefault(), "%.0f lux", event.values[0]));
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

        repeatSwitch = new Switch(this);
        repeatSwitch.setText(R.string.repeat_every_day);
        repeatSwitch.setTextColor(textColor);
        repeatSwitch.setTextSize(15);
        repeatSwitch.setPadding(0, dp(8), 0, 0);
        formPanel.addView(repeatSwitch);

        formPanel.addView(label("Ringtone"));
        LinearLayout ringtoneRow = new LinearLayout(this);
        ringtoneRow.setOrientation(LinearLayout.HORIZONTAL);
        ringtoneRow.setGravity(Gravity.CENTER_VERTICAL);
        selectedRingtoneText = text(AlarmConfig.DEFAULT_RINGTONE_LABEL, 14, Typeface.BOLD, textColor);
        ringtoneRow.addView(selectedRingtoneText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button chooseRingtoneButton = miniButton("Audio", neonAmber);
        chooseRingtoneButton.setOnClickListener(view -> chooseRingtone());
        ringtoneRow.addView(chooseRingtoneButton, new LinearLayout.LayoutParams(dp(96), dp(42)));
        formPanel.addView(ringtoneRow, blockParams(4));

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

        currentLightText = text("-- lux", 22, Typeface.BOLD, neonAmber);
        currentLightText.setGravity(Gravity.CENTER_HORIZONTAL);
        currentLightText.setPadding(0, dp(12), 0, 0);
        formPanel.addView(currentLightText);
        TextView currentLabel = text("Current sensor reading", 13, Typeface.NORMAL, mutedColor);
        currentLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        formPanel.addView(currentLabel);

        Button saveButton = primaryButton("Save alarm");
        saveButton.setOnClickListener(view -> saveFormAlarm());
        formPanel.addView(saveButton, blockParams(16));
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
        hourPicker.setValue(AlarmPreferences.DEFAULT_HOUR);
        minutePicker.setValue(AlarmPreferences.DEFAULT_MINUTE);
        repeatSwitch.setChecked(true);
        selectedRingtoneUri = null;
        selectedRingtoneName = null;
        updateSelectedRingtone();
        setThresholdLux(AlarmPreferences.DEFAULT_THRESHOLD_LUX);
    }

    private void loadAlarmIntoForm(AlarmConfig alarm) {
        editingAlarmId = alarm.id;
        formTitleText.setText(R.string.form_edit_alarm);
        hourPicker.setValue(alarm.hour);
        minutePicker.setValue(alarm.minute);
        repeatSwitch.setChecked(alarm.repeatDaily);
        selectedRingtoneUri = alarm.ringtoneUri;
        selectedRingtoneName = alarm.ringtoneName;
        updateSelectedRingtone();
        setThresholdLux(alarm.thresholdLux);
    }

    private void saveFormAlarm() {
        AlarmConfig alarm = new AlarmConfig(
                editingAlarmId == NO_EDITING_ALARM ? AlarmStore.newAlarmId(this) : editingAlarmId,
                hourPicker.getValue(),
                minutePicker.getValue(),
                true,
                repeatSwitch.isChecked(),
                currentThresholdLux(),
                selectedRingtoneUri,
                selectedRingtoneName
        );

        if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            alarm.enabled = false;
            AlarmStore.saveAlarm(this, alarm);
            updateAlarmList();
            updateAlarmStatus();
            updatePermissionStatus();
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

        loadAlarmIntoForm(alarm);
        updateAlarmList();
        updateAlarmStatus();
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

        String repeat = getString(alarm.repeatDaily ? R.string.repeat_daily : R.string.repeat_once);
        String next = alarm.enabled ? AlarmPreferences.formatDateTime(this, alarm.nextTriggerMillis()) : "Off";
        card.addView(text(repeat + " / Next: " + next, 13, Typeface.NORMAL, mutedColor), blockParams(6));
        card.addView(text("Light target: " + alarm.thresholdLux + " lux", 13, Typeface.NORMAL, neonCyan), blockParams(4));
        card.addView(text("Tone: " + alarm.ringtoneLabel(), 13, Typeface.NORMAL, mutedColor), blockParams(4));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button editButton = miniButton("Edit", neonAmber);
        editButton.setOnClickListener(view -> loadAlarmIntoForm(alarm));
        Button deleteButton = miniButton("Delete", dangerColor);
        deleteButton.setOnClickListener(view -> deleteAlarm(alarm));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        editParams.setMarginEnd(dp(8));
        actions.addView(editButton, editParams);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        deleteParams.setMarginStart(dp(8));
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
