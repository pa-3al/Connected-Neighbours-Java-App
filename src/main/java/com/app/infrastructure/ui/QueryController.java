package com.app.infrastructure.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class QueryController implements Initializable {

    @FXML private TextField  queryInput;
    @FXML private ScrollPane resultsScroll;
    @FXML private TextFlow   resultsFlow;
    @FXML private Button     executeButton;
    @FXML private Label      promptLabel;
    @FXML private Button     reconnectButton;

    private Process        pythonProcess;
    private BufferedWriter processIn;
    private BufferedReader processOut;
    private boolean        isProcessRunning = false;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "python-io");
        t.setDaemon(true);
        return t;
    });

    private static final String END_MARKER    = "<<<END>>>";
    private static final String PROMPT_PREFIX = "PROMPT:";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        queryInput.setOnAction(e -> executeQuery());
        startPythonProcess();
    }

    private void startPythonProcess() {
        try {
            String exePath = extractInterpreter();

            ProcessBuilder pb = new ProcessBuilder(exePath);
            pb.environment().put("PYTHONIOENCODING", "UTF-8");
            pb.redirectErrorStream(true);
            pb.directory(resolveWorkDir().toFile());

            pythonProcess = pb.start();
            isProcessRunning = true;

            Platform.runLater(() -> toggleReconnectButton(false));

            processIn  = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream(), StandardCharsets.UTF_8));
            processOut = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream(), StandardCharsets.UTF_8));

            ioExecutor.submit(this::readUntilEnd);

            pythonProcess.onExit().thenRun(() -> Platform.runLater(this::handleProcessExit));

        } catch (Exception e) {
            appendResult("Impossible de démarrer l'interpréteur : " + e.getMessage());
            Platform.runLater(this::handleProcessExit);
        }
    }

    private String extractInterpreter() throws Exception {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String  resName   = isWindows ? "interpreter/interpreter.exe" : "interpreter/interpreter";
        String  exeName   = isWindows ? "interpreter.exe" : "interpreter";

        Path targetDir = resolveWorkDir().resolve("bin");
        Files.createDirectories(targetDir);
        Path targetExe = targetDir.resolve(exeName);

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resName)) {
            if (in == null) {
                throw new FileNotFoundException("Ressource introuvable dans le jar : " + resName);
            }
            Files.copy(in, targetExe, StandardCopyOption.REPLACE_EXISTING);
        }

        if (!isWindows) {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(targetExe);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            Files.setPosixFilePermissions(targetExe, perms);
        }

        return targetExe.toAbsolutePath().toString();
    }

    private Path resolveWorkDir() throws IOException {
        Path dir = Path.of(System.getProperty("user.home"), ".voisinea");
        Files.createDirectories(dir);
        return dir;
    }

    @FXML
    private void executeQuery() {
        String command = queryInput.getText().trim();
        if (command.isEmpty()) return;

        if (!command.endsWith(";")) command += ";";

        appendResult("\n> " + command);
        queryInput.clear();
        setInputEnabled(false);

        final String cmd = command;
        ioExecutor.submit(() -> {
            try {
                processIn.write(cmd + "\n");
                processIn.flush();
                readUntilEnd();
            } catch (IOException e) {
                Platform.runLater(() -> appendResult("Erreur d'envoi : " + e.getMessage()));
            } finally {
                Platform.runLater(() -> setInputEnabled(true));
            }
        });
    }

    @FXML
    private void clearConsole() {
        resultsFlow.getChildren().clear();
        queryInput.requestFocus();
    }

    private void readUntilEnd() {
        StringBuilder output = new StringBuilder();
        String        prompt = null;

        try {
            String line;
            while ((line = processOut.readLine()) != null) {
                if (line.equals(END_MARKER)) break;
                if (line.startsWith(PROMPT_PREFIX)) {
                    prompt = line.substring(PROMPT_PREFIX.length());
                } else {
                    output.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            output.append("Erreur de lecture : ").append(e.getMessage());
        }

        final String text = output.toString().trim();
        final String p    = prompt;

        Platform.runLater(() -> {
            if (!text.isEmpty()) appendResult(text);
            if (p != null)       updatePrompt(p);
        });
    }

    private void appendResult(String text) {
        Text node = new Text(text + "\n");
        node.getStyleClass().add("log-text");

        node.setStyle("-fx-fill: -fx-color-text;");

        resultsFlow.getChildren().add(node);
        Platform.runLater(() -> resultsScroll.setVvalue(1.0));
    }

    private void updatePrompt(String prompt) {
        if (promptLabel != null) promptLabel.setText(prompt);
        queryInput.setPromptText(prompt);
    }

    private void setInputEnabled(boolean enabled) {
        queryInput.setDisable(!enabled);
        executeButton.setDisable(!enabled);
    }

    public void shutdown() {
        ioExecutor.shutdownNow();
        if (pythonProcess != null && pythonProcess.isAlive()) {
            try {
                processIn.write("exit;\n");
                processIn.flush();
                processIn.close();
            } catch (IOException ignored) {
            }
            try {
                if (!pythonProcess.waitFor(2, TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pythonProcess.destroyForcibly();
            }
        }
    }

    private void handleProcessExit() {
        isProcessRunning = false;
        setInputEnabled(false);
        toggleReconnectButton(true);
        appendResult("\n[Interpréteur déconnecté. Cliquez sur 'Relancer' pour redémarrer]");

        // Fermeture propre des flux locaux devenus obsolètes
        try { if (processIn != null) processIn.close(); } catch (IOException ignored) {}
        try { if (processOut != null) processOut.close(); } catch (IOException ignored) {}
    }

    @FXML
    private void handleReconnect() {
        appendResult("\n[Relancement de l'interpréteur...]");
        startPythonProcess();
    }

    private void toggleReconnectButton(boolean showReconnect) {
        if (reconnectButton != null) {
            reconnectButton.setVisible(showReconnect);
            reconnectButton.setManaged(showReconnect);
        }
        // Si le processus tourne, on réactive les inputs
        if (!showReconnect) {
            setInputEnabled(true);
        }
    }
}