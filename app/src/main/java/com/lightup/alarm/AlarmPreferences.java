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
    static final int DAYS_NONE = 0;
    static final int DAYS_ALL = 0b1111111;
    static final String[] DAY_LABELS = new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    private AlarmPreferences() {
    }

    static long nextTriggerMillis(int hour, int minute, long fromMillis) {
        return nextTriggerMillis(hour, minute, DAYS_NONE, 0L, fromMillis);
    }

    static long nextTriggerMillis(int hour, int minute, int repeatDays, long skipTriggerMillis, long fromMillis) {
        Calendar trigger = Calendar.getInstance();
        trigger.setTimeInMillis(fromMillis);
        trigger.set(Calendar.HOUR_OF_DAY, hour);
        trigger.set(Calendar.MINUTE, minute);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);

        int safeRepeatDays = repeatDays & DAYS_ALL;
        if (safeRepeatDays == DAYS_NONE) {
            if (trigger.getTimeInMillis() <= fromMillis || sameTrigger(trigger.getTimeInMillis(), skipTriggerMillis)) {
                trigger.add(Calendar.DAY_OF_YEAR, 1);
            }
            return trigger.getTimeInMillis();
        }

        for (int dayOffset = 0; dayOffset <= 7; dayOffset++) {
            if (dayOffset > 0) {
                trigger.add(Calendar.DAY_OF_YEAR, 1);
            }
            boolean dayMatches = (safeRepeatDays & dayBit(trigger)) != 0;
            boolean future = trigger.getTimeInMillis() > fromMillis;
            if (dayMatches && future && !sameTrigger(trigger.getTimeInMillis(), skipTriggerMillis)) {
                return trigger.getTimeInMillis();
            }
        }

        trigger.add(Calendar.DAY_OF_YEAR, 1);
        return trigger.getTimeInMillis();
    }

    static int dayBit(Calendar calendar) {
        return 1 << (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY);
    }

    static String formatRepeatDays(int repeatDays) {
        int safeRepeatDays = repeatDays & DAYS_ALL;
        if (safeRepeatDays == DAYS_NONE) {
            return "Once";
        }
        if (safeRepeatDays == DAYS_ALL) {
            return "Daily";
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < DAY_LABELS.length; index++) {
            if ((safeRepeatDays & (1 << index)) != 0) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(DAY_LABELS[index]);
            }
        }
        return builder.toString();
    }

    static boolean sameTrigger(long firstMillis, long secondMillis) {
        return secondMillis > 0L && Math.abs(firstMillis - secondMillis) < 1000L;
    }

    static long nextTriggerMillis(int hour, int minute, int repeatDays, long fromMillis) {
        return nextTriggerMillis(hour, minute, repeatDays, 0L, fromMillis);
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
