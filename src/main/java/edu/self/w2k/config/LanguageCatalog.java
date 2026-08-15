package edu.self.w2k.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import edu.self.w2k.write.DictionaryTitles;
import lombok.extern.slf4j.Slf4j;

/**
 * The two language lists the GUI's dropdowns offer when kaikki.org cannot be reached.
 * <p>
 * Both are fallbacks. {@code KaikkiCatalog} fetches the live edition list and the selected edition's
 * own language list at startup; these bundled lists are what the dropdowns show until that returns,
 * and all they show if it never does.
 */
@Slf4j
public final class LanguageCatalog {

    static final String EDITIONS_RESOURCE = "/kaikki-editions.properties";
    private static final String EDITIONS_KEY = "editions";
    private static final String NAME_OVERRIDE_PREFIX = "name.";

    /** A language code paired with a display name, and how many senses the dump holds for it. */
    public record Language(String code, String displayName, long senses) implements Comparable<Language> {

        public static Language of(String code) {
            return new Language(code, displayNameFor(code), 0);
        }

        public static Language of(String code, String displayName, long senses) {
            return new Language(code, displayName, senses);
        }

        @Override
        public int compareTo(Language other) {
            return displayName.compareToIgnoreCase(other.displayName);
        }

        /** What the dropdown shows, e.g. {@code "Greek (el)"}. */
        @Override
        public String toString() {
            return "%s (%s)".formatted(displayName, code);
        }
    }

    private LanguageCatalog() {}

    private static final class Bundled {

        static final Properties PROPERTIES = load();

        static final List<Language> EDITIONS = parseCodes(PROPERTIES.getProperty(EDITIONS_KEY));

        private static Properties load() {
            Properties properties = new Properties();
            try (InputStream in = LanguageCatalog.class.getResourceAsStream(EDITIONS_RESOURCE)) {
                if (in == null) {
                    log.warn("{} not found on the classpath; edition list will be empty", EDITIONS_RESOURCE);
                    return properties;
                }
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
            catch (IOException | IllegalArgumentException e) {
                log.warn("Could not read {}: {}", EDITIONS_RESOURCE, e.getLocalizedMessage());
            }
            return properties;
        }
    }

    /**
     * Wiktionary editions kaikki.org serves, sorted by display name.
     * <p>
     * Returns an empty list if the resource is missing or malformed: a packaging mistake here must
     * leave the app usable, not stop it starting.
     */
    public static List<Language> editions() {
        return Bundled.EDITIONS;
    }

    public static String displayNameFor(String code) {
        String override = Bundled.PROPERTIES.getProperty(NAME_OVERRIDE_PREFIX + code);
        return override == null || override.isBlank() ? DictionaryTitles.displayName(code) : override.strip();
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
     * Every ISO 639-1 language the JDK knows, sorted by display name — what the word language
     * dropdown offers when the selected edition's own list is unavailable.
     */
    public static List<Language> wordLanguages() {
        return Arrays.stream(Locale.getISOLanguages())
                .map(Language::of)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public static Optional<Language> find(List<Language> languages, String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        String trimmed = query.strip();
        return languages.stream()
                .filter(language -> language.code().equalsIgnoreCase(trimmed)
                        || language.displayName().equalsIgnoreCase(trimmed))
                .findFirst();
    }

    public static Optional<Language> findEdition(String input) {
        return find(editions(), input);
    }
}
