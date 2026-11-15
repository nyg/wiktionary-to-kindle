package edu.self.w2k.render;

import java.util.List;

import edu.self.w2k.model.WiktionaryExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExamplesTest {

    @Test
    void should_embolden_the_headword_and_append_the_translation_when_the_example_has_both() {
        // Given
        WiktionaryExample example = new WiktionaryExample("Let us have a chat over coffee!")
                .withBoldTextOffsets(List.of(List.of(14, 18)))
                .withTranslation("Discutons autour d’un café !");
        StringBuilder html = new StringBuilder();

        // When
        Examples.appendTo(html, List.of(example));

        // Then
        assertThat(html).hasToString("<ul><li>Let us have a <b>chat</b> over coffee!"
                + " — <i>Discutons autour d’un café !</i></li></ul>");
    }

    @Test
    void should_fall_back_to_the_english_field_when_the_example_has_no_translation() {
        // Given
        WiktionaryExample example = new WiktionaryExample("Δεν είπε λέξη.").withEnglish("He did not say a word.");
        StringBuilder html = new StringBuilder();

        // When
        Examples.appendTo(html, List.of(example));

        // Then
        assertThat(html).hasToString("<ul><li>Δεν είπε λέξη. — <i>He did not say a word.</i></li></ul>");
    }

    @Test
    void should_keep_the_three_shortest_examples_in_their_order_when_a_sense_has_more() {
        // Given
        List<WiktionaryExample> examples = List.of(new WiktionaryExample("A long quotation from a novel."),
                new WiktionaryExample("One."), new WiktionaryExample(" "), new WiktionaryExample("Two."),
                new WiktionaryExample("Three."));
        StringBuilder html = new StringBuilder();

        // When
        Examples.appendTo(html, examples);

        // Then
        assertThat(html).hasToString("<ul><li>One.</li><li>Two.</li><li>Three.</li></ul>");
    }

    @Test
    void should_append_nothing_when_no_example_has_text() {
        // Given
        StringBuilder html = new StringBuilder();

        // When
        Examples.appendTo(html, List.of(new WiktionaryExample(""), new WiktionaryExample(null)));

        // Then
        assertThat(html).isEmpty();
    }

    @Test
    void should_count_offsets_in_code_points_when_the_text_has_characters_outside_the_basic_plane() {
        // When
        String result = Examples.emphasised("𝔸 cat sat", List.of(List.of(2, 5)));

        // Then
        assertThat(result).isEqualTo("𝔸 <b>cat</b> sat");
    }

    @Test
    void should_ignore_offsets_that_overlap_or_fall_outside_the_text_when_emphasising() {
        // When
        String result = Examples.emphasised("a < b", List.of(List.of(4, 5), List.of(0, 1), List.of(0, 3), List.of(3, 99), List.of(2)));

        // Then
        assertThat(result).isEqualTo("<b>a</b> &lt; <b>b</b>");
    }
}
