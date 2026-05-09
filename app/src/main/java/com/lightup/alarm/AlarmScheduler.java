package com.lightup.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class AlarmScheduler {
    static final String ACTION_FIRE_ALARM = "com.lightup.alarm.action.FIRE_ALARM";
    static final String EXTRA_ALARM_ID = "com.lightup.alarm.extra.ALARM_ID";

    private static final int FIRE_ALARM_SALT = 4100;
    private static final int SHOW_ALARM_SALT = 4101;

    private AlarmScheduler() {
    }

    static boolean scheduleAll(Context context) {
        boolean allScheduled = true;
        for (AlarmConfig alarm : AlarmStore.getAlarms(context)) {
            if (alarm.enabled) {
                allScheduled = schedule(context, alarm) && allScheduled;
            }
        }
        return allScheduled;
    }

    static boolean schedule(Context context, AlarmConfig alarm) {
        return schedule(context, alarm, alarm.nextTriggerMillis());
    }

    static boolean schedule(Context context, AlarmConfig alarm, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }

        if (!canScheduleExactAlarms(context)) {
            return false;
        }

        PendingIntent operation = fireAlarmIntent(context, alarm.id, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent showIntent = showAlarmIntent(context, alarm.id);
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

    static void cancel(Context context, long alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = fireAlarmIntent(context, alarmId, PendingIntent.FLAG_NO_CREATE);

        if (alarmManager != null && operation != null) {
            alarmManager.cancel(operation);
            operation.cancel();
        }
    }

    private static PendingIntent fireAlarmIntent(Context context, long alarmId, int flags) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_FIRE_ALARM);
        intent.putExtra(EXTRA_ALARM_ID, alarmId);
        return PendingIntent.getBroadcast(
                context,
                requestCode(alarmId, FIRE_ALARM_SALT),
                intent,
                flags | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent showAlarmIntent(Context context, long alarmId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_ALARM_ID, alarmId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                requestCode(alarmId, SHOW_ALARM_SALT),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static int requestCode(long alarmId, int salt) {
        return ((int) (alarmId ^ (alarmId >>> 32))) ^ salt;
    }
}
