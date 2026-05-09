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
    private static final String KEY_SNOOZE_MINUTES = "snooze_minutes";
    private static final String KEY_MAX_SNOOZES = "max_snoozes";
    private static final String KEY_SNOOZES_USED = "snoozes_used";
    private static final String KEY_SNOOZE_UNTIL = "snooze_until";
    private static final String KEY_VOLUME_RAMP_SECONDS = "volume_ramp_seconds";
    private static final String KEY_VIBRATION_PATTERN = "vibration_pattern";

    static final int DEFAULT_SNOOZE_MINUTES = 5;
    static final int DEFAULT_MAX_SNOOZES = 2;
    static final int DEFAULT_VOLUME_RAMP_SECONDS = 30;

    static final int VIBRATION_OFF = 0;
    static final int VIBRATION_PULSE = 1;
    static final int VIBRATION_STEADY = 2;
    static final int VIBRATION_URGENT = 3;
    static final int DEFAULT_VIBRATION_PATTERN = VIBRATION_URGENT;

    static final int[] SNOOZE_MINUTE_OPTIONS = new int[]{0, 5, 10, 15};
    static final int[] MAX_SNOOZE_OPTIONS = new int[]{0, 1, 2, 3};
    static final int[] VOLUME_RAMP_OPTIONS = new int[]{0, 15, 30, 60};
    static final int[] VIBRATION_PATTERN_OPTIONS = new int[]{
            VIBRATION_OFF,
            VIBRATION_PULSE,
            VIBRATION_STEADY,
            VIBRATION_URGENT
    };

    final long id;
    int hour;
    int minute;
    boolean enabled;
    int repeatDays;
    long skipNextTriggerMillis;
    int thresholdLux;
    String ringtoneUri;
    String ringtoneName;
    int snoozeMinutes;
    int maxSnoozes;
    int snoozesUsed;
    long snoozeUntilMillis;
    int volumeRampSeconds;
    int vibrationPattern;

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
                ringtoneName,
                DEFAULT_SNOOZE_MINUTES,
                DEFAULT_MAX_SNOOZES,
                0,
                0L,
                DEFAULT_VOLUME_RAMP_SECONDS,
                DEFAULT_VIBRATION_PATTERN
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
        this(
                id,
                hour,
                minute,
                enabled,
                repeatDays,
                skipNextTriggerMillis,
                thresholdLux,
                ringtoneUri,
                ringtoneName,
                DEFAULT_SNOOZE_MINUTES,
                DEFAULT_MAX_SNOOZES,
                0,
                0L,
                DEFAULT_VOLUME_RAMP_SECONDS,
                DEFAULT_VIBRATION_PATTERN
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
            String ringtoneName,
            int snoozeMinutes,
            int maxSnoozes,
            int snoozesUsed,
            long snoozeUntilMillis,
            int volumeRampSeconds,
            int vibrationPattern
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
        this.snoozeMinutes = clamp(snoozeMinutes, 0, 30);
        this.maxSnoozes = clamp(maxSnoozes, 0, 3);
        this.snoozesUsed = clamp(snoozesUsed, 0, this.maxSnoozes);
        this.snoozeUntilMillis = Math.max(0L, snoozeUntilMillis);
        this.volumeRampSeconds = clamp(volumeRampSeconds, 0, 120);
        this.vibrationPattern = clampVibrationPattern(vibrationPattern);
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
                null,
                DEFAULT_SNOOZE_MINUTES,
                DEFAULT_MAX_SNOOZES,
                0,
                0L,
                DEFAULT_VOLUME_RAMP_SECONDS,
                DEFAULT_VIBRATION_PATTERN
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
                jsonObject.optString(KEY_RINGTONE_NAME, null),
                jsonObject.optInt(KEY_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES),
                jsonObject.optInt(KEY_MAX_SNOOZES, DEFAULT_MAX_SNOOZES),
                jsonObject.optInt(KEY_SNOOZES_USED, 0),
                jsonObject.optLong(KEY_SNOOZE_UNTIL, 0L),
                jsonObject.optInt(KEY_VOLUME_RAMP_SECONDS, DEFAULT_VOLUME_RAMP_SECONDS),
                jsonObject.optInt(KEY_VIBRATION_PATTERN, DEFAULT_VIBRATION_PATTERN)
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
        jsonObject.put(KEY_SNOOZE_MINUTES, snoozeMinutes);
        jsonObject.put(KEY_MAX_SNOOZES, maxSnoozes);
        jsonObject.put(KEY_SNOOZES_USED, snoozesUsed);
        jsonObject.put(KEY_SNOOZE_UNTIL, snoozeUntilMillis);
        jsonObject.put(KEY_VOLUME_RAMP_SECONDS, volumeRampSeconds);
        jsonObject.put(KEY_VIBRATION_PATTERN, vibrationPattern);
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

    boolean hasSnoozeEnabled() {
        return snoozeMinutes > 0 && maxSnoozes > 0;
    }

    int snoozesRemaining() {
        return hasSnoozeEnabled() ? Math.max(0, maxSnoozes - snoozesUsed) : 0;
    }

    boolean hasSnoozesRemaining() {
        return snoozesRemaining() > 0;
    }

    long markSnoozed(long nowMillis) {
        if (!hasSnoozesRemaining()) {
            return 0L;
        }

        snoozesUsed++;
        snoozeUntilMillis = nowMillis + snoozeMinutes * 60_000L;
        enabled = true;
        return snoozeUntilMillis;
    }

    void clearSnoozeSession() {
        snoozesUsed = 0;
        snoozeUntilMillis = 0L;
    }

    String snoozeLabel() {
        if (!hasSnoozeEnabled()) {
            return "Off";
        }
        return snoozeMinutes + " min, " + maxSnoozes + " max";
    }

    String volumeRampLabel() {
        return volumeRampSeconds == 0 ? "Off" : volumeRampSeconds + " sec";
    }

    String vibrationLabel() {
        switch (vibrationPattern) {
            case VIBRATION_OFF:
                return "Off";
            case VIBRATION_PULSE:
                return "Pulse";
            case VIBRATION_STEADY:
                return "Steady";
            case VIBRATION_URGENT:
            default:
                return "Urgent";
        }
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampVibrationPattern(int vibrationPattern) {
        if (vibrationPattern < VIBRATION_OFF || vibrationPattern > VIBRATION_URGENT) {
            return DEFAULT_VIBRATION_PATTERN;
        }
        return vibrationPattern;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty() || "null".equals(value)) {
            return null;
        }
        return value;
    }
}
