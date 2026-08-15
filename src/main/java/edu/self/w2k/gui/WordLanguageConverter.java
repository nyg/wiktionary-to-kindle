package edu.self.w2k.gui;

import java.util.Locale;

import edu.self.w2k.config.LanguageCatalog.Language;
import javafx.util.StringConverter;

public class WordLanguageConverter extends StringConverter<Language> {

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

    @Override
    public Language fromString(String text) {
        return null;
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
