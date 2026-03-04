package com.app.domain.port.out;
public interface LoggerPort {
    void debug(String tag, String message);
    void info(String tag, String message);
    void warn(String tag, String message);
    void error(String tag, String message, Throwable throwable);
}
