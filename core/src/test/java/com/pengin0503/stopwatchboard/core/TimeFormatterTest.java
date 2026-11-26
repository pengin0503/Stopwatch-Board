package com.pengin0503.stopwatchboard.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeFormatterTest {

    @Test
    void formatTotalUnderOneHour() {
        assertEquals("0:00.000", TimeFormatter.formatTotal(0L));
        assertEquals("0:00.005", TimeFormatter.formatTotal(5L));
        assertEquals("0:09.042", TimeFormatter.formatTotal(9_042L));
        assertEquals("3:07.042", TimeFormatter.formatTotal(187_042L));
        assertEquals("59:59.999", TimeFormatter.formatTotal(3_599_999L));
    }

    @Test
    void formatTotalHourRolloverAndAbove() {
        assertEquals("1:00:00.000", TimeFormatter.formatTotal(3_600_000L));
        assertEquals("1:02:03.004", TimeFormatter.formatTotal(3_723_004L));
        assertEquals("12:34:56.789", TimeFormatter.formatTotal(45_296_789L));
    }

    @Test
    void formatTotalRejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> TimeFormatter.formatTotal(-1L));
    }

    @Test
    void formatDeltaUnderSixtySeconds() {
        assertEquals("+0.000s", TimeFormatter.formatDelta(0L));
        assertEquals("+1.234s", TimeFormatter.formatDelta(1_234L));
        assertEquals("-0.087s", TimeFormatter.formatDelta(-87L));
        assertEquals("+59.999s", TimeFormatter.formatDelta(59_999L));
        assertEquals("-59.999s", TimeFormatter.formatDelta(-59_999L));
    }

    @Test
    void formatDeltaSixtySecondsAndAbove() {
        assertEquals("+1:00.000", TimeFormatter.formatDelta(60_000L));
        assertEquals("-1:00.000", TimeFormatter.formatDelta(-60_000L));
        assertEquals("+1:02.345", TimeFormatter.formatDelta(62_345L));
        assertEquals("-1:02.345", TimeFormatter.formatDelta(-62_345L));
        assertEquals("+1:00:00.000", TimeFormatter.formatDelta(3_600_000L));
        assertEquals("-1:02:03.456", TimeFormatter.formatDelta(-3_723_456L));
    }

    @Test
    void formatDateUtc() {
        // 2026-11-26T00:00:00Z
        assertEquals("2026-11-26", TimeFormatter.formatDate(1795651200000L));
    }
}
