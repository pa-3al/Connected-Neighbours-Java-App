package com.app.domain.service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
class JarPatcherTest {
    @TempDir
    Path tempDir;
    @Test
    void applyShouldMergeChangedAddedAndDeletedEntries() throws Exception {
        Path baseJar = tempDir.resolve("base.jar");
        Path patchJar = tempDir.resolve("patch.jar");
        Path targetJar = tempDir.resolve("target.jar");
        writeJar(baseJar, Map.of(
            "unchanged.txt", "same",
            "changed.txt", "old",
            "deleted.txt", "remove"
        ));
        writeJar(patchJar, Map.of(
            "changed.txt", "new",
            "added.txt", "added",
            "META-INF/update-delete.list", "deleted.txt\n"
        ));
        JarPatcher.apply(baseJar, patchJar, targetJar);
        assertEquals("same", readEntry(targetJar, "unchanged.txt"));
        assertEquals("new", readEntry(targetJar, "changed.txt"));
        assertEquals("added", readEntry(targetJar, "added.txt"));
        assertNull(readEntry(targetJar, "deleted.txt"));
    }
    private void writeJar(Path jarPath, Map<String, String> entries) throws IOException {
        try (JarOutputStream output = new JarOutputStream(java.nio.file.Files.newOutputStream(jarPath))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                output.putNextEntry(new JarEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
    }
    private String readEntry(Path jarPath, String name) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry(name);
            if (entry == null) {
                return null;
            }
            try (java.io.InputStream input = jar.getInputStream(entry)) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }
}
