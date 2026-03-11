package com.app.infrastructure.adapter.logging;
import com.app.domain.port.out.LoggerPort;
import com.app.infrastructure.adapter.logging.InfrastructureLogger;
public class LoggerAdapter implements LoggerPort {
    @Override
    public void debug(String tag, String message) {
        InfrastructureLogger.logDebug(tag, message);
    }
    @Override
    public void info(String tag, String message) {
        InfrastructureLogger.logInfo(tag, message);
    }
    @Override
    public void warn(String tag, String message) {
        InfrastructureLogger.logWarn(tag, message);
    }
    @Override
    public void error(String tag, String message, Throwable throwable) {
        InfrastructureLogger.logError(tag, message, throwable);
    }
}

