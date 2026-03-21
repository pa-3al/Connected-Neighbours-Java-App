package com.app.infrastructure.ui;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.app.domain.port.in.AuthUseCase;
import com.app.infrastructure.i18n.I18nService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

public class LoginController {
    private final AuthUseCase authUseCase;
    private final Consumer<String> onAuthenticated;
    private final I18nService i18n = I18nService.getInstance();

    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private Button ssoButton;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressIndicator loadingIndicator;

    public LoginController(AuthUseCase authUseCase, Consumer<String> onAuthenticated) {
        this.authUseCase = authUseCase;
        this.onAuthenticated = onAuthenticated;
    }

    @FXML
    public void initialize() {
        bindI18n();
        setBusy(false);
    }

    private void bindI18n() {
        titleLabel.textProperty().bind(i18n.createStringBinding("auth.login.title"));
        subtitleLabel.textProperty().bind(i18n.createStringBinding("auth.login.subtitle"));
        ssoButton.textProperty().bind(i18n.createStringBinding("auth.login.sso"));
    }

    @FXML
    public void handleSso() {
        setBusy(true);
        setStatus(i18n.get("auth.login.wait.sso"), false);

        CompletableFuture
                .supplyAsync(authUseCase::loginWithSso)
                .thenAccept(accessToken -> Platform.runLater(() -> {
                    setBusy(false);
                    setStatus(i18n.get("auth.login.success"), false);
                    // Maximize the main window before handing off
                    javafx.stage.Window.getWindows().stream()
                            .filter(w -> w instanceof javafx.stage.Stage && w.isShowing())
                            .map(w -> (javafx.stage.Stage) w)
                            .findFirst()
                            .ifPresent(s -> s.setMaximized(true));
                    onAuthenticated.accept(accessToken);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setBusy(false);
                        setStatus(resolveErrorMessage(ex), true);
                    });
                    return null;
                });
    }

    private void setBusy(boolean busy) {
        ssoButton.setDisable(busy);
        loadingIndicator.setVisible(busy);
        loadingIndicator.setManaged(busy);
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message == null ? "" : message);
        statusLabel.setStyle(isError ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #22c55e;");
    }

    private String resolveErrorMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            return i18n.get("auth.login.error.generic");
        }

        String lower = message.toLowerCase();
        if (lower.contains("sso")) {
            return i18n.get("auth.login.error.sso");
        }

        return message;
    }
}