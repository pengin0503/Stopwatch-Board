package com.pengin0503.stopwatchboard.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardIdTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "a",
            "0",
            "parkour",
            "elytra-race_1",
            "boss_kill-2026",
            "a1234567890123456789012345678901" // 32 chars
    })
    void validBoardIds(String id) {
        assertTrue(BoardId.isValid(id));
        assertEquals(id, BoardId.requireValid(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "Parkour",
            "-leading-dash",
            "_leading_underscore",
            "has space",
            "has.dot",
            "has/slash",
            "日本語",
            "a12345678901234567890123456789012" // 33 chars
    })
    void invalidBoardIds(String id) {
        assertFalse(BoardId.isValid(id));
        assertThrows(IllegalArgumentException.class, () -> BoardId.requireValid(id));
    }

    @Test
    void nullBoardIdIsInvalid() {
        assertFalse(BoardId.isValid(null));
        assertThrows(IllegalArgumentException.class, () -> BoardId.requireValid(null));
    }
}
