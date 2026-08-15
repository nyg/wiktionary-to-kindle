package edu.self.w2k.kaikki;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import edu.self.w2k.config.LanguageCatalog.Language;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LanguageCodeResolverTest {

    @Test
    void should_resolve_a_localised_name_when_the_jdk_knows_it() {
        // When / Then
        assertThat(LanguageCodeResolver.codeFor("fr", "Anglais")).contains("en");
        assertThat(LanguageCodeResolver.codeFor("de", "Englisch")).contains("en");
        assertThat(LanguageCodeResolver.codeFor("en", "Latin")).contains("la");
    }

    @Test
    void should_resolve_a_name_outside_iso_639_1_when_the_alias_table_covers_it() {
        // When / Then
        assertThat(LanguageCodeResolver.codeFor("en", "Ancient Greek")).contains("grc");
        assertThat(LanguageCodeResolver.codeFor("en", "Translingual")).contains("mul");
        assertThat(LanguageCodeResolver.codeFor("fr", "Grec ancien")).contains("grc");
    }

    @Test
    void should_prefer_the_alias_table_when_the_jdk_resolves_a_name_to_a_deprecated_code() {
        // When / Then — Locale.getISOLanguages() still carries ji, while kaikki emits yi
        assertThat(LanguageCodeResolver.codeFor("en", "Yiddish")).contains("yi");
    }

    @Test
    void should_resolve_a_name_the_jdk_spells_differently_when_the_alias_table_covers_it() {
        // When / Then
        assertThat(LanguageCodeResolver.codeFor("en", "Bengali")).contains("bn");
        assertThat(LanguageCodeResolver.codeFor("de", "Weißrussisch")).contains("be");
    }

    @Test
    void should_resolve_nothing_when_the_name_is_unknown() {
        // When
        Optional<String> code = LanguageCodeResolver.codeFor("fr", "Langue Imaginaire");

        // Then
        assertThat(code).isEmpty();
    }

    @Test
    void should_drop_unresolvable_names_when_building_the_picker_list() {
        // Given
        List<KaikkiLanguage> scraped = List.of(
                new KaikkiLanguage("Anglais", 224552),
                new KaikkiLanguage("Langue Imaginaire", 99));

        // When
        List<Language> languages = LanguageCodeResolver.toLanguages("fr", scraped);

        // Then
        assertThat(languages).extracting(Language::code).containsExactly("en");
    }

    @Test
    void should_sort_by_sense_count_when_building_the_picker_list() {
        // Given
        List<KaikkiLanguage> scraped = List.of(
                new KaikkiLanguage("Russe", 363095),
                new KaikkiLanguage("Anglais", 224552),
                new KaikkiLanguage("Allemand", 2200333));

        // When
        List<Language> languages = LanguageCodeResolver.toLanguages("fr", scraped);

        // Then
        assertThat(languages).extracting(Language::code).containsExactly("de", "ru", "en");
        assertThat(languages).extracting(Language::senses).containsExactly(2200333L, 363095L, 224552L);
    }

    @Test
    void should_keep_the_larger_entry_when_two_names_resolve_to_one_code() {
        // Given
        List<KaikkiLanguage> scraped = List.of(
                new KaikkiLanguage("Anglais", 224552),
                new KaikkiLanguage("Anglais", 12));

        // When
        List<Language> languages = LanguageCodeResolver.toLanguages("fr", scraped);

        // Then
        assertThat(languages).singleElement()
                .extracting(Language::senses)
                .isEqualTo(224552L);
    }

    @Test
    void should_fall_back_to_the_kaikki_name_when_the_jdk_cannot_name_the_code() {
        // Given
        List<KaikkiLanguage> scraped = List.of(new KaikkiLanguage("Kotava", 100248));

        // When
        List<Language> languages = LanguageCodeResolver.toLanguages("fr", scraped);

        // Then
        assertThat(languages).singleElement()
                .extracting(Language::displayName)
                .isEqualTo("Kotava");
    }

    @Test
    void should_ignore_case_accents_and_punctuation_when_normalising_a_name() {
        // When / Then
        assertThat(LanguageCodeResolver.normalise("Grec ancien")).isEqualTo("grecancien");
        assertThat(LanguageCodeResolver.normalise("K'iche'")).isEqualTo("kiche");
        assertThat(LanguageCodeResolver.normalise("Norvégien (bokmål)")).isEqualTo("norvegienbokmal");
    }

    @Test
    void should_return_nothing_when_asked_for_a_blank_name() {
        // When / Then
        assertThat(LanguageCodeResolver.codeFor("fr", "  ")).isEmpty();
        assertThat(LanguageCodeResolver.toLanguages(null, List.of())).isEmpty();
    }
}
