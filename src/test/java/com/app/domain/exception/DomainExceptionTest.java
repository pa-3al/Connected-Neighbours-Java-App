package com.app.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    void pluginExceptionShouldExposeErrorCodeAndMessage() {
        PluginException exception = new PluginException(PluginException.ErrorCode.PLUGIN_NOT_FOUND, "Missing plugin");

        assertEquals(PluginException.ErrorCode.PLUGIN_NOT_FOUND, exception.getErrorCode());
        assertEquals("Missing plugin", exception.getMessage());
    }

    @Test
    void pluginExceptionShouldExposeCause() {
        RuntimeException cause = new RuntimeException("boom");
        PluginException exception = new PluginException(PluginException.ErrorCode.LOAD_FAILED, "Load failed", cause);

        assertEquals(PluginException.ErrorCode.LOAD_FAILED, exception.getErrorCode());
        assertSame(cause, exception.getCause());
    }

    @Test
    void themeExceptionShouldExposeErrorCodeAndMessage() {
        ThemeException exception = new ThemeException(ThemeException.ErrorCode.THEME_NOT_FOUND, "Missing theme");

        assertEquals(ThemeException.ErrorCode.THEME_NOT_FOUND, exception.getErrorCode());
        assertEquals("Missing theme", exception.getMessage());
    }

    @Test
    void themeExceptionShouldExposeCause() {
        RuntimeException cause = new RuntimeException("boom");
        ThemeException exception = new ThemeException(ThemeException.ErrorCode.LOAD_FAILED, "Load failed", cause);

        assertEquals(ThemeException.ErrorCode.LOAD_FAILED, exception.getErrorCode());
        assertSame(cause, exception.getCause());
    }
}
