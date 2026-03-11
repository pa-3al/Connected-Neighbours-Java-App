package com.app.infrastructure.adapter.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class InfrastructureLogger {
    private InfrastructureLogger() {
    }

    private static Logger getLogger(String tag) {
        return Logger.getLogger("com.app." + tag);
    }

    public static void debug(String tag, String message) {
        getLogger(tag).fine(message);
    }

    public static void info(String tag, String message) {
        getLogger(tag).info(message);
    }

    public static void warn(String tag, String message) {
        getLogger(tag).warning(message);
    }

    public static void error(String tag, String message) {
        getLogger(tag).severe(message);
    }

    public static void error(String tag, String message, Throwable throwable) {
        getLogger(tag).log(Level.SEVERE, message, throwable);
    }

    public static void logDebug(String tag, String message) {
        debug(tag, message);
    }

    public static void logInfo(String tag, String message) {
        info(tag, message);
    }

    public static void logWarn(String tag, String message) {
        warn(tag, message);
    }

    public static void logError(String tag, String message) {
        error(tag, message);
    }

    public static void logError(String tag, String message, Throwable throwable) {
        error(tag, message, throwable);
    }
}
