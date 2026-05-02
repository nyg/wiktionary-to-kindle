package edu.self.w2k.write;

import java.util.Locale;

public final class DictionaryTitles {

    private DictionaryTitles() {}

    /**
     * Builds an auto-generated title from the source and target language codes.
     * Example: {@code "el"} + {@code "en"} → {@code "Modern Greek–English Dictionary"}.
     */
    public static String autoTitle(String srcLang, String trgLang) {
        String src = Locale.forLanguageTag(srcLang).getDisplayLanguage(Locale.ENGLISH);
        String trg = Locale.forLanguageTag(trgLang).getDisplayLanguage(Locale.ENGLISH);
        if (src.isBlank() || src.equalsIgnoreCase(srcLang)) {
            src = srcLang.toUpperCase(Locale.ROOT);
        }
        if (trg.isBlank() || trg.equalsIgnoreCase(trgLang)) {
            trg = trgLang.toUpperCase(Locale.ROOT);
        }
        return src + "–" + trg + " Dictionary";
    }
}
