package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import edu.self.w2k.config.LanguageCatalog.Language;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ComboBoxFilterTest {

    private static final List<Language> ITEMS = List.of(
            Language.of("fr"), Language.of("de"), Language.of("el"), Language.of("af"),
            Language.of("grc", "Ancient Greek", 89814));

    @Test
    void should_keep_a_language_when_its_code_starts_with_the_query() {
        // When / Then
        assertThat(ComboBoxFilter.filter(ITEMS, "el")).extracting(Language::code).containsExactly("el");
    }

    @Test
    void should_keep_every_language_whose_name_contains_the_query() {
        // When / Then
        assertThat(ComboBoxFilter.filter(ITEMS, "greek"))
                .extracting(Language::code)
                .containsExactly("el", "grc");
    }

    @Test
    void should_ignore_case_when_filtering() {
        // When / Then
        assertThat(ComboBoxFilter.filter(ITEMS, "GERMAN"))
                .extracting(Language::code)
                .containsExactly("de");
    }

    @Test
    void should_narrow_the_list_as_more_characters_are_typed() {
        // When / Then
        assertThat(ComboBoxFilter.filter(ITEMS, "a")).hasSize(3);
        assertThat(ComboBoxFilter.filter(ITEMS, "af")).extracting(Language::code).containsExactly("af");
    }

    @Test
    void should_keep_nothing_when_the_query_matches_no_language() {
        // When / Then
        assertThat(ComboBoxFilter.filter(ITEMS, "zzz")).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void should_keep_everything_when_the_query_is_absent(String query) {
        // When / Then
        assertThat(ComboBoxFilter.filter(ITEMS, query)).hasSameSizeAs(ITEMS);
    }

    @Test
    void should_match_a_code_by_prefix_rather_than_by_substring() {
        // Given
        Language language = Language.of("ab", "Zulu", 0);

        // When / Then
        assertThat(ComboBoxFilter.matches(language, "a")).isTrue();
        assertThat(ComboBoxFilter.matches(language, "b")).isFalse();
    }
}
