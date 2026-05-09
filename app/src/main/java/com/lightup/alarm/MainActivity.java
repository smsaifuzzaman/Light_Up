package com.lightup.alarm;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {
    private static final int REQUEST_POST_NOTIFICATIONS = 2301;

    private final int backgroundColor = Color.rgb(247, 250, 252);
    private final int surfaceColor = Color.WHITE;
    private final int textColor = Color.rgb(23, 32, 51);
    private final int mutedColor = Color.rgb(95, 111, 134);
    private final int primaryColor = Color.rgb(245, 182, 66);

    private TimePicker timePicker;
    private SeekBar thresholdSeekBar;
    private Switch repeatSwitch;
    private TextView thresholdValueText;
    private TextView currentLightText;
    private TextView sensorStatusText;
    private TextView alarmStatusText;
    private TextView permissionStatusText;
    private Button exactAlarmSettingsButton;
    private Button fullScreenSettingsButton;

    private SensorManager sensorManager;
    private Sensor lightSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlarmRingingService.createAlarmChannel(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        setContentView(createContentView());
        loadSavedValues();
        updateThresholdLabel();
        updateSensorStatus();
        updateAlarmStatus();
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerLightPreview();
        updateAlarmStatus();
        updatePermissionStatus();
    }

    @Override
    protected void onPause() {
        unregisterLightPreview();
        super.onPause();
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
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text("Light Up", 34, Typeface.BOLD, textColor);
        root.addView(title);

        TextView subtitle = text("Alarm clock with light-sensor dismissal", 16, Typeface.NORMAL, mutedColor);
        subtitle.setPadding(0, dp(6), 0, dp(18));
        root.addView(subtitle);

        LinearLayout statusPanel = panel();
        alarmStatusText = text("", 17, Typeface.BOLD, textColor);
        statusPanel.addView(alarmStatusText);
        addSpacer(statusPanel, 8);
        sensorStatusText = text("", 14, Typeface.NORMAL, mutedColor);
        statusPanel.addView(sensorStatusText);
        addSpacer(statusPanel, 8);
        permissionStatusText = text("", 14, Typeface.NORMAL, mutedColor);
        statusPanel.addView(permissionStatusText);
        exactAlarmSettingsButton = secondaryButton("Alarms & reminders settings");
        exactAlarmSettingsButton.setVisibility(View.GONE);
        exactAlarmSettingsButton.setOnClickListener(view -> openExactAlarmSettings());
        statusPanel.addView(exactAlarmSettingsButton, blockParams(12));
        fullScreenSettingsButton = secondaryButton("Alarm display settings");
        fullScreenSettingsButton.setVisibility(View.GONE);
        fullScreenSettingsButton.setOnClickListener(view -> openFullScreenAlarmSettings());
        statusPanel.addView(fullScreenSettingsButton, blockParams(12));
        root.addView(statusPanel, blockParams(0));

        LinearLayout timePanel = panel();
        timePanel.addView(sectionTitle("Alarm time"));
        timePicker = new TimePicker(this);
        timePicker.setIs24HourView(DateFormat.is24HourFormat(this));
        timePanel.addView(timePicker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        repeatSwitch = new Switch(this);
        repeatSwitch.setText(R.string.repeat_every_day);
        repeatSwitch.setTextColor(textColor);
        repeatSwitch.setTextSize(16);
        repeatSwitch.setPadding(0, dp(10), 0, 0);
        timePanel.addView(repeatSwitch);
        root.addView(timePanel, blockParams(14));

        LinearLayout lightPanel = panel();
        lightPanel.addView(sectionTitle("Light target"));
        thresholdValueText = text("", 28, Typeface.BOLD, textColor);
        thresholdValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        thresholdValueText.setPadding(0, dp(8), 0, dp(4));
        lightPanel.addView(thresholdValueText);

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
        lightPanel.addView(thresholdSeekBar, blockParams(6));

        TextView hint = text("Default: 180 lux. Bright rooms are often 300 lux or higher.", 13, Typeface.NORMAL, mutedColor);
        lightPanel.addView(hint, blockParams(4));

        currentLightText = text("-- lux", 22, Typeface.BOLD, primaryColor);
        currentLightText.setGravity(Gravity.CENTER_HORIZONTAL);
        currentLightText.setPadding(0, dp(14), 0, 0);
        lightPanel.addView(currentLightText);

        TextView currentLabel = text("Current sensor reading", 13, Typeface.NORMAL, mutedColor);
        currentLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        lightPanel.addView(currentLabel);
        root.addView(lightPanel, blockParams(14));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        Button setButton = primaryButton("Set alarm");
        setButton.setOnClickListener(view -> setAlarm());
        Button cancelButton = secondaryButton("Cancel");
        cancelButton.setOnClickListener(view -> cancelAlarm());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        buttonParams.setMarginEnd(dp(8));
        buttons.addView(setButton, buttonParams);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        cancelParams.setMarginStart(dp(8));
        buttons.addView(cancelButton, cancelParams);
        root.addView(buttons, blockParams(18));

        return scrollView;
    }

    private void loadSavedValues() {
        timePicker.setHour(AlarmPreferences.getHour(this));
        timePicker.setMinute(AlarmPreferences.getMinute(this));
        repeatSwitch.setChecked(AlarmPreferences.repeatsDaily(this));

        int progress = (AlarmPreferences.getThresholdLux(this) - AlarmPreferences.MIN_THRESHOLD_LUX)
                / AlarmPreferences.THRESHOLD_STEP_LUX;
        thresholdSeekBar.setProgress(Math.max(0, Math.min(thresholdSeekBar.getMax(), progress)));
    }

    private void setAlarm() {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        int threshold = currentThresholdLux();
        boolean repeatDaily = repeatSwitch.isChecked();

        AlarmPreferences.saveAlarm(this, hour, minute, threshold, repeatDaily);
        long triggerAtMillis = AlarmPreferences.nextTriggerMillis(this);
        boolean scheduled = AlarmScheduler.schedule(this, triggerAtMillis);
        if (!scheduled) {
            AlarmPreferences.disableAlarm(this);
            updateAlarmStatus();
            updatePermissionStatus();
            Toast.makeText(
                    this,
                    "Allow Alarms & reminders, then set the alarm again.",
                    Toast.LENGTH_LONG
            ).show();
            openExactAlarmSettings();
            return;
        }

        updateAlarmStatus();

        Toast.makeText(
                this,
                "Alarm set for " + AlarmPreferences.formatDateTime(this, triggerAtMillis),
                Toast.LENGTH_LONG
        ).show();
    }

    private void cancelAlarm() {
        AlarmScheduler.cancel(this);
        AlarmPreferences.disableAlarm(this);
        updateAlarmStatus();
        Toast.makeText(this, "Alarm canceled", Toast.LENGTH_SHORT).show();
    }

    private void updateAlarmStatus() {
        if (!AlarmPreferences.isEnabled(this)) {
            alarmStatusText.setText(R.string.alarm_status_none);
            return;
        }

        long triggerAtMillis = AlarmPreferences.nextTriggerMillis(this);
        String repeat = getString(AlarmPreferences.repeatsDaily(this)
                ? R.string.repeat_daily
                : R.string.repeat_once);
        alarmStatusText.setText(getString(
                R.string.alarm_status_scheduled,
                repeat,
                AlarmPreferences.formatDateTime(this, triggerAtMillis)
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

        boolean needsFullScreenSettings = false;
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            needsFullScreenSettings = notificationManager != null
                    && !notificationManager.canUseFullScreenIntent();
        }

        boolean needsExactAlarmSettings = !AlarmScheduler.canScheduleExactAlarms(this);
        if (needsExactAlarmSettings) {
            if (notificationText.isEmpty()) {
                notificationText = "Alarms & reminders permission is off.";
            } else {
                notificationText += " Alarms & reminders permission is also off.";
            }
        }

        if (needsFullScreenSettings) {
            if (notificationText.isEmpty()) {
                notificationText = "Full-screen alarm display is disabled by Android settings.";
            } else {
                notificationText += " Full-screen alarm display is also disabled.";
            }
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

    private int currentThresholdLux() {
        return AlarmPreferences.MIN_THRESHOLD_LUX
                + thresholdSeekBar.getProgress() * AlarmPreferences.THRESHOLD_STEP_LUX;
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

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable background = new GradientDrawable();
        background.setColor(surfaceColor);
        background.setCornerRadius(dp(8));
        panel.setBackground(background);
        return panel;
    }

    private TextView sectionTitle(String value) {
        TextView textView = text(value, 18, Typeface.BOLD, textColor);
        textView.setPadding(0, 0, 0, dp(8));
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
        button.setTextColor(Color.rgb(35, 25, 0));
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(buttonBackground(primaryColor, 0));
        return button;
    }

    private Button secondaryButton(String value) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(value);
        button.setTextColor(textColor);
        button.setTextSize(16);
        button.setBackground(buttonBackground(Color.TRANSPARENT, Color.rgb(208, 216, 226)));
        return button;
    }

    private GradientDrawable buttonBackground(int fillColor, int strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fillColor);
        background.setCornerRadius(dp(8));
        if (strokeColor != 0) {
            background.setStroke(dp(1), strokeColor);
        }
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
