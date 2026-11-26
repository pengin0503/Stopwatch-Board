package com.pengin0503.stopwatchboard.core;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Pure Java formatting utilities for stopwatch totals, split deltas, and record dates.
 */
public final class TimeFormatter {
    private static final long MILLIS_PER_SECOND = 1_000L;
    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final long MILLIS_PER_HOUR = 3_600_000L;

    private static final DateTimeFormatter UTC_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT).withZone(ZoneOffset.UTC);

    private TimeFormatter() {
    }

    /**
     * Formats a non-negative duration in milliseconds.
     * Under 1 hour: {@code m:ss.mmm} (e.g. {@code 3:07.042}).
     * 1 hour or more: {@code h:mm:ss.mmm} (e.g. {@code 1:02:03.004}).
     */
    public static String formatTotal(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Total duration must be non-negative: " + millis);
        }
        long hours = millis / MILLIS_PER_HOUR;
        long minutes = (millis % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE;
        long seconds = (millis % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND;
        long ms = millis % MILLIS_PER_SECOND;

        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d.%03d", hours, minutes, seconds, ms);
        }
        return String.format(Locale.ROOT, "%d:%02d.%03d", minutes, seconds, ms);
    }

    /**
     * Formats a signed delta in milliseconds. Sign is always shown ({@code +} for >= 0, {@code -} for < 0).
     * Under 60 seconds: {@code +1.234s} / {@code -0.087s} / {@code +0.000s}.
     * 60 seconds or more: sign + total format (e.g. {@code +1:02.345}, {@code -1:00:00.000}).
     */
    public static String formatDelta(long deltaMillis) {
        boolean negative = deltaMillis < 0;
        String sign = negative ? "-" : "+";
        long absMillis = negative ? Math.negateExact(deltaMillis) : deltaMillis;

        if (absMillis < MILLIS_PER_MINUTE) {
            long seconds = absMillis / MILLIS_PER_SECOND;
            long ms = absMillis % MILLIS_PER_SECOND;
            return String.format(Locale.ROOT, "%s%d.%03ds", sign, seconds, ms);
        }
        return sign + formatTotal(absMillis);
    }

    /**
     * Formats an epoch-millisecond timestamp as a UTC date ({@code yyyy-MM-dd}).
     */
    public static String formatDate(long epochMillis) {
        return UTC_DATE_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }
}
