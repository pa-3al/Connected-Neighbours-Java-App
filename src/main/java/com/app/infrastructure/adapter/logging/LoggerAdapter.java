package com.app.infrastructure.adapter.logging;
import com.app.domain.port.out.LoggerPort;
import com.app.infrastructure.util.DailyLogger;
public class LoggerAdapter implements LoggerPort {
    @Override
    public void debug(String tag, String message) {
        DailyLogger.logDebug(tag, message);
    }
    @Override
    public void info(String tag, String message) {
        DailyLogger.logInfo(tag, message);
    }
    @Override
    public void warn(String tag, String message) {
        DailyLogger.logWarn(tag, message);
    }
    @Override
    public void error(String tag, String message, Throwable throwable) {
        DailyLogger.logError(tag, message, throwable);
    }
}
