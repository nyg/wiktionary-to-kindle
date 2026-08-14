package edu.self.w2k.config;

import static org.assertj.core.api.Assertions.assertThat;

import edu.self.w2k.config.LanguageCatalog.Language;
import edu.self.w2k.gui.LanguageConverter;

import org.junit.jupiter.api.Test;

class LanguageCatalogTest {

    @Test
    void should_load_editions_from_the_resource_file() {
        // When
        var editions = LanguageCatalog.editions();

        // Then
        assertThat(editions).isNotEmpty()
                .extracting(Language::code)
                .contains("en", "fr", "el", "de");
    }

    @Test
    void should_sort_editions_by_display_name() {
        // When
        var names = LanguageCatalog.editions().stream().map(Language::displayName).toList();

        // Then
        assertThat(names).isSortedAccordingTo(String::compareToIgnoreCase);
    }

    @Test
    void should_resolve_display_names_for_every_edition() {
        // When / Then — a code with no JDK name would show as a bare uppercase code in the dropdown
        assertThat(LanguageCatalog.editions())
                .allSatisfy(lang -> assertThat(lang.displayName()).isNotEqualTo(lang.code().toUpperCase()));
    }

    @Test
    void should_offer_every_iso_language_as_a_word_language() {
        // When
        var wordLanguages = LanguageCatalog.wordLanguages();

        // Then
        assertThat(wordLanguages).hasSizeGreaterThan(150)
                .extracting(Language::code)
                .contains("en", "el", "la");
    }

    @Test
    void should_sort_word_languages_by_display_name() {
        // When
        var names = LanguageCatalog.wordLanguages().stream().map(Language::displayName).toList();

        // Then
        assertThat(names).isSortedAccordingTo(String::compareToIgnoreCase);
    }

    @Test
    void should_render_code_alongside_name_when_displayed_in_a_dropdown() {
        // "fr" is used rather than "el": CLDR renamed el from "Modern Greek" to "Greek", so only a
        // language with a stable English name is safe to assert exactly across JDK versions.
        assertThat(Language.of("fr")).hasToString("French (fr)");
    }

    @Test
    void should_fall_back_to_uppercase_code_when_language_is_unknown() {
        // When
        Language unknown = Language.of("qqq");

        // Then
        assertThat(unknown.displayName()).isEqualTo("QQQ");
    }

    @Test
    void should_trim_and_deduplicate_when_parsing_codes() {
        // When
        var parsed = LanguageCatalog.parseCodes(" fr , el ,fr,  , en ");

        // Then
        assertThat(parsed).extracting(Language::code).containsExactly("en", "fr", "el");
    }

    @Test
    void should_return_empty_list_when_codes_are_missing_or_blank() {
        // When / Then
        assertThat(LanguageCatalog.parseCodes(null)).isEmpty();
        assertThat(LanguageCatalog.parseCodes("   ")).isEmpty();
    }

    @Test
    void should_resolve_edition_by_display_name() {
        // When
        var edition = LanguageCatalog.findEdition("english");

        // Then 
        assertThat(edition).isNotNull();
        assertThat(edition).isPresent().get().extracting(Language::code).isEqualTo("en");
    }

    @Test
    void should_keep_unknown_edition() {
        // Given
        var input = "Not listed language";

        // When
        var result = new LanguageConverter().fromString(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.code()).isEqualTo(input);
    }
}
