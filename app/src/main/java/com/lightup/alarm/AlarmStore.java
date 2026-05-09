package com.lightup.alarm;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

final class AlarmStore {
    private static final String PREFS_NAME = "light_up_preferences";
    private static final String KEY_ALARMS = "alarms";
    private static final String KEY_MIGRATED_SINGLE_ALARM = "migrated_single_alarm";

    private static final String LEGACY_KEY_ENABLED = "enabled";
    private static final String LEGACY_KEY_HOUR = "hour";
    private static final String LEGACY_KEY_MINUTE = "minute";
    private static final String LEGACY_KEY_REPEAT_DAILY = "repeat_daily";
    private static final String LEGACY_KEY_THRESHOLD_LUX = "threshold_lux";

    private AlarmStore() {
    }

    static List<AlarmConfig> getAlarms(Context context) {
        SharedPreferences prefs = prefs(context);
        String stored = prefs.getString(KEY_ALARMS, null);
        if (stored == null) {
            return migrateSingleAlarmIfNeeded(prefs);
        }

        List<AlarmConfig> alarms = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(stored);
            for (int index = 0; index < array.length(); index++) {
                alarms.add(AlarmConfig.fromJson(array.getJSONObject(index)));
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return alarms;
    }

    static AlarmConfig getAlarm(Context context, long alarmId) {
        for (AlarmConfig alarm : getAlarms(context)) {
            if (alarm.id == alarmId) {
                return alarm;
            }
        }
        return null;
    }

    static long newAlarmId(Context context) {
        long candidate = System.currentTimeMillis();
        while (getAlarm(context, candidate) != null) {
            candidate++;
        }
        return candidate;
    }

    static void saveAlarm(Context context, AlarmConfig alarm) {
        List<AlarmConfig> alarms = getAlarms(context);
        boolean replaced = false;
        for (int index = 0; index < alarms.size(); index++) {
            if (alarms.get(index).id == alarm.id) {
                alarms.set(index, alarm);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            alarms.add(alarm);
        }
        saveAlarms(context, alarms);
    }

    static void deleteAlarm(Context context, long alarmId) {
        List<AlarmConfig> alarms = getAlarms(context);
        for (int index = alarms.size() - 1; index >= 0; index--) {
            if (alarms.get(index).id == alarmId) {
                alarms.remove(index);
            }
        }
        saveAlarms(context, alarms);
    }

    static void saveAlarms(Context context, List<AlarmConfig> alarms) {
        JSONArray array = new JSONArray();
        for (AlarmConfig alarm : alarms) {
            try {
                array.put(alarm.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs(context).edit().putString(KEY_ALARMS, array.toString()).apply();
    }

    private static List<AlarmConfig> migrateSingleAlarmIfNeeded(SharedPreferences prefs) {
        List<AlarmConfig> alarms = new ArrayList<>();
        if (prefs.getBoolean(KEY_MIGRATED_SINGLE_ALARM, false)) {
            return alarms;
        }

        boolean hasLegacyAlarm = prefs.contains(LEGACY_KEY_HOUR)
                || prefs.contains(LEGACY_KEY_MINUTE)
                || prefs.getBoolean(LEGACY_KEY_ENABLED, false);
        if (hasLegacyAlarm) {
            alarms.add(new AlarmConfig(
                    System.currentTimeMillis(),
                    prefs.getInt(LEGACY_KEY_HOUR, AlarmPreferences.DEFAULT_HOUR),
                    prefs.getInt(LEGACY_KEY_MINUTE, AlarmPreferences.DEFAULT_MINUTE),
                    prefs.getBoolean(LEGACY_KEY_ENABLED, false),
                    prefs.getBoolean(LEGACY_KEY_REPEAT_DAILY, true),
                    prefs.getInt(LEGACY_KEY_THRESHOLD_LUX, AlarmPreferences.DEFAULT_THRESHOLD_LUX),
                    null,
                    null
            ));
        }

        JSONArray array = new JSONArray();
        for (AlarmConfig alarm : alarms) {
            try {
                array.put(alarm.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs.edit()
                .putString(KEY_ALARMS, array.toString())
                .putBoolean(KEY_MIGRATED_SINGLE_ALARM, true)
                .apply();
        return alarms;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
