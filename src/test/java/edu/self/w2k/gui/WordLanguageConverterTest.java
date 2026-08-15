package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import edu.self.w2k.config.LanguageCatalog.Language;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WordLanguageConverterTest {

    @Test
    void should_append_the_sense_count_when_the_language_carries_one() {
        // Given
        WordLanguageConverter unit = new WordLanguageConverter();

        // When
        String rendered = unit.toString(Language.of("fr", "French", 2653194));

        // Then
        assertThat(rendered).isEqualTo("French (fr) — 2.7M senses");
    }

    @Test
    void should_render_thousands_when_the_count_is_below_a_million() {
        // Given
        WordLanguageConverter unit = new WordLanguageConverter();

        // When
        String rendered = unit.toString(Language.of("el", "Greek", 106294));

        // Then
        assertThat(rendered).isEqualTo("Greek (el) — 106k senses");
    }

    @Test
    void should_omit_the_sense_count_when_the_language_has_none() {
        // Given
        WordLanguageConverter unit = new WordLanguageConverter();

        // When
        String rendered = unit.toString(Language.of("fr"));

        // Then
        assertThat(rendered).isEqualTo("French (fr)");
    }

    @Test
    void should_render_nothing_when_no_language_is_selected() {
        // Given
        WordLanguageConverter unit = new WordLanguageConverter();

        // When / Then
        assertThat(unit.toString(null)).isEmpty();
    }

    @Test
    void should_resolve_its_own_decorated_form_back_to_the_language() {
        // Given
        Language french = Language.of("fr", "French", 2653194);
        WordLanguageConverter unit = new WordLanguageConverter(() -> List.of(french));

        // When
        Language parsed = unit.fromString(unit.toString(french));

        // Then
        assertThat(parsed).isEqualTo(french);
    }

    @Test
    void should_resolve_nothing_when_the_code_is_outside_the_selected_edition() {
        // Given
        WordLanguageConverter unit = new WordLanguageConverter(() -> List.of(Language.of("fr")));

        // When / Then
        assertThat(unit.fromString("Klingon (tlh)")).isNull();
    }

    @Test
    void should_use_a_dot_decimal_separator_regardless_of_the_default_locale() {
        // When / Then
        assertThat(WordLanguageConverter.formatSenses(2653194)).isEqualTo("2.7M");
        assertThat(WordLanguageConverter.formatSenses(1500)).isEqualTo("2k");
        assertThat(WordLanguageConverter.formatSenses(505)).isEqualTo("505");
    }
}
