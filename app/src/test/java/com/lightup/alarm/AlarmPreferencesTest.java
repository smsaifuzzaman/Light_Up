package com.lightup.alarm;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class AlarmPreferencesTest {
    private TimeZone originalTimeZone;

    @Before
    public void setUp() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void oneTimeAlarmUsesTomorrowWhenTimeAlreadyPassed() {
        long fromMillis = millis(2026, Calendar.MAY, 10, 7, 1);

        long nextMillis = AlarmPreferences.nextTriggerMillis(
                7,
                0,
                AlarmPreferences.DAYS_NONE,
                0L,
                fromMillis
        );

        assertEquals(millis(2026, Calendar.MAY, 11, 7, 0), nextMillis);
    }

    @Test
    public void weekdayRepeatUsesNextSelectedDay() {
        int mondayWednesday = bit(Calendar.MONDAY) | bit(Calendar.WEDNESDAY);
        long sundayMorning = millis(2026, Calendar.MAY, 10, 6, 30);

        long nextMillis = AlarmPreferences.nextTriggerMillis(
                7,
                15,
                mondayWednesday,
                0L,
                sundayMorning
        );

        assertEquals(millis(2026, Calendar.MAY, 11, 7, 15), nextMillis);
    }

    @Test
    public void skipNextTriggerMovesToFollowingOccurrence() {
        long sundayMorning = millis(2026, Calendar.MAY, 10, 6, 30);
        long skippedMillis = millis(2026, Calendar.MAY, 10, 7, 0);

        long nextMillis = AlarmPreferences.nextTriggerMillis(
                7,
                0,
                AlarmPreferences.DAYS_ALL,
                skippedMillis,
                sundayMorning
        );

        assertEquals(millis(2026, Calendar.MAY, 11, 7, 0), nextMillis);
    }

    @Test
    public void repeatLabelFormatsSelectedWeekdays() {
        int fridaySaturday = bit(Calendar.FRIDAY) | bit(Calendar.SATURDAY);

        assertEquals("Fri, Sat", AlarmPreferences.formatRepeatDays(fridaySaturday));
    }

    private static int bit(int calendarDayOfWeek) {
        return 1 << (calendarDayOfWeek - Calendar.SUNDAY);
    }

    private static long millis(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
