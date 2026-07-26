package edu.self.w2k.write;

import java.util.Locale;

public final class DictionaryTitles {

    private DictionaryTitles() {}

    /**
     * Builds an auto-generated title from the source and target language codes.
     * Example: {@code "fr"} + {@code "en"} → {@code "French–English Dictionary"}.
     */
    public static String autoTitle(String srcLang, String trgLang) {
        return displayName(srcLang) + "–" + displayName(trgLang) + " Dictionary";
    }

    /**
     * The English name of a language code, e.g. {@code "fr"} → {@code "French"}.
     * <p>
     * The exact wording comes from the JDK's CLDR data and can change between releases — {@code "el"}
     * was "Modern Greek" in older JDKs and is "Greek" now — so callers should not depend on a
     * specific string.
     * <p>
     * Falls back to the upper-cased code itself when the JDK has no name for it — either because the
     * code is unknown, or because {@code getDisplayLanguage} echoes the input back unchanged.
     * Shared with the GUI's language dropdowns so labels match the generated titles exactly.
     */
    public static String displayName(String langCode) {
        String name = Locale.forLanguageTag(langCode).getDisplayLanguage(Locale.ENGLISH);
        if (name.isBlank() || name.equalsIgnoreCase(langCode)) {
            return langCode.toUpperCase(Locale.ROOT);
        }
        return name;
    }
}
