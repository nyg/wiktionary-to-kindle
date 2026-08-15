package edu.self.w2k.gui;

import edu.self.w2k.config.LanguageCatalog;
import edu.self.w2k.config.LanguageCatalog.Language;
import javafx.util.StringConverter;

/**
 * Renders a language as {@code "Name (code)"} and parses either that form or a bare code back.
 * <p>
 * Neither picker is editable, so {@code fromString} is not on the path of anything a user types. It
 * stays because {@code StringConverter} requires it, and because FXML and tests round-trip through
 * it.
 */
public class LanguageConverter extends StringConverter<Language> {

    @Override
    public String toString(Language language) {
        return language == null ? "" : language.toString();
    }

    @Override
    public Language fromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.strip();
        int open = trimmed.lastIndexOf('(');
        int close = trimmed.lastIndexOf(')');
        String code = open >= 0 && close > open
                ? trimmed.substring(open + 1, close).strip()
                : trimmed;

        if (code.isBlank()) {
            return null;
        }
        
        return LanguageCatalog.findEdition(code).orElseGet(() -> Language.of(code));
    }
}
