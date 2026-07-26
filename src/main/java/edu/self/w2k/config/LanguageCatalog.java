package edu.self.w2k.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import edu.self.w2k.write.DictionaryTitles;
import lombok.extern.slf4j.Slf4j;

/**
 * The two language lists the GUI's dropdowns offer.
 * <p>
 * They are deliberately different. A <em>Wiktionary edition</em> must be one kaikki.org actually
 * publishes a dump for, so it comes from a curated resource file. A <em>word language</em> is just a
 * filter over whatever the dump contains, so it comes from the JDK's full ISO 639-1 set.
 */
@Slf4j
public final class LanguageCatalog {

    static final String EDITIONS_RESOURCE = "/kaikki-editions.properties";
    private static final String EDITIONS_KEY = "editions";

    /** A language code paired with its English name, for display in a dropdown. */
    public record Language(String code, String displayName) implements Comparable<Language> {

        public static Language of(String code) {
            return new Language(code, DictionaryTitles.displayName(code));
        }

        @Override
        public int compareTo(Language other) {
            return displayName.compareToIgnoreCase(other.displayName);
        }

        /** What the dropdown shows, e.g. {@code "Modern Greek (el)"}. */
        @Override
        public String toString() {
            return "%s (%s)".formatted(displayName, code);
        }
    }

    private LanguageCatalog() {}

    /**
     * Wiktionary editions kaikki.org serves, sorted by English name.
     * <p>
     * Returns an empty list if the resource is missing or malformed: the dropdown is a convenience,
     * and a code can always be typed in by hand, so a packaging mistake here must not stop the app
     * from working.
     */
    public static List<Language> editions() {
        Properties props = new Properties();
        try (InputStream in = LanguageCatalog.class.getResourceAsStream(EDITIONS_RESOURCE)) {
            if (in == null) {
                log.warn("{} not found on the classpath; edition list will be empty", EDITIONS_RESOURCE);
                return List.of();
            }
            props.load(in);
        }
        catch (IOException e) {
            log.warn("Could not read {}: {}", EDITIONS_RESOURCE, e.getLocalizedMessage());
            return List.of();
        }

        return parseCodes(props.getProperty(EDITIONS_KEY));
    }

    static List<Language> parseCodes(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::strip)
                .filter(code -> !code.isEmpty())
                .distinct()
                .map(Language::of)
                .sorted()
                .toList();
    }

    /**
     * Every ISO 639-1 language the JDK knows, sorted by English name — the candidate set for the word
     * language filter, since any language may appear in any edition's dump.
     */
    public static List<Language> wordLanguages() {
        return Arrays.stream(Locale.getISOLanguages())
                .map(Language::of)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
