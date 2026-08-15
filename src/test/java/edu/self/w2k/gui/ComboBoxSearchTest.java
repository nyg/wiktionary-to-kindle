package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import edu.self.w2k.config.LanguageCatalog.Language;
import edu.self.w2k.gui.ComboBoxSearch.Buffer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComboBoxSearchTest {

    @Test
    void should_match_a_code_exactly_when_the_query_is_a_code() {
        // Given
        List<Language> items = List.of(Language.of("fr"), Language.of("de"), Language.of("en"));

        // When / Then
        assertThat(ComboBoxSearch.match(items, "de")).isPresent().get()
                .extracting(Language::code).isEqualTo("de");
    }

    @Test
    void should_match_a_name_prefix_when_the_query_is_partly_typed() {
        // Given
        List<Language> items = List.of(Language.of("fr"), Language.of("de"), Language.of("fi"));

        // When / Then
        assertThat(ComboBoxSearch.match(items, "fre")).isPresent().get()
                .extracting(Language::code).isEqualTo("fr");
    }

    @Test
    void should_ignore_case_when_matching() {
        // Given
        List<Language> items = List.of(Language.of("de"), Language.of("fr"));

        // When / Then
        assertThat(ComboBoxSearch.match(items, "GERMAN")).isPresent().get()
                .extracting(Language::code).isEqualTo("de");
    }

    @Test
    void should_fall_back_to_a_substring_when_nothing_starts_with_the_query() {
        // Given
        List<Language> items = List.of(Language.of("fr"), Language.of("grc", "Ancient Greek", 89814));

        // When / Then
        assertThat(ComboBoxSearch.match(items, "greek")).isPresent().get()
                .extracting(Language::code).isEqualTo("grc");
    }

    @Test
    void should_prefer_an_exact_code_over_a_name_that_starts_with_the_same_letters() {
        // Given
        List<Language> items = List.of(Language.of("id"), Language.of("is"));

        // When / Then
        assertThat(ComboBoxSearch.match(items, "is")).isPresent().get()
                .extracting(Language::code).isEqualTo("is");
    }

    @Test
    void should_match_nothing_when_the_query_is_absent_or_unknown() {
        // Given
        List<Language> items = List.of(Language.of("fr"));

        // When / Then
        assertThat(ComboBoxSearch.match(items, "zzz")).isEmpty();
        assertThat(ComboBoxSearch.match(items, "  ")).isEmpty();
        assertThat(ComboBoxSearch.match(items, null)).isEmpty();
        assertThat(ComboBoxSearch.match(null, "fr")).isEmpty();
    }

    @Test
    void should_narrow_the_match_as_more_characters_are_typed() {
        // Given
        List<Language> items = List.of(Language.of("de"), Language.of("el"), Language.of("fr"));
        Buffer buffer = new ComboBoxSearch.Buffer();

        // When / Then
        assertThat(ComboBoxSearch.typed(buffer, items, "g")).isPresent().get()
                .extracting(Language::code).isEqualTo("de");
        assertThat(ComboBoxSearch.typed(buffer, items, "r")).isPresent().get()
                .extracting(Language::code).isEqualTo("el");
        assertThat(buffer.text()).isEqualTo("gr");
    }

    @Test
    void should_ignore_control_characters_when_typed() {
        // Given
        List<Language> items = List.of(Language.of("fr"));
        Buffer buffer = new ComboBoxSearch.Buffer();

        // When / Then
        assertThat(ComboBoxSearch.typed(buffer, items, "\t")).isEmpty();
        assertThat(ComboBoxSearch.typed(buffer, items, "")).isEmpty();
        assertThat(ComboBoxSearch.typed(buffer, items, null)).isEmpty();
        assertThat(buffer.text()).isEmpty();
    }

    @Test
    void should_widen_the_match_when_a_character_is_deleted() {
        // Given
        List<Language> items = List.of(Language.of("de"), Language.of("da"));
        Buffer buffer = new ComboBoxSearch.Buffer();
        ComboBoxSearch.typed(buffer, items, "d");
        ComboBoxSearch.typed(buffer, items, "a");

        // When
        ComboBoxSearch.backspaced(buffer, items);

        // Then
        assertThat(buffer.text()).isEqualTo("d");
    }

    @Test
    void should_stay_empty_when_backspacing_an_empty_buffer() {
        // Given
        Buffer buffer = new ComboBoxSearch.Buffer();

        // When
        ComboBoxSearch.backspaced(buffer, List.of(Language.of("fr")));

        // Then
        assertThat(buffer.text()).isEmpty();
    }

    @Test
    void should_clear_the_buffer_when_the_previous_keystroke_has_gone_stale() {
        // Given
        AtomicLong fakeClock = new AtomicLong();
        Buffer buffer = new ComboBoxSearch.Buffer(
                () -> fakeClock.addAndGet(ComboBoxSearch.RESET_AFTER_NANOS * 2));
        buffer.append("fr");

        // When
        buffer.append("d");

        // Then
        assertThat(buffer.text()).isEqualTo("d");
    }

    @Test
    void should_discard_the_buffer_when_cleared() {
        // Given
        Buffer buffer = new ComboBoxSearch.Buffer();
        buffer.append("fre");

        // When
        buffer.clear();

        // Then
        assertThat(buffer.text()).isEmpty();
    }
}
