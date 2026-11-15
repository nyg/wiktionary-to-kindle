package edu.self.w2k.render;

import java.util.List;

import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.model.WiktionaryLinkage;
import edu.self.w2k.model.WiktionarySense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RelatedWordsTest {

    @Test
    void should_list_synonyms_then_antonyms_when_the_entry_has_both() {
        // Given
        WiktionaryEntry entry = new WiktionaryEntry("беда", "ru", "noun", List.of(), List.of())
                .withSynonyms(List.of(new WiktionaryLinkage("несчастье"), new WiktionaryLinkage("бедствие")))
                .withAntonyms(List.of(new WiktionaryLinkage("счастье")));
        StringBuilder html = new StringBuilder();

        // When
        RelatedWords.appendTo(html, entry);

        // Then
        assertThat(html).hasToString("<div>≈ несчастье, бедствие</div><div>≠ счастье</div>");
    }

    @Test
    void should_gather_sense_level_words_without_repeats_or_the_headword_when_listing() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("A conversation."), List.of(), List.of())
                .withSynonyms(List.of(new WiktionaryLinkage("talk"), new WiktionaryLinkage("natter")));
        WiktionaryEntry entry = new WiktionaryEntry("chat", "en", "noun", List.of(sense), List.of())
                .withSynonyms(List.of(new WiktionaryLinkage("talk"), new WiktionaryLinkage("chat"),
                        new WiktionaryLinkage(" "), new WiktionaryLinkage(null)));
        StringBuilder html = new StringBuilder();

        // When
        RelatedWords.appendTo(html, entry);

        // Then
        assertThat(html).hasToString("<div>≈ talk, natter</div>");
    }

    @Test
    void should_stop_at_the_limit_when_the_entry_lists_more_words() {
        // Given
        List<WiktionaryLinkage> many = List.of(new WiktionaryLinkage("a"), new WiktionaryLinkage("b"),
                new WiktionaryLinkage("c"), new WiktionaryLinkage("d"), new WiktionaryLinkage("e"),
                new WiktionaryLinkage("f"), new WiktionaryLinkage("g"), new WiktionaryLinkage("h"));
        WiktionaryEntry entry = new WiktionaryEntry("x", "en", "noun", List.of(), List.of()).withAntonyms(many);
        StringBuilder html = new StringBuilder();

        // When
        RelatedWords.appendTo(html, entry);

        // Then
        assertThat(html).hasToString("<div>≠ a, b, c, d, e, f</div>");
    }

    @Test
    void should_append_nothing_when_the_entry_lists_no_related_words() {
        // Given
        WiktionaryEntry entry = new WiktionaryEntry("x", "en", "noun", List.of(), List.of());
        StringBuilder html = new StringBuilder();

        // When
        RelatedWords.appendTo(html, entry);

        // Then
        assertThat(html).isEmpty();
    }
}
