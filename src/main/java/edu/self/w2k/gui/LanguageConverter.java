package edu.self.w2k.gui;

import java.util.List;
import java.util.function.Supplier;

import edu.self.w2k.config.LanguageCatalog;
import edu.self.w2k.config.LanguageCatalog.Language;
import javafx.util.StringConverter;

/**
 * Renders a language as {@code "Name (code)"}, and resolves that form, a bare code or a display name
 * back to one of the languages the picker currently offers.
 * <p>
 * Text matching none of them resolves to {@code null}. Both pickers accept typing so their lists can
 * be filtered, which puts this on the path of everything a user types: minting a {@code Language} from
 * an unrecognised code here is what once let an edition kaikki does not serve reach the downloader and
 * fail at the HTTP 404.
 */
public class LanguageConverter extends StringConverter<Language> {

    private final Supplier<List<Language>> candidates;

    public LanguageConverter() {
        this(LanguageCatalog::editions);
    }

    public LanguageConverter(Supplier<List<Language>> candidates) {
        this.candidates = candidates;
    }

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

        return LanguageCatalog.find(candidates.get(), code).orElse(null);
    }
}
