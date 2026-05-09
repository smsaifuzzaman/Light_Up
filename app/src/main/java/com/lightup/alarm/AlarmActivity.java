package com.lightup.alarm;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class AlarmActivity extends Activity implements SensorEventListener {
    private final int backgroundColor = Color.rgb(7, 9, 24);
    private final int textColor = Color.rgb(232, 248, 255);
    private final int mutedColor = Color.rgb(139, 159, 205);
    private final int neonCyan = Color.rgb(54, 244, 255);
    private final int neonMagenta = Color.rgb(255, 43, 214);
    private final int neonAmber = Color.rgb(255, 184, 0);

    private SensorManager sensorManager;
    private Sensor lightSensor;
    private TextView currentLuxText;
    private TextView statusText;
    private ProgressBar progressBar;
    private Button emergencyButton;

    private int thresholdLux;
    private long alarmId = -1L;
    private long aboveThresholdSince = -1L;
    private boolean stopped;
    private final float[] luxSamples = new float[8];
    private int luxSampleIndex;
    private int luxSampleCount;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            updateHoldProgress(SystemClock.elapsedRealtime());
            if (!stopped && aboveThresholdSince >= 0L) {
                handler.postDelayed(this, 100L);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureAlarmWindow();

        alarmId = getIntent().getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L);
        AlarmConfig alarm = AlarmStore.getAlarm(this, alarmId);
        thresholdLux = alarm == null ? AlarmPreferences.DEFAULT_THRESHOLD_LUX : alarm.thresholdLux;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        setContentView(createContentView());
        if (lightSensor == null) {
            statusText.setText(R.string.alarm_no_light_sensor);
            emergencyButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        handler.removeCallbacks(progressUpdater);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT || event.values.length == 0 || stopped) {
            return;
        }

        float lux = smoothedLux(event.values[0]);
        currentLuxText.setText(String.format(Locale.getDefault(), "%.0f lux", lux));

        if (lux >= thresholdLux) {
            if (aboveThresholdSince < 0L) {
                aboveThresholdSince = SystemClock.elapsedRealtime();
                statusText.setText(R.string.alarm_hold_steady);
                handler.removeCallbacks(progressUpdater);
                handler.post(progressUpdater);
            }
            updateHoldProgress(SystemClock.elapsedRealtime());
        } else if (lux < Math.max(0, thresholdLux - 10)) {
            aboveThresholdSince = -1L;
            progressBar.setProgress(0);
            statusText.setText(R.string.alarm_waiting_for_light);
            handler.removeCallbacks(progressUpdater);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private View createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(36), dp(24), dp(28));
        root.setBackgroundColor(backgroundColor);

        Space topSpace = new Space(this);
        root.addView(topSpace, new LinearLayout.LayoutParams(1, 0, 0.45f));

        TextView title = text("LIGHT UP", 42, Typeface.BOLD, textColor);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView target = text("Target " + thresholdLux + " lux for 3 seconds", 17, Typeface.NORMAL, mutedColor);
        target.setGravity(Gravity.CENTER_HORIZONTAL);
        target.setPadding(0, dp(8), 0, dp(22));
        root.addView(target);

        currentLuxText = text("-- lux", 52, Typeface.BOLD, neonAmber);
        currentLuxText.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(currentLuxText);

        statusText = text(getString(R.string.alarm_waiting_for_light), 20, Typeface.BOLD, textColor);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        statusText.setPadding(0, dp(14), 0, dp(18));
        root.addView(statusText);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(AlarmPreferences.REQUIRED_LIGHT_MS);
        progressBar.setProgress(0);
        root.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(16)
        ));

        TextView note = text("Aim room light at the top edge of the phone.", 15, Typeface.NORMAL, mutedColor);
        note.setGravity(Gravity.CENTER_HORIZONTAL);
        note.setPadding(0, dp(18), 0, dp(24));
        root.addView(note);

        emergencyButton = new Button(this);
        emergencyButton.setAllCaps(false);
        emergencyButton.setText(R.string.alarm_hold_to_stop);
        emergencyButton.setTextColor(textColor);
        emergencyButton.setTextSize(16);
        emergencyButton.setBackground(buttonBackground());
        emergencyButton.setOnClickListener(view ->
                Toast.makeText(this, "Long-press to stop manually", Toast.LENGTH_SHORT).show());
        emergencyButton.setOnLongClickListener(view -> {
            stopAlarmAndFinish();
            return true;
        });
        root.addView(emergencyButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
        ));

        Space bottomSpace = new Space(this);
        root.addView(bottomSpace, new LinearLayout.LayoutParams(1, 0, 0.55f));

        return root;
    }

    private void configureAlarmWindow() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        }
    }

    private void updateHoldProgress(long nowMillis) {
        if (aboveThresholdSince < 0L || stopped) {
            return;
        }

        int progress = (int) Math.min(
                AlarmPreferences.REQUIRED_LIGHT_MS,
                nowMillis - aboveThresholdSince
        );
        progressBar.setProgress(progress);

        if (progress >= AlarmPreferences.REQUIRED_LIGHT_MS) {
            stopAlarmAndFinish();
        }
    }

    private float smoothedLux(float latestLux) {
        luxSamples[luxSampleIndex] = latestLux;
        luxSampleIndex = (luxSampleIndex + 1) % luxSamples.length;
        luxSampleCount = Math.min(luxSampleCount + 1, luxSamples.length);

        float total = 0f;
        for (int index = 0; index < luxSampleCount; index++) {
            total += luxSamples[index];
        }
        return total / luxSampleCount;
    }

    private void stopAlarmAndFinish() {
        if (stopped) {
            return;
        }

        stopped = true;
        handler.removeCallbacks(progressUpdater);
        Intent intent = new Intent(this, AlarmRingingService.class);
        intent.setAction(AlarmRingingService.ACTION_STOP);
        intent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
        startService(intent);
        finishAndRemoveTask();
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

    private GradientDrawable buttonBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.TRANSPARENT);
        background.setStroke(dp(1), neonMagenta);
        background.setCornerRadius(dp(8));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
