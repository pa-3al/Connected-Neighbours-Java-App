package com.app.domain.model;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

class UpdateInfoTest {

    @Test
    void shorthandConstructorShouldApplyDefaultValues() {
        UpdateInfo updateInfo = new UpdateInfo("2.1.0", "https://example.com/app.zip", "Bug fixes");

        assertEquals("2.1.0", updateInfo.version());
        assertEquals("https://example.com/app.zip", updateInfo.downloadUrl());
        assertEquals("Bug fixes", updateInfo.description());
        assertEquals("", updateInfo.changelog());
        assertEquals(LocalDate.now(), updateInfo.releaseDate());
        assertFalse(updateInfo.mandatory());
        assertEquals("1.0.0", updateInfo.minVersion());
    }
}