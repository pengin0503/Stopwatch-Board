package com.pengin0503.stopwatchboard.core;

import java.util.Objects;

/**
 * Immutable definition of a timed leaderboard board.
 */
public record BoardDefinition(
        String id,
        String displayName,
        String createdBy,
        long createdAt
) {
    public BoardDefinition {
        BoardId.requireValid(id);
        displayName = (displayName == null || displayName.isBlank()) ? id : displayName.trim();
        createdBy = (createdBy == null || createdBy.isBlank()) ? "Server" : createdBy;
    }

    public String effectiveDisplayName() {
        return displayName;
    }
}
