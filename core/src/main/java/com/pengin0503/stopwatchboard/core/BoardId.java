package com.pengin0503.stopwatchboard.core;

import java.util.regex.Pattern;

/**
 * Validation utilities for Stopwatch Board identifiers.
 * Valid IDs match {@code [a-z0-9][a-z0-9_-]*} and are at most 32 characters long.
 */
public final class BoardId {
    public static final int MAX_LENGTH = 32;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_-]{0,31}$");

    private BoardId() {
    }

    public static boolean isValid(String id) {
        if (id == null || id.isEmpty() || id.length() > MAX_LENGTH) {
            return false;
        }
        return VALID_PATTERN.matcher(id).matches();
    }

    public static String requireValid(String id) {
        if (!isValid(id)) {
            throw new IllegalArgumentException(
                    "Invalid board id '" + id + "': must match [a-z0-9][a-z0-9_-]* and be at most " + MAX_LENGTH + " characters"
            );
        }
        return id;
    }
}
