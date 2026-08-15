package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import edu.self.w2k.config.LanguageCatalog.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LanguageConverterTest {

    private final LanguageConverter unit = new LanguageConverter();

    @Test
    void should_render_name_and_code_when_converting_to_string() {
        // When / Then
        assertThat(unit.toString(Language.of("fr"))).isEqualTo("French (fr)");
    }

    @Test
    void should_render_empty_string_when_language_is_null() {
        // When / Then
        assertThat(unit.toString(null)).isEmpty();
    }

    @Test
    void should_round_trip_a_rendered_language() {
        // Given
        Language original = Language.of("fr");

        // When
        Language parsed = unit.fromString(unit.toString(original));

        // Then
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void should_return_null_when_the_code_is_not_one_of_the_offered_languages() {
        // When
        Language parsed = unit.fromString("nds");

        // Then
        assertThat(parsed).isNull();
    }

    @Test
    void should_resolve_against_the_languages_it_is_given() {
        // Given
        LanguageConverter scoped = new LanguageConverter(() -> List.of(Language.of("grc", "Ancient Greek", 89814)));

        // When / Then
        assertThat(scoped.fromString("grc")).isNotNull().extracting(Language::displayName)
                .isEqualTo("Ancient Greek");
        assertThat(scoped.fromString("fr")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  fr  ", "French (fr)", " French (fr) ", "Anything (fr)"})
    void should_extract_the_code_regardless_of_surrounding_text(String input) {
        // When / Then
        assertThat(unit.fromString(input).code()).isEqualTo("fr");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void should_return_null_when_text_is_absent(String input) {
        // When / Then
        assertThat(unit.fromString(input)).isNull();
    }

    @Test
    void should_return_null_when_parentheses_are_empty() {
        // When / Then
        assertThat(unit.fromString("Nothing ()")).isNull();
    }
}
