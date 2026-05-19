package com.app.infrastructure.update;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
public class UpdateInstaller {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            return;
        }
        Path currentJar = Paths.get(args[0]);
        Path updateJar = Paths.get(args[1]);
        long parentPid = Long.parseLong(args[2]);
        waitForParent(parentPid);
        replaceJar(currentJar, updateJar);
        restart(currentJar);
    }
    private static void waitForParent(long parentPid) {
        Optional<ProcessHandle> parent = ProcessHandle.of(parentPid);
        parent.ifPresent(handle -> {
            try {
                handle.onExit().get(30, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        });
    }
    private static void replaceJar(Path currentJar, Path updateJar) throws IOException, InterruptedException {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                Files.copy(updateJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException e) {
                lastFailure = e;
                Thread.sleep(1000);
            }
        }
        throw lastFailure;
    }
    private static void restart(Path currentJar) throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = Paths.get(javaHome, "bin", "java").toString();
        new ProcessBuilder(javaBin, "-jar", currentJar.toAbsolutePath().toString())
            .inheritIO()
            .start();
    }
}
