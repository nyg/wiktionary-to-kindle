package edu.self.w2k.write;

import java.util.Locale;

import edu.self.w2k.config.AppInfo;

public final class DictionaryTitles {

    private DictionaryTitles() {}

    /**
     * Builds an auto-generated title from the source and target language codes.
     * Example: {@code "fr"} + {@code "en"} → {@code "W2K French–English Dictionary"}.
     * <p>
     * The prefix is what makes the dictionary identifiable in the Kindle settings list, where every
     * installed dictionary is listed by this title alone.
     */
    public static String autoTitle(String srcLang, String trgLang) {
        return AppInfo.DICTIONARY_PREFIX + " "
                + displayName(srcLang) + "–" + displayName(trgLang) + " Dictionary";
    }

    /**
     * Shared stem of every file a single generation produces — {@code .mobi}, {@code .opf}, the
     * chapter {@code .html} files, the NCX and the cover.
     * Example: {@code "en"} + {@code "el"} → {@code "w2k-dictionary-en-el"}.
     * <p>
     * All of them are named from this one stem because several dictionaries share a single output
     * directory: fixed names would have each run overwrite the previous one's side-artefacts.
     */
    public static String baseName(String srcLang, String trgLang) {
        return "%s-dictionary-%s-%s"
                .formatted(AppInfo.DICTIONARY_PREFIX, srcLang, trgLang)
                .toLowerCase(Locale.ROOT);
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
