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
    private static final String KEY_THRESHOLD_LUX = "threshold_lux";
    private static final String KEY_RINGTONE_URI = "ringtone_uri";
    private static final String KEY_RINGTONE_NAME = "ringtone_name";

    final long id;
    int hour;
    int minute;
    boolean enabled;
    boolean repeatDaily;
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
        this.id = id;
        this.hour = clampHour(hour);
        this.minute = clampMinute(minute);
        this.enabled = enabled;
        this.repeatDaily = repeatDaily;
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
                true,
                AlarmPreferences.DEFAULT_THRESHOLD_LUX,
                null,
                null
        );
    }

    static AlarmConfig fromJson(JSONObject jsonObject) throws JSONException {
        return new AlarmConfig(
                jsonObject.getLong(KEY_ID),
                jsonObject.optInt(KEY_HOUR, AlarmPreferences.DEFAULT_HOUR),
                jsonObject.optInt(KEY_MINUTE, AlarmPreferences.DEFAULT_MINUTE),
                jsonObject.optBoolean(KEY_ENABLED, true),
                jsonObject.optBoolean(KEY_REPEAT_DAILY, true),
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
        jsonObject.put(KEY_REPEAT_DAILY, repeatDaily);
        jsonObject.put(KEY_THRESHOLD_LUX, thresholdLux);
        jsonObject.put(KEY_RINGTONE_URI, ringtoneUri);
        jsonObject.put(KEY_RINGTONE_NAME, ringtoneName);
        return jsonObject;
    }

    long nextTriggerMillis() {
        return AlarmPreferences.nextTriggerMillis(hour, minute, System.currentTimeMillis());
    }

    long nextTriggerMillis(long fromMillis) {
        return AlarmPreferences.nextTriggerMillis(hour, minute, fromMillis);
    }

    String ringtoneLabel() {
        return ringtoneName == null ? DEFAULT_RINGTONE_LABEL : ringtoneName;
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
