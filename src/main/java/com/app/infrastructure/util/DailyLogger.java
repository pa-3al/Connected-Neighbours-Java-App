package com.app.infrastructure.util;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public class DailyLogger {
    private static final Path LOGS_DIR = Paths.get("logs");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static DailyLogger instance;
    private Path currentLogFile;
    private LocalDate currentDate;
    private final String sessionId;
    private final String userName;
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }
    private DailyLogger() {
        this.sessionId = java.util.UUID.randomUUID().toString().substring(0, 8);
        this.userName = System.getProperty("user.name", "UnknownUser");
        ensureLogsDirExists();
        updateLogFile();
    }
    public static synchronized DailyLogger getInstance() {
        if (instance == null) {
            instance = new DailyLogger();
        }
        return instance;
    }
    private void ensureLogsDirExists() {
        try {
            if (!Files.exists(LOGS_DIR)) {
                Files.createDirectories(LOGS_DIR);
                System.out.println("[DailyLogger] Created logs directory: " + LOGS_DIR.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[DailyLogger] Failed to create logs directory: " + e.getMessage());
        }
    }
    private void updateLogFile() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            currentDate = today;
            String fileName = "app-" + today.format(FILE_DATE_FORMAT) + ".log";
            currentLogFile = LOGS_DIR.resolve(fileName);
        }
    }
    private void writeLog(Level level, String source, String message, Throwable error) {
        updateLogFile(); 
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(timestamp).append("] ");
        sb.append("[").append(level.name()).append("] ");
        sb.append("[").append(sessionId).append("] ");
        sb.append("[").append(source).append("] ");
        sb.append(message);
        if (error != null) {
            sb.append("\n  Exception: ").append(error.getClass().getName());
            sb.append(": ").append(error.getMessage());
            for (StackTraceElement ste : error.getStackTrace()) {
                sb.append("\n    at ").append(ste.toString());
            }
        }
        sb.append("\n");
        try {
            Files.writeString(
                currentLogFile, 
                sb.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("[DailyLogger] Failed to write log: " + e.getMessage());
        }
        if (level == Level.ERROR || level == Level.WARN) {
            System.err.print(sb);
        } else {
            System.out.print(sb);
        }
    }
    public void debug(String source, String message) {
        writeLog(Level.DEBUG, source, message, null);
    }
    public void info(String source, String message) {
        writeLog(Level.INFO, source, message, null);
    }
    public void warn(String source, String message) {
        writeLog(Level.WARN, source, message, null);
    }
    public void warn(String source, String message, Throwable error) {
        writeLog(Level.WARN, source, message, error);
    }
    public void error(String source, String message) {
        writeLog(Level.ERROR, source, message, null);
    }
    public void error(String source, String message, Throwable error) {
        writeLog(Level.ERROR, source, message, error);
    }
    public static void logDebug(String source, String message) {
        getInstance().debug(source, message);
    }
    public static void logInfo(String source, String message) {
        getInstance().info(source, message);
    }
    public static void logWarn(String source, String message) {
        getInstance().warn(source, message);
    }
    public static void logWarn(String source, String message, Throwable error) {
        getInstance().warn(source, message, error);
    }
    public static void logError(String source, String message) {
        getInstance().error(source, message);
    }
    public static void logError(String source, String message, Throwable error) {
        getInstance().error(source, message, error);
    }
    public Path getCurrentLogFile() {
        updateLogFile();
        return currentLogFile;
    }
    public void logAppStart() {
        info("Application", "========== APPLICATION STARTED ==========");
        info("Application", "Java Version: " + System.getProperty("java.version"));
        info("Application", "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        info("Application", "User: " + userName);
        info("Application", "User Dir: " + System.getProperty("user.dir"));
    }
    public void logAppShutdown() {
        info("Application", "========== APPLICATION SHUTDOWN ==========");
    }
}
