package com.app.infrastructure.util;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.Node;
public class KeyboardShortcutsHandler {
    private static final KeyCombination CTRL_A = new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination CTRL_C = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination CTRL_V = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination CTRL_X = new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination CTRL_F = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination CTRL_K = new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN);
    public static void attachTo(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Node focusOwner = scene.getFocusOwner();
            if (CTRL_A.match(event)) {
                handleSelectAll(focusOwner, event);
            } else if (CTRL_C.match(event)) {
                handleCopy(focusOwner, event);
            } else if (CTRL_V.match(event)) {
                handlePaste(focusOwner, event);
            } else if (CTRL_X.match(event)) {
                handleCut(focusOwner, event);
            } else if (CTRL_F.match(event)) {
                 handleFind(focusOwner, event);
            } else if (CTRL_K.match(event)) {
                 handleCommandPalette(focusOwner, event);
            }
        });
    }
    private static void handleSelectAll(Node focusOwner, KeyEvent event) {
        if (focusOwner instanceof TextInputControl) {
            ((TextInputControl) focusOwner).selectAll();
            event.consume();
        } else if (focusOwner instanceof ListView) {
            ((ListView<?>) focusOwner).getSelectionModel().selectAll();
            event.consume();
        } else if (focusOwner instanceof TableView) {
            ((TableView<?>) focusOwner).getSelectionModel().selectAll();
            event.consume();
        }
    }
    private static void handleCopy(Node focusOwner, KeyEvent event) {
        if (focusOwner instanceof TextInputControl) {
            ((TextInputControl) focusOwner).copy();
            event.consume();
        } else if (focusOwner instanceof ListView) {
            copySelectionToClipboard((ListView<?>) focusOwner);
            event.consume();
        }
    }
    private static void handlePaste(Node focusOwner, KeyEvent event) {
        if (focusOwner instanceof TextInputControl) {
            ((TextInputControl) focusOwner).paste();
            event.consume();
        }
    }
    private static void handleCut(Node focusOwner, KeyEvent event) {
        if (focusOwner instanceof TextInputControl) {
            ((TextInputControl) focusOwner).cut();
            event.consume();
        }
    }
    private static void handleFind(Node focusOwner, KeyEvent event) {
        com.app.infrastructure.util.DailyLogger.logWarn("Shortcuts", "Ctrl+F: Search functionality not yet implemented globally.");
        event.consume();
    }
    private static void handleCommandPalette(Node focusOwner, KeyEvent event) {
        com.app.infrastructure.util.DailyLogger.logWarn("Shortcuts", "Ctrl+K: Command Palette triggered.");
        event.consume();
    }
    private static void copySelectionToClipboard(ListView<?> listView) {
        StringBuilder sb = new StringBuilder();
        for (Object item : listView.getSelectionModel().getSelectedItems()) {
            if (sb.length() > 0) sb.append(System.lineSeparator());
            sb.append(item.toString());
        }
        final javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        final javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);
    }
}
