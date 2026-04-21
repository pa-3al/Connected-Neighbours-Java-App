package com.app.infrastructure.util;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class KeyboardShortcutsHandler {

    public enum ShortcutAction {
        SAVE,
        NEW_ITEM,
        REFRESH,
        DELETE_ITEM,
        FIND,
        COMMAND_PALETTE,
        ESCAPE,
        HELP
    }

    @FunctionalInterface
    public interface ShortcutActionHandler {
        boolean handle(ShortcutAction action, Node focusOwner);
    }

    private static final Map<Node, ShortcutActionHandler> CONTEXT_HANDLERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final KeyCombination SHORTCUT_A = new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SHORTCUT_C = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SHORTCUT_V = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SHORTCUT_X = new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SHORTCUT_Z = new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SHORTCUT_Y = new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SHORTCUT_F = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SHORTCUT_K = new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SHORTCUT_S = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SHORTCUT_N = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SHORTCUT_R = new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination DELETE_KEY = new KeyCodeCombination(KeyCode.DELETE);
    private static final KeyCombination ESCAPE_KEY = new KeyCodeCombination(KeyCode.ESCAPE);
    private static final KeyCombination F1_KEY = new KeyCodeCombination(KeyCode.F1);
    private static final KeyCombination F5_KEY = new KeyCodeCombination(KeyCode.F5);

    public static void registerContext(Node rootNode, ShortcutActionHandler handler) {
        if (rootNode == null || handler == null) {
            return;
        }
        CONTEXT_HANDLERS.put(rootNode, handler);
    }

    public static void unregisterContext(Node rootNode) {
        if (rootNode == null) {
            return;
        }
        CONTEXT_HANDLERS.remove(rootNode);
    }

    public static void attachTo(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Node focusOwner = scene.getFocusOwner() != null ? scene.getFocusOwner() : scene.getRoot();
            if (SHORTCUT_A.match(event)) {
                handleSelectAll(focusOwner, event);
            } else if (SHORTCUT_C.match(event)) {
                handleCopy(focusOwner, event);
            } else if (SHORTCUT_V.match(event)) {
                handlePaste(focusOwner, event);
            } else if (SHORTCUT_X.match(event)) {
                handleCut(focusOwner, event);
            } else if (SHORTCUT_Z.match(event)) {
                handleUndo(focusOwner, event);
            } else if (SHORTCUT_Y.match(event)) {
                handleRedo(focusOwner, event);
            } else if (SHORTCUT_F.match(event)) {
                handleFind(focusOwner, event);
            } else if (SHORTCUT_K.match(event)) {
                handleCommandPalette(focusOwner, event);
            } else if (SHORTCUT_S.match(event)) {
                handleContextAction(ShortcutAction.SAVE, focusOwner, event);
            } else if (SHORTCUT_N.match(event)) {
                handleContextAction(ShortcutAction.NEW_ITEM, focusOwner, event);
            } else if (SHORTCUT_R.match(event) || F5_KEY.match(event)) {
                handleContextAction(ShortcutAction.REFRESH, focusOwner, event);
            } else if (DELETE_KEY.match(event)) {
                handleDelete(focusOwner, event);
            } else if (ESCAPE_KEY.match(event)) {
                handleContextAction(ShortcutAction.ESCAPE, focusOwner, event);
            } else if (F1_KEY.match(event)) {
                handleContextAction(ShortcutAction.HELP, focusOwner, event);
            }
        });
    }

    private static void handleContextAction(ShortcutAction action, Node focusOwner, KeyEvent event) {
        if (dispatchContextAction(action, focusOwner)) {
            event.consume();
        }
    }

    private static boolean dispatchContextAction(ShortcutAction action, Node focusOwner) {
        Node current = focusOwner;
        while (current != null) {
            ShortcutActionHandler handler = CONTEXT_HANDLERS.get(current);
            if (handler != null && handler.handle(action, focusOwner)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static void handleSelectAll(Node focusOwner, KeyEvent event) {
        if (focusOwner instanceof TextInputControl textInputControl) {
            textInputControl.selectAll();
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
        if (focusOwner instanceof TextInputControl textInputControl) {
            textInputControl.copy();
            event.consume();
        } else if (focusOwner instanceof ListView) {
            copySelectionToClipboard((ListView<?>) focusOwner);
            event.consume();
        }
    }

    private static void handlePaste(Node focusOwner, KeyEvent event) {
        if (focusOwner instanceof TextInputControl textInputControl) {
            textInputControl.paste();
            event.consume();
        }
    }

    private static void handleCut(Node focusOwner, KeyEvent event) {
        if (focusOwner instanceof TextInputControl textInputControl) {
            textInputControl.cut();
            event.consume();
        }
    }

    private static void handleUndo(Node focusOwner, KeyEvent event) {
        if (focusOwner instanceof TextInputControl textInputControl) {
            textInputControl.undo();
            event.consume();
        }
    }

    private static void handleRedo(Node focusOwner, KeyEvent event) {
        if (focusOwner instanceof TextInputControl textInputControl) {
            textInputControl.redo();
            event.consume();
        }
    }

    private static void handleFind(Node focusOwner, KeyEvent event) {
        if (dispatchContextAction(ShortcutAction.FIND, focusOwner)) {
            event.consume();
            return;
        }
        com.app.infrastructure.util.DailyLogger.logWarn("Shortcuts", "Ctrl+F: Search functionality not yet implemented globally.");
        event.consume();
    }

    private static void handleCommandPalette(Node focusOwner, KeyEvent event) {
        if (dispatchContextAction(ShortcutAction.COMMAND_PALETTE, focusOwner)) {
            event.consume();
            return;
        }
        com.app.infrastructure.util.DailyLogger.logWarn("Shortcuts", "Ctrl+K: Command Palette triggered.");
        event.consume();
    }

    private static void handleDelete(Node focusOwner, KeyEvent event) {
        if (focusOwner instanceof TextInputControl) {
            return;
        }
        if (dispatchContextAction(ShortcutAction.DELETE_ITEM, focusOwner)) {
            event.consume();
        }
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
