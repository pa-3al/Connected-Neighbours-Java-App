package com.app.infrastructure.ui;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AppState {
    private static final AppState instance = new AppState();
    private static final String CHECK_HOST = "www.google.com";
    private static final int CHECK_PORT = 80;
    private static final int CHECK_TIMEOUT_MS = 2000;
    private static final int CHECK_INTERVAL_SECONDS = 5;

    private final BooleanProperty online = new SimpleBooleanProperty(true);
    private final BooleanProperty syncing = new SimpleBooleanProperty(false);
    private final BooleanProperty manualMode = new SimpleBooleanProperty(false);
    private final BooleanProperty autoDetectionEnabled = new SimpleBooleanProperty(true);
    private final StringProperty accessToken = new SimpleStringProperty("");

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "NetworkChecker");
        t.setDaemon(true);
        return t;
    });

    private final List<Consumer<Boolean>> statusListeners = new ArrayList<>();

    private AppState() {
        startNetworkMonitoring();
    }

    public static AppState getInstance() {
        return instance;
    }

    private void startNetworkMonitoring() {
        scheduler.schedule(this::checkNetworkConnectivity, 1, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(
                this::checkNetworkConnectivity,
                CHECK_INTERVAL_SECONDS,
                CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void checkNetworkConnectivity() {
        if (manualMode.get()) {
            return;
        }
        boolean isConnected = false;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(CHECK_HOST, CHECK_PORT), CHECK_TIMEOUT_MS);
            isConnected = true;
        } catch (IOException e) {
            isConnected = false;
        }
        final boolean connected = isConnected;
        Platform.runLater(() -> {
            boolean wasOnline = online.get();
            online.set(connected);
            if (wasOnline != connected) {
                notifyListeners(connected);
            }
        });
    }

    public void recheckNow() {
        scheduler.schedule(this::checkNetworkConnectivity, 0, TimeUnit.MILLISECONDS);
    }

    public BooleanProperty onlineProperty() {
        return online;
    }

    public boolean isOnline() {
        return online.get();
    }

    public void setOnline(boolean isOnline) {
        this.online.set(isOnline);
        notifyListeners(isOnline);
    }

    public BooleanProperty syncingProperty() {
        return syncing;
    }

    public boolean isSyncing() {
        return syncing.get();
    }

    public void setSyncing(boolean isSyncing) {
        this.syncing.set(isSyncing);
    }

    public void setManualMode(boolean manual) {
        this.manualMode.set(manual);
        if (!manual) {
            recheckNow();
        }
    }

    public boolean isManualMode() {
        return manualMode.get();
    }

    public ReadOnlyBooleanProperty manualModeProperty() {
        return manualMode;
    }

    public BooleanProperty autoDetectionEnabledProperty() {
        return autoDetectionEnabled;
    }

    public void forceOffline() {
        setManualMode(true);
        online.set(false);
        notifyListeners(false);
    }

    public void forceOnline() {
        setManualMode(true);
        online.set(true);
        notifyListeners(true);
    }

    public void enableAutoDetection() {
        setManualMode(false);
    }

    public void addStatusListener(Consumer<Boolean> listener) {
        statusListeners.add(listener);
    }

    public void removeStatusListener(Consumer<Boolean> listener) {
        statusListeners.remove(listener);
    }

    private void notifyListeners(boolean online) {
        for (Consumer<Boolean> listener : statusListeners) {
            try {
                listener.accept(online);
            } catch (Exception e) {
                com.app.infrastructure.util.DailyLogger.logError("AppState", "Error notifying status listener", e);
            }
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public String getAccessToken() {
        return accessToken.get();
    }

    public ReadOnlyStringProperty accessTokenProperty() {
        return accessToken;
    }

    public void setAccessToken(String token) {
        accessToken.set(token == null ? "" : token);
    }

    public void clearSession() {
        accessToken.set("");
    }

    public boolean isAuthenticated() {
        return !accessToken.get().isBlank();
    }
}