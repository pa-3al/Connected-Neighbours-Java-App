package com.app.domain.service;
import com.app.domain.port.out.LoggerPort;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
class UpdateServiceTest {
    private final LoggerPort mockLogger = new MockLoggerPort();
    @Test
    void testIsNewer_shouldReturnTrue_whenRemoteIsNewer() throws Exception {
        UpdateService service = new UpdateService(null, mockLogger);
        Method isNewer = UpdateService.class.getDeclaredMethod("isNewer", String.class, String.class);
        isNewer.setAccessible(true);
        assertTrue((boolean) isNewer.invoke(service, "2.0.0", "1.0.0"));
        assertTrue((boolean) isNewer.invoke(service, "1.1.0", "1.0.0"));
        assertTrue((boolean) isNewer.invoke(service, "1.0.1", "1.0.0"));
    }
    @Test
    void testIsNewer_shouldReturnFalse_whenCurrentIsNewer() throws Exception {
        UpdateService service = new UpdateService(null, mockLogger);
        Method isNewer = UpdateService.class.getDeclaredMethod("isNewer", String.class, String.class);
        isNewer.setAccessible(true);
        assertFalse((boolean) isNewer.invoke(service, "1.0.0", "2.0.0"));
        assertFalse((boolean) isNewer.invoke(service, "1.0.0", "1.1.0"));
        assertFalse((boolean) isNewer.invoke(service, "1.0.0", "1.0.1"));
    }
    @Test
    void testIsNewer_shouldReturnFalse_whenEqual() throws Exception {
        UpdateService service = new UpdateService(null, mockLogger);
        Method isNewer = UpdateService.class.getDeclaredMethod("isNewer", String.class, String.class);
        isNewer.setAccessible(true);
        assertFalse((boolean) isNewer.invoke(service, "1.0.0", "1.0.0"));
        assertFalse((boolean) isNewer.invoke(service, "2.5.3", "2.5.3"));
    }
    @Test
    void testIsNewer_shouldHandleDifferentLengths() throws Exception {
        UpdateService service = new UpdateService(null, mockLogger);
        Method isNewer = UpdateService.class.getDeclaredMethod("isNewer", String.class, String.class);
        isNewer.setAccessible(true);
        assertTrue((boolean) isNewer.invoke(service, "1.0.0.1", "1.0.0"));
        assertFalse((boolean) isNewer.invoke(service, "1.0", "1.0.0"));
        assertTrue((boolean) isNewer.invoke(service, "2", "1.9.9"));
    }
    @Test
    void testParseVersion_shouldHandlePrereleaseVersions() throws Exception {
        UpdateService service = new UpdateService(null, mockLogger);
        Method parseVersion = UpdateService.class.getDeclaredMethod("parseVersion", String.class);
        parseVersion.setAccessible(true);
        int[] result = (int[]) parseVersion.invoke(service, "1.0.0-beta");
        assertEquals(3, result.length);
        assertEquals(1, result[0]);
        assertEquals(0, result[1]);
        assertEquals(0, result[2]);
    }
    @Test
    void testGetCurrentVersion_shouldReturnValidVersion() {
        UpdateService service = new UpdateService(null, mockLogger);
        String version = service.getCurrentVersion();
        assertNotNull(version);
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"));
    }
    private static class MockLoggerPort implements LoggerPort {
        @Override public void debug(String tag, String message) {}
        @Override public void info(String tag, String message) {}
        @Override public void warn(String tag, String message) {}
        @Override public void error(String tag, String message, Throwable throwable) {}
    }
}
