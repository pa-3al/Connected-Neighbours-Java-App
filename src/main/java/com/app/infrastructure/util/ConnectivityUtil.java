package com.app.infrastructure.util;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
public class ConnectivityUtil {
    private static long lastCheckTime = 0;
    private static boolean isConnected = false;
    private static final long CACHE_DURATION_MS = 5000; 
    public static synchronized boolean checkConnectivity() {
        if (System.currentTimeMillis() - lastCheckTime < CACHE_DURATION_MS) {
            return isConnected;
        }
        try {
            com.app.infrastructure.util.DailyLogger.logDebug("Connectivity", "Checking network status...");
            URL url = java.net.URI.create("https://www.google.com").toURL();
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(2000); 
            connection.connect();
            isConnected = true;
            com.app.infrastructure.util.DailyLogger.logDebug("Connectivity", "Network status: ONLINE");
        } catch (MalformedURLException e) {
            com.app.infrastructure.util.DailyLogger.logDebug("Connectivity", "Network status: OFFLINE (Malformed URL: " + e.getMessage() + ")");
            isConnected = false;
            throw new RuntimeException(e);
        } catch (IOException e) {
            isConnected = false;
            com.app.infrastructure.util.DailyLogger.logDebug("Connectivity", "Network status: OFFLINE (" + e.getMessage() + ")");
        }
        lastCheckTime = System.currentTimeMillis();
        return isConnected;
    }
}
