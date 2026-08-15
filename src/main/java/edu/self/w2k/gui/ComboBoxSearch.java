package edu.self.w2k.gui;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

import edu.self.w2k.config.LanguageCatalog;
import edu.self.w2k.config.LanguageCatalog.Language;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class ComboBoxSearch {

    private static final long RESET_AFTER_NANOS = 1_000_000_000L;

    private ComboBoxSearch() {}

    public static void install(ComboBox<Language> combo) {
        Buffer buffer = new Buffer();

        combo.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.BACK_SPACE) {
                buffer.backspace();
                select(combo, buffer.text());
                event.consume();
            }
            else if (event.getCode() == KeyCode.ESCAPE) {
                buffer.clear();
            }
        });

        combo.addEventHandler(KeyEvent.KEY_TYPED, event -> {
            String typed = event.getCharacter();
            if (typed == null || typed.isEmpty() || typed.charAt(0) < ' ') {
                return;
            }
            buffer.append(typed);
            if (select(combo, buffer.text())) {
                event.consume();
            }
        });
    }

    private static boolean select(ComboBox<Language> combo, String query) {
        Optional<Language> match = match(combo.getItems(), query);
        match.ifPresent(language -> {
            combo.setValue(language);
            combo.getSelectionModel().select(language);
        });
        return match.isPresent();
    }

    static Optional<Language> match(List<Language> items, String query) {
        if (query == null || query.isBlank() || items == null) {
            return Optional.empty();
        }
        return LanguageCatalog.find(items, query)
                .or(() -> firstMatching(items, language -> startsWith(language.displayName(), query)))
                .or(() -> firstMatching(items, language -> startsWith(language.code(), query)))
                .or(() -> firstMatching(items, language -> contains(language.displayName(), query)));
    }

    private static Optional<Language> firstMatching(List<Language> items, Predicate<Language> predicate) {
        return items.stream().filter(predicate).findFirst();
    }

    private static boolean startsWith(String value, String query) {
        return value.toLowerCase(Locale.ROOT).startsWith(query.toLowerCase(Locale.ROOT));
    }

    private static boolean contains(String value, String query) {
        return value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private static final class Buffer {

        private final StringBuilder text = new StringBuilder();
        private long lastKeystroke;

        void append(String typed) {
            expireIfStale();
            text.append(typed);
            lastKeystroke = System.nanoTime();
        }

        void backspace() {
            expireIfStale();
            if (!text.isEmpty()) {
                text.setLength(text.length() - 1);
            }
            lastKeystroke = System.nanoTime();
        }

        void clear() {
            text.setLength(0);
        }

        String text() {
            return text.toString();
        }

        private void expireIfStale() {
            if (System.nanoTime() - lastKeystroke > RESET_AFTER_NANOS) {
                clear();
            }
        }
    }
}
