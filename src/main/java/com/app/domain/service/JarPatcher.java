package com.app.domain.service;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
final class JarPatcher {
    private static final String DELETE_LIST = "META-INF/update-delete.list";
    private JarPatcher() {
    }
    static void apply(Path baseJar, Path patchJar, Path targetJar) throws IOException {
        Path targetParent = targetJar.toAbsolutePath().getParent();
        if (targetParent != null) {
            Files.createDirectories(targetParent);
        }
        Path tempJar = Files.createTempFile(targetParent, targetJar.getFileName().toString(), ".tmp");
        try (JarFile base = new JarFile(baseJar.toFile());
             JarFile patch = new JarFile(patchJar.toFile());
             JarOutputStream output = new JarOutputStream(Files.newOutputStream(tempJar))) {
            Set<String> deletedEntries = readDeletedEntries(patch);
            Set<String> patchedEntries = patch.stream()
                .filter(entry -> !entry.isDirectory())
                .map(JarEntry::getName)
                .filter(name -> !DELETE_LIST.equals(name))
                .collect(Collectors.toSet());
            Set<String> writtenEntries = new HashSet<>();
            for (JarEntry entry : java.util.Collections.list(base.entries())) {
                if (entry.isDirectory() || deletedEntries.contains(entry.getName()) || patchedEntries.contains(entry.getName())) {
                    continue;
                }
                copyEntry(base, entry, output, writtenEntries);
            }
            for (JarEntry entry : java.util.Collections.list(patch.entries())) {
                if (entry.isDirectory() || DELETE_LIST.equals(entry.getName())) {
                    continue;
                }
                copyEntry(patch, entry, output, writtenEntries);
            }
        } catch (IOException e) {
            Files.deleteIfExists(tempJar);
            throw e;
        }
        try {
            Files.move(tempJar, targetJar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tempJar, targetJar, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    private static Set<String> readDeletedEntries(JarFile patch) throws IOException {
        JarEntry deleteList = patch.getJarEntry(DELETE_LIST);
        if (deleteList == null) {
            return Set.of();
        }
        try (InputStream input = patch.getInputStream(deleteList)) {
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return content.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("#"))
                .collect(Collectors.toSet());
        }
    }
    private static void copyEntry(JarFile source, JarEntry entry, JarOutputStream output, Set<String> writtenEntries) throws IOException {
        if (!writtenEntries.add(entry.getName())) {
            return;
        }
        JarEntry copy = new JarEntry(entry.getName());
        copy.setTime(entry.getTime());
        output.putNextEntry(copy);
        try (InputStream input = source.getInputStream(entry)) {
            input.transferTo(output);
        }
        output.closeEntry();
    }
}
