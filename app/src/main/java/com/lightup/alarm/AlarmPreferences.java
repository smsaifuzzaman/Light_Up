package com.lightup.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class AlarmPreferences {
    static final int DEFAULT_THRESHOLD_LUX = 180;
    static final int MIN_THRESHOLD_LUX = 25;
    static final int MAX_THRESHOLD_LUX = 2500;
    static final int THRESHOLD_STEP_LUX = 5;
    static final int REQUIRED_LIGHT_MS = 3000;

    private static final String PREFS_NAME = "light_up_preferences";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_REPEAT_DAILY = "repeat_daily";
    private static final String KEY_THRESHOLD_LUX = "threshold_lux";
    private static final int DEFAULT_HOUR = 7;
    private static final int DEFAULT_MINUTE = 0;

    private AlarmPreferences() {
    }

    static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    static int getHour(Context context) {
        return prefs(context).getInt(KEY_HOUR, DEFAULT_HOUR);
    }

    static int getMinute(Context context) {
        return prefs(context).getInt(KEY_MINUTE, DEFAULT_MINUTE);
    }

    static boolean repeatsDaily(Context context) {
        return prefs(context).getBoolean(KEY_REPEAT_DAILY, true);
    }

    static int getThresholdLux(Context context) {
        return prefs(context).getInt(KEY_THRESHOLD_LUX, DEFAULT_THRESHOLD_LUX);
    }

    static void saveAlarm(Context context, int hour, int minute, int thresholdLux, boolean repeatDaily) {
        prefs(context)
                .edit()
                .putBoolean(KEY_ENABLED, true)
                .putInt(KEY_HOUR, hour)
                .putInt(KEY_MINUTE, minute)
                .putBoolean(KEY_REPEAT_DAILY, repeatDaily)
                .putInt(KEY_THRESHOLD_LUX, clampThreshold(thresholdLux))
                .apply();
    }

    static void disableAlarm(Context context) {
        prefs(context)
                .edit()
                .putBoolean(KEY_ENABLED, false)
                .apply();
    }

    static long nextTriggerMillis(Context context) {
        return nextTriggerMillis(getHour(context), getMinute(context), System.currentTimeMillis());
    }

    static long nextTriggerMillis(Context context, long fromMillis) {
        return nextTriggerMillis(getHour(context), getMinute(context), fromMillis);
    }

    static long nextTriggerMillis(int hour, int minute, long fromMillis) {
        Calendar trigger = Calendar.getInstance();
        trigger.setTimeInMillis(fromMillis);
        trigger.set(Calendar.HOUR_OF_DAY, hour);
        trigger.set(Calendar.MINUTE, minute);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);

        if (trigger.getTimeInMillis() <= fromMillis) {
            trigger.add(Calendar.DAY_OF_YEAR, 1);
        }

        return trigger.getTimeInMillis();
    }

    static String formatAlarmTime(Context context, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return DateFormat.getTimeFormat(context).format(calendar.getTime());
    }

    static String formatDateTime(Context context, long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        return dateFormat.format(calendar.getTime()) + " at "
                + DateFormat.getTimeFormat(context).format(calendar.getTime());
    }

    static int clampThreshold(int thresholdLux) {
        return Math.max(MIN_THRESHOLD_LUX, Math.min(MAX_THRESHOLD_LUX, thresholdLux));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
