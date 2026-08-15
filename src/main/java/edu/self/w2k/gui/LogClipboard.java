package edu.self.w2k.gui;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public final class LogClipboard {

    private static final KeyCombination COPY =
            new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

    private LogClipboard() {}

    public static void install(ListView<String> logView) {
        logView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        MenuItem copySelection = new MenuItem("Copy");
        copySelection.setOnAction(_ -> copySelectionOf(logView));
        copySelection.disableProperty()
                .bind(Bindings.isEmpty(logView.getSelectionModel().getSelectedIndices()));

        MenuItem copyEverything = new MenuItem("Copy all");
        copyEverything.setOnAction(_ -> copy(textOf(logView.getItems())));
        copyEverything.disableProperty().bind(Bindings.isEmpty(logView.getItems()));

        logView.setContextMenu(new ContextMenu(copySelection, copyEverything));

        logView.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (COPY.match(event)) {
                copySelectionOf(logView);
                event.consume();
            }
        });
    }

    static void copySelectionOf(ListView<String> logView) {
        copy(textOf(logView.getSelectionModel().getSelectedItems()));
    }

    static String textOf(List<String> lines) {
        return lines.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static void copy(String text) {
        if (text.isEmpty()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
