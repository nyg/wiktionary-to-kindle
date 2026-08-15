package edu.self.w2k.gui;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import edu.self.w2k.config.LanguageCatalog;
import edu.self.w2k.config.LanguageCatalog.Language;

/**
 * The word language picker's renderer: {@code "French (fr) — 2.7M senses"}.
 * <p>
 * The sense count trails the code, so the inherited {@code fromString} still finds the code in the
 * last parenthesised group and resolves the decorated form back.
 */
public class WordLanguageConverter extends LanguageConverter {

    public WordLanguageConverter() {
        super(LanguageCatalog::wordLanguages);
    }

    public WordLanguageConverter(Supplier<List<Language>> candidates) {
        super(candidates);
    }

    @Override
    public String toString(Language language) {
        if (language == null) {
            return "";
        }
        if (language.senses() <= 0) {
            return language.toString();
        }
        return "%s — %s senses".formatted(language, formatSenses(language.senses()));
    }

    static String formatSenses(long senses) {
        if (senses >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", senses / 1_000_000d);
        }
        if (senses >= 1_000) {
            return String.format(Locale.ROOT, "%dk", Math.round(senses / 1_000d));
        }
        return Long.toString(senses);
    }
}
