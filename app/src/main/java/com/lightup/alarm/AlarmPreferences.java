package com.lightup.alarm;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class AlarmPreferences {
    static final int DEFAULT_THRESHOLD_LUX = 180;
    static final int MIN_THRESHOLD_LUX = 0;
    static final int MAX_THRESHOLD_LUX = 500;
    static final int THRESHOLD_STEP_LUX = 5;
    static final int REQUIRED_LIGHT_MS = 3000;
    static final int DEFAULT_HOUR = 7;
    static final int DEFAULT_MINUTE = 0;

    private AlarmPreferences() {
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
}
