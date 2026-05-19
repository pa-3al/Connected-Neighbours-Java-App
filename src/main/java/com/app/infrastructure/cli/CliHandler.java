package com.app.infrastructure.cli;
import com.app.domain.model.PluginMetadata;
import com.app.domain.model.UpdateInfo;
import com.app.infrastructure.di.ServiceContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
public class CliHandler {
    private final ServiceContext context;
    public CliHandler(ServiceContext context) {
        this.context = context;
    }
    public void handle(String[] args) {
        if (args.length == 0) return;
        String command = args[0];
        switch (command) {
            case "--parse":
                handleParse(args);
                break;
            case "--export":
                handleExport(args);
                break;
            case "--check-update":
                handleCheckUpdate();
                break;
            default:
                System.out.println("Unknown command: " + command);
                printUsage();
        }
    }
    private void handleParse(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: --parse <file>");
            return;
        }
        String filePath = args[1];
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            List<String> results = context.getQueryEngine().parseAndRun(content);
            System.out.println("--- Execution Results ---");
            for (String result : results) {
                System.out.println(result);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
    private void handleExport(String[] args) {
        String format = "json";
        String outFile = "export.json";
        for (int i = 1; i < args.length; i++) {
            if ("--format".equals(args[i]) && i + 1 < args.length) {
                format = args[i + 1];
                i++;
            } else if ("--out".equals(args[i]) && i + 1 < args.length) {
                outFile = args[i + 1];
                i++;
            }
        }
        System.out.println("Exporting plugins to " + outFile + " in format " + format + "...");
        List<PluginMetadata> plugins = context.getPluginService().getInstalledPlugins();
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < plugins.size(); i++) {
            PluginMetadata p = plugins.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": \"").append(p.id()).append("\",\n");
            sb.append("    \"name\": \"").append(p.name()).append("\",\n");
            sb.append("    \"version\": \"").append(p.version()).append("\"\n");
            sb.append("  }");
            if (i < plugins.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        try {
            Files.write(Paths.get(outFile), sb.toString().getBytes());
            System.out.println("Export completed successfully.");
        } catch (IOException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }
    private void handleCheckUpdate() {
        System.out.println("Checking for updates...");
        try {
            UpdateInfo info = context.getUpdateService().checkForUpdates().get();
            if (info != null) {
                System.out.println("Update available: v" + info.version());
                System.out.println("Download URL: " + info.downloadUrl());
                System.out.println("Run with graphical interface to update.");
            } else {
                System.out.println("No updates available. You are on the latest version (" + context.getUpdateService().getCurrentVersion() + ").");
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to check updates: " + e.getMessage());
        }
    }
    private void printUsage() {
        System.out.println("Available commands:");
        System.out.println("  --parse <file>                     Executes a script");
        System.out.println("  --export --format <type> --out <file>  Exports plugin data");
        System.out.println("  --check-update                     Checks for application updates");
    }
}
