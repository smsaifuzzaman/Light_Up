package com.lightup.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class AlarmConfigTest {
    @Test
    public void legacyJsonUsesWakeOptionDefaults() throws Exception {
        JSONObject jsonObject = new JSONObject()
                .put("id", 42L)
                .put("hour", 7)
                .put("minute", 30)
                .put("enabled", true)
                .put("repeat_daily", true)
                .put("threshold_lux", 180);

        AlarmConfig alarm = AlarmConfig.fromJson(jsonObject);

        assertEquals(AlarmConfig.DEFAULT_SNOOZE_MINUTES, alarm.snoozeMinutes);
        assertEquals(AlarmConfig.DEFAULT_MAX_SNOOZES, alarm.maxSnoozes);
        assertEquals(AlarmConfig.DEFAULT_VOLUME_RAMP_SECONDS, alarm.volumeRampSeconds);
        assertEquals(AlarmConfig.DEFAULT_VIBRATION_PATTERN, alarm.vibrationPattern);
    }

    @Test
    public void markSnoozedConsumesLimitedSnoozes() {
        AlarmConfig alarm = new AlarmConfig(
                42L,
                7,
                0,
                true,
                AlarmPreferences.DAYS_ALL,
                0L,
                180,
                null,
                null,
                10,
                2,
                0,
                0L,
                30,
                AlarmConfig.VIBRATION_PULSE
        );

        long firstSnooze = alarm.markSnoozed(1_000L);
        long secondSnooze = alarm.markSnoozed(2_000L);

        assertEquals(601_000L, firstSnooze);
        assertEquals(602_000L, secondSnooze);
        assertFalse(alarm.hasSnoozesRemaining());

        alarm.clearSnoozeSession();
        assertTrue(alarm.hasSnoozesRemaining());
        assertEquals(0, alarm.snoozesUsed);
        assertEquals(0L, alarm.snoozeUntilMillis);
    }

    @Test
    public void invalidWakeOptionsAreClamped() {
        AlarmConfig alarm = new AlarmConfig(
                42L,
                7,
                0,
                true,
                AlarmPreferences.DAYS_ALL,
                0L,
                180,
                null,
                null,
                90,
                12,
                8,
                -20L,
                900,
                99
        );

        assertEquals(30, alarm.snoozeMinutes);
        assertEquals(3, alarm.maxSnoozes);
        assertEquals(3, alarm.snoozesUsed);
        assertEquals(0L, alarm.snoozeUntilMillis);
        assertEquals(120, alarm.volumeRampSeconds);
        assertEquals(AlarmConfig.DEFAULT_VIBRATION_PATTERN, alarm.vibrationPattern);
    }
}
