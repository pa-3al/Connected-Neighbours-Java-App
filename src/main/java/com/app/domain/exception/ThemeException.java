package com.app.domain.exception;
public class ThemeException extends RuntimeException {
    private final ErrorCode errorCode;
    public ThemeException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public ThemeException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    public enum ErrorCode {
        THEME_NOT_FOUND,
        LOAD_FAILED,
        APPLY_FAILED,
        EXPORT_FAILED,
        IMPORT_FAILED,
        DELETE_FAILED,
        DOWNLOAD_FAILED,
        INVALID_THEME
    }
}
