package edu.self.w2k.kaikki;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import edu.self.w2k.config.LanguageCatalog.Language;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class LanguageCodeResolver {

    static final String ALIASES_RESOURCE = "/kaikki-language-codes.properties";

    private static final Map<String, Map<String, String>> CLDR_INDEXES = new ConcurrentHashMap<>();

    private LanguageCodeResolver() {}

    private static final class Aliases {

        static final Properties PROPERTIES = load();

        private static Properties load() {
            Properties properties = new Properties();
            try (InputStream in = LanguageCodeResolver.class.getResourceAsStream(ALIASES_RESOURCE)) {
                if (in == null) {
                    log.debug("{} not found on the classpath; falling back to display-name matching",
                            ALIASES_RESOURCE);
                    return properties;
                }
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
            catch (IOException | IllegalArgumentException e) {
                log.debug("Could not read {}: {}", ALIASES_RESOURCE, e.toString());
            }
            return properties;
        }
    }

    public static List<Language> toLanguages(String edition, List<KaikkiLanguage> languages) {
        if (edition == null || languages == null) {
            return List.of();
        }
        Map<String, Language> byCode = new LinkedHashMap<>();
        for (KaikkiLanguage language : languages) {
            Optional<String> code = codeFor(edition, language.name());
            if (code.isEmpty()) {
                continue;
            }
            Language resolved = Language.of(code.get(), displayNameFor(code.get(), language.name()),
                    language.senses());
            byCode.merge(resolved.code(), resolved,
                    (a, b) -> a.senses() >= b.senses() ? a : b);
        }
        return byCode.values().stream()
                .sorted(Comparator.comparingLong(Language::senses).reversed()
                        .thenComparing(Language::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static Optional<String> codeFor(String edition, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String key = normalise(name);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        String alias = Aliases.PROPERTIES.getProperty(edition + "." + key);
        if (alias != null && !alias.isBlank()) {
            return Optional.of(alias.strip());
        }
        return Optional.ofNullable(cldrIndex(edition).get(key));
    }

    static String normalise(String name) {
        String withoutMarks = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return withoutMarks.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private static String displayNameFor(String code, String kaikkiName) {
        String english = Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH);
        return english.isBlank() || english.equalsIgnoreCase(code) ? kaikkiName : english;
    }

    private static Map<String, String> cldrIndex(String edition) {
        return CLDR_INDEXES.computeIfAbsent(edition, key -> {
            Locale locale = Locale.forLanguageTag("simple".equals(key) ? "en" : key);
            Map<String, String> index = new HashMap<>();
            for (String code : Locale.getISOLanguages()) {
                String name = Locale.forLanguageTag(code).getDisplayLanguage(locale);
                if (!name.isBlank() && !name.equalsIgnoreCase(code)) {
                    index.putIfAbsent(normalise(name), code);
                }
            }
            return Map.copyOf(index);
        });
    }
}
