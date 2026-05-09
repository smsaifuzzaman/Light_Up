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

        if (AlarmPreferences.repeatsDaily(context)) {
            long nextTrigger = AlarmPreferences.nextTriggerMillis(context, System.currentTimeMillis() + 60_000L);
            AlarmScheduler.schedule(context, nextTrigger);
        } else {
            AlarmPreferences.disableAlarm(context);
        }

        Intent serviceIntent = new Intent(context, AlarmRingingService.class);
        serviceIntent.setAction(AlarmRingingService.ACTION_START);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
