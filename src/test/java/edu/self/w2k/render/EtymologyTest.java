package edu.self.w2k.render;

import java.util.List;

import edu.self.w2k.model.WiktionaryEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EtymologyTest {

    @Test
    void should_bracket_the_etymology_when_the_entry_has_one() {
        // Given
        WiktionaryEntry entry = new WiktionaryEntry("accueil", "fr", "noun", List.of(), List.of())
                .withEtymologyTexts(List.of("Déverbal de accueillir."));
        StringBuilder html = new StringBuilder();

        // When
        Etymology.appendTo(html, entry);

        // Then
        assertThat(html).hasToString("<div>[Déverbal de accueillir.]</div>");
    }

    @Test
    void should_append_nothing_when_the_entry_has_no_etymology() {
        // Given
        WiktionaryEntry entry = new WiktionaryEntry("x", "fr", "noun", List.of(), List.of());
        StringBuilder html = new StringBuilder();

        // When
        Etymology.appendTo(html, entry);

        // Then
        assertThat(html).isEmpty();
    }

    @Test
    void should_join_the_parts_when_the_edition_lists_several() {
        // Given
        WiktionaryEntry entry = new WiktionaryEntry("chat", "fro", "noun", List.of(), List.of())
                .withEtymologyTexts(List.of("(Nom 1) Du latin cattus.", "(Nom 2) À rapprocher de chaffaut."));

        // When
        String result = Etymology.textOf(entry);

        // Then
        assertThat(result).isEqualTo("(Nom 1) Du latin cattus. (Nom 2) À rapprocher de chaffaut.");
    }

    @Test
    void should_drop_the_ancestry_tree_when_the_english_edition_prints_one() {
        // Given
        WiktionaryEntry entry = new WiktionaryEntry("dictionary", "en", "noun", List.of(), List.of())
                .withEtymologyText("Etymology tree\nProto-Italic *deikō\nLatin dīcō\n▲\nEnglish dictionary\n"
                        + "From Middle English dixionare, a learned borrowing from Medieval Latin dictiōnārium.");

        // When
        String result = Etymology.textOf(entry);

        // Then
        assertThat(result)
                .isEqualTo("From Middle English dixionare, a learned borrowing from Medieval Latin dictiōnārium.");
    }

    @Test
    void should_strip_wiki_magic_words_and_language_markers_when_cleaning() {
        // Given
        WiktionaryEntry entry = new WiktionaryEntry("x", "de", "noun", List.of(), List.of())
                .withEtymologyTexts(List.of("Substantiv zu dem Verb ordiri ^(→ la)\n__NOEDITSECTION__"));

        // When
        String result = Etymology.textOf(entry);

        // Then
        assertThat(result).isEqualTo("Substantiv zu dem Verb ordiri");
    }

    @Test
    void should_keep_the_whole_text_when_it_runs_long() {
        // Given
        String history = "lorem ".repeat(400).strip();
        WiktionaryEntry entry = new WiktionaryEntry("x", "en", "noun", List.of(), List.of())
                .withEtymologyText(history);

        // When
        String result = Etymology.textOf(entry);

        // Then
        assertThat(result).isEqualTo(history);
    }
}
