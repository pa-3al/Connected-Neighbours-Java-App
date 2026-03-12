package com.app.infrastructure.ui;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.app.domain.model.AuthResult;
import com.app.domain.port.in.AuthUseCase;
import com.app.infrastructure.i18n.I18nService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class LoginController {
    private final AuthUseCase authUseCase;
    private final Consumer<String> onAuthenticated;
    private final I18nService i18n = I18nService.getInstance();

    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label twoFactorLabel;
    @FXML
    private TextField twoFactorField;
    @FXML
    private HBox twoFactorBox;
    @FXML
    private Button loginButton;
    @FXML
    private Button ssoButton;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressIndicator loadingIndicator;

    private boolean awaitingTwoFactor;

    public LoginController(AuthUseCase authUseCase, Consumer<String> onAuthenticated) {
        this.authUseCase = authUseCase;
        this.onAuthenticated = onAuthenticated;
    }

    @FXML
    public void initialize() {
        bindI18n();
        setBusy(false);
        setAwaitingTwoFactor(false);
    }

    private void bindI18n() {
        titleLabel.textProperty().bind(i18n.createStringBinding("auth.login.title"));
        subtitleLabel.textProperty().bind(i18n.createStringBinding("auth.login.subtitle"));
        emailField.promptTextProperty().bind(i18n.createStringBinding("auth.login.email"));
        passwordField.promptTextProperty().bind(i18n.createStringBinding("auth.login.password"));
        twoFactorLabel.textProperty().bind(i18n.createStringBinding("auth.login.code"));
        ssoButton.textProperty().bind(i18n.createStringBinding("auth.login.sso"));

        i18n.localeProperty().addListener((obs, oldValue, newValue) -> updateLoginButtonText());
        updateLoginButtonText();
    }

    @FXML
    public void handleLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String code = twoFactorField.getText() == null ? "" : twoFactorField.getText().trim();

        if (email.isBlank() || password.isBlank()) {
            setStatus(i18n.get("auth.login.error.missing"), true);
            return;
        }

        if (awaitingTwoFactor && code.isBlank()) {
            setStatus(i18n.get("auth.login.error.code_required"), true);
            return;
        }

        setBusy(true);
        setStatus(i18n.get("auth.login.wait"), false);

        CompletableFuture
            .supplyAsync(() -> awaitingTwoFactor
                ? authUseCase.loginWith2FA(email, password, code)
                : authUseCase.login(email, password))
            .thenAccept(result -> Platform.runLater(() -> handleLoginResult(result)))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    setBusy(false);
                    setStatus(resolveErrorMessage(ex), true);
                });
                return null;
            });
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

    private void handleLoginResult(AuthResult result) {
        setBusy(false);

        if (result.twoFactorRequired()) {
            setAwaitingTwoFactor(true);
            setStatus(i18n.get("auth.login.twofactor.required"), false);
            twoFactorField.requestFocus();
            return;
        }

        if (result.isAuthenticated()) {
            setStatus(i18n.get("auth.login.success"), false);
            onAuthenticated.accept(result.accessToken());
            return;
        }

        setStatus(i18n.get("auth.login.error.unexpected"), true);
    }

    private void setAwaitingTwoFactor(boolean awaitingTwoFactor) {
        this.awaitingTwoFactor = awaitingTwoFactor;
        twoFactorBox.setVisible(awaitingTwoFactor);
        twoFactorBox.setManaged(awaitingTwoFactor);
        updateLoginButtonText();
    }

    private void updateLoginButtonText() {
        loginButton.setText(i18n.get(awaitingTwoFactor ? "auth.login.button.2fa" : "auth.login.button"));
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
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
        if (lower.contains("invalid credentials")) {
            return i18n.get("auth.login.error.invalid");
        }
        if (lower.contains("2fa")) {
            return i18n.get("auth.login.error.invalid_2fa");
        }
        if (lower.contains("sso")) {
            return i18n.get("auth.login.error.sso");
        }

        return message;
    }
}
