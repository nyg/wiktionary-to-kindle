package edu.self.w2k.gui;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

import edu.self.w2k.config.LanguageCatalog;
import edu.self.w2k.config.LanguageCatalog.Language;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class ComboBoxSearch {

    static final long RESET_AFTER_NANOS = 1_000_000_000L;

    private ComboBoxSearch() {}

    public static void install(ComboBox<Language> combo) {
        Buffer buffer = new Buffer();

        combo.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.BACK_SPACE) {
                select(combo, backspaced(buffer, combo.getItems()));
                event.consume();
            }
            else if (event.getCode() == KeyCode.ESCAPE) {
                buffer.clear();
            }
        });

        combo.addEventHandler(KeyEvent.KEY_TYPED, event -> {
            if (select(combo, typed(buffer, combo.getItems(), event.getCharacter()))) {
                event.consume();
            }
        });
    }

    static Optional<Language> typed(Buffer buffer, List<Language> items, String character) {
        if (!isPrintable(character)) {
            return Optional.empty();
        }
        buffer.append(character);
        return match(items, buffer.text());
    }

    static Optional<Language> backspaced(Buffer buffer, List<Language> items) {
        buffer.backspace();
        return match(items, buffer.text());
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

    private static boolean select(ComboBox<Language> combo, Optional<Language> match) {
        match.ifPresent(language -> {
            combo.setValue(language);
            combo.getSelectionModel().select(language);
        });
        return match.isPresent();
    }

    private static boolean isPrintable(String character) {
        return character != null && !character.isEmpty() && character.charAt(0) >= ' ';
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

    static final class Buffer {

        private final StringBuilder text = new StringBuilder();
        private final LongSupplier clock;
        private long lastKeystroke;

        Buffer() {
            this(System::nanoTime);
        }

        Buffer(LongSupplier clock) {
            this.clock = clock;
        }

        void append(String typed) {
            expireIfStale();
            text.append(typed);
            lastKeystroke = now();
        }

        void backspace() {
            expireIfStale();
            if (!text.isEmpty()) {
                text.setLength(text.length() - 1);
            }
            lastKeystroke = now();
        }

        void clear() {
            text.setLength(0);
        }

        String text() {
            return text.toString();
        }

        private long now() {
            return clock.getAsLong();
        }

        private void expireIfStale() {
            if (now() - lastKeystroke > RESET_AFTER_NANOS) {
                clear();
            }
        }
    }
}
