package com.lightup.alarm;

import org.json.JSONException;
import org.json.JSONObject;

final class AlarmConfig {
    static final String DEFAULT_RINGTONE_LABEL = "System alarm sound";

    private static final String KEY_ID = "id";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_REPEAT_DAILY = "repeat_daily";
    private static final String KEY_REPEAT_DAYS = "repeat_days";
    private static final String KEY_SKIP_NEXT_TRIGGER = "skip_next_trigger";
    private static final String KEY_THRESHOLD_LUX = "threshold_lux";
    private static final String KEY_RINGTONE_URI = "ringtone_uri";
    private static final String KEY_RINGTONE_NAME = "ringtone_name";

    final long id;
    int hour;
    int minute;
    boolean enabled;
    int repeatDays;
    long skipNextTriggerMillis;
    int thresholdLux;
    String ringtoneUri;
    String ringtoneName;

    AlarmConfig(
            long id,
            int hour,
            int minute,
            boolean enabled,
            boolean repeatDaily,
            int thresholdLux,
            String ringtoneUri,
            String ringtoneName
    ) {
        this(
                id,
                hour,
                minute,
                enabled,
                repeatDaily ? AlarmPreferences.DAYS_ALL : AlarmPreferences.DAYS_NONE,
                0L,
                thresholdLux,
                ringtoneUri,
                ringtoneName
        );
    }

    AlarmConfig(
            long id,
            int hour,
            int minute,
            boolean enabled,
            int repeatDays,
            long skipNextTriggerMillis,
            int thresholdLux,
            String ringtoneUri,
            String ringtoneName
    ) {
        this.id = id;
        this.hour = clampHour(hour);
        this.minute = clampMinute(minute);
        this.enabled = enabled;
        this.repeatDays = repeatDays & AlarmPreferences.DAYS_ALL;
        this.skipNextTriggerMillis = Math.max(0L, skipNextTriggerMillis);
        this.thresholdLux = AlarmPreferences.clampThreshold(thresholdLux);
        this.ringtoneUri = emptyToNull(ringtoneUri);
        this.ringtoneName = emptyToNull(ringtoneName);
    }

    static AlarmConfig newDefault(long id) {
        return new AlarmConfig(
                id,
                AlarmPreferences.DEFAULT_HOUR,
                AlarmPreferences.DEFAULT_MINUTE,
                true,
                AlarmPreferences.DAYS_ALL,
                0L,
                AlarmPreferences.DEFAULT_THRESHOLD_LUX,
                null,
                null
        );
    }

    static AlarmConfig fromJson(JSONObject jsonObject) throws JSONException {
        int repeatDays = jsonObject.has(KEY_REPEAT_DAYS)
                ? jsonObject.optInt(KEY_REPEAT_DAYS, AlarmPreferences.DAYS_ALL)
                : (jsonObject.optBoolean(KEY_REPEAT_DAILY, true)
                        ? AlarmPreferences.DAYS_ALL
                        : AlarmPreferences.DAYS_NONE);
        return new AlarmConfig(
                jsonObject.getLong(KEY_ID),
                jsonObject.optInt(KEY_HOUR, AlarmPreferences.DEFAULT_HOUR),
                jsonObject.optInt(KEY_MINUTE, AlarmPreferences.DEFAULT_MINUTE),
                jsonObject.optBoolean(KEY_ENABLED, true),
                repeatDays,
                jsonObject.optLong(KEY_SKIP_NEXT_TRIGGER, 0L),
                jsonObject.optInt(KEY_THRESHOLD_LUX, AlarmPreferences.DEFAULT_THRESHOLD_LUX),
                jsonObject.optString(KEY_RINGTONE_URI, null),
                jsonObject.optString(KEY_RINGTONE_NAME, null)
        );
    }

    JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_ID, id);
        jsonObject.put(KEY_HOUR, hour);
        jsonObject.put(KEY_MINUTE, minute);
        jsonObject.put(KEY_ENABLED, enabled);
        jsonObject.put(KEY_REPEAT_DAILY, isDaily());
        jsonObject.put(KEY_REPEAT_DAYS, repeatDays);
        jsonObject.put(KEY_SKIP_NEXT_TRIGGER, skipNextTriggerMillis);
        jsonObject.put(KEY_THRESHOLD_LUX, thresholdLux);
        jsonObject.put(KEY_RINGTONE_URI, ringtoneUri);
        jsonObject.put(KEY_RINGTONE_NAME, ringtoneName);
        return jsonObject;
    }

    long nextTriggerMillis() {
        long trigger = AlarmPreferences.nextTriggerMillis(hour, minute, repeatDays, skipNextTriggerMillis, System.currentTimeMillis());
        clearExpiredSkip(trigger);
        return trigger;
    }

    long nextTriggerMillis(long fromMillis) {
        long trigger = AlarmPreferences.nextTriggerMillis(hour, minute, repeatDays, skipNextTriggerMillis, fromMillis);
        clearExpiredSkip(trigger);
        return trigger;
    }

    String ringtoneLabel() {
        return ringtoneName == null ? DEFAULT_RINGTONE_LABEL : ringtoneName;
    }

    boolean isRepeating() {
        return (repeatDays & AlarmPreferences.DAYS_ALL) != AlarmPreferences.DAYS_NONE;
    }

    boolean isDaily() {
        return (repeatDays & AlarmPreferences.DAYS_ALL) == AlarmPreferences.DAYS_ALL;
    }

    String repeatLabel() {
        return AlarmPreferences.formatRepeatDays(repeatDays);
    }

    void clearExpiredSkip(long currentNextTriggerMillis) {
        if (skipNextTriggerMillis > 0L && skipNextTriggerMillis < System.currentTimeMillis() - 60_000L) {
            skipNextTriggerMillis = 0L;
        }
    }

    long markSkipNext() {
        long nextTrigger = AlarmPreferences.nextTriggerMillis(hour, minute, repeatDays, 0L, System.currentTimeMillis());
        skipNextTriggerMillis = nextTrigger;
        return nextTriggerMillis();
    }

    private static int clampHour(int hour) {
        return Math.max(0, Math.min(23, hour));
    }

    private static int clampMinute(int minute) {
        return Math.max(0, Math.min(59, minute));
    }

    private static String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty() || "null".equals(value)) {
            return null;
        }
        return value;
    }
}
