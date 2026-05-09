package com.lightup.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !AlarmScheduler.ACTION_FIRE_ALARM.equals(intent.getAction())) {
            return;
        }

        long alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L);
        AlarmConfig alarm = AlarmStore.getAlarm(context, alarmId);
        if (alarm == null || !alarm.enabled) {
            return;
        }

        if (alarm.repeatDaily) {
            long nextTrigger = alarm.nextTriggerMillis(System.currentTimeMillis() + 60_000L);
            AlarmScheduler.schedule(context, alarm, nextTrigger);
        } else {
            alarm.enabled = false;
            AlarmStore.saveAlarm(context, alarm);
        }

        Intent serviceIntent = new Intent(context, AlarmRingingService.class);
        serviceIntent.setAction(AlarmRingingService.ACTION_START);
        serviceIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
