package com.lightup.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class AlarmScheduler {
    static final String ACTION_FIRE_ALARM = "com.lightup.alarm.action.FIRE_ALARM";

    private static final int REQUEST_FIRE_ALARM = 4100;
    private static final int REQUEST_SHOW_ALARM = 4101;

    private AlarmScheduler() {
    }

    static boolean scheduleFromPreferences(Context context) {
        if (!AlarmPreferences.isEnabled(context)) {
            return false;
        }
        return schedule(context, AlarmPreferences.nextTriggerMillis(context));
    }

    static boolean schedule(Context context, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }

        if (!canScheduleExactAlarms(context)) {
            return false;
        }

        PendingIntent operation = fireAlarmIntent(context, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent showIntent = showAlarmIntent(context);
        AlarmManager.AlarmClockInfo alarmClockInfo =
                new AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent);
        try {
            alarmManager.setAlarmClock(alarmClockInfo, operation);
            return true;
        } catch (SecurityException exception) {
            return false;
        }
    }

    static boolean canScheduleExactAlarms(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms();
    }

    static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = fireAlarmIntent(context, PendingIntent.FLAG_NO_CREATE);

        if (alarmManager != null && operation != null) {
            alarmManager.cancel(operation);
            operation.cancel();
        }
    }

    private static PendingIntent fireAlarmIntent(Context context, int flags) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_FIRE_ALARM);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_FIRE_ALARM,
                intent,
                flags | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent showAlarmIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                REQUEST_SHOW_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
