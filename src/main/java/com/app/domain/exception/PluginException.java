package com.app.domain.exception;
public class PluginException extends RuntimeException {
    private final ErrorCode errorCode;
    public PluginException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public PluginException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    public enum ErrorCode {
        LOAD_FAILED,
        INVALID_PLUGIN,
        DUPLICATE_ID,
        PLUGIN_NOT_FOUND,
        UNINSTALL_FAILED,
        SECURITY_VIOLATION,
        INCOMPATIBLE_VERSION
    }
}
