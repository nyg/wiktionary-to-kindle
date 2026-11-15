package edu.self.w2k.render;

import java.util.List;

import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.model.WiktionarySense;
import edu.self.w2k.model.WiktionarySound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HeadlineTest {

    @Test
    void should_show_part_of_speech_gender_and_pronunciation_when_the_entry_has_all_three() {
        // Given
        WiktionaryEntry entry = new WiktionaryEntry("chat", "fr", "noun", List.of(), List.of())
                .withPosTitle("Nom commun 1")
                .withTags(List.of("masculine", "letter"))
                .withSounds(List.of(new WiktionarySound(null), new WiktionarySound("\\ʃa\\"), new WiktionarySound("[ʃɑ]")));
        StringBuilder html = new StringBuilder();

        // When
        Headline.appendTo(html, "fr", entry);

        // Then
        assertThat(html).hasToString("<div><i>Nom commun, m.</i> \\ʃa\\</div>");
    }

    @Test
    void should_wrap_the_pronunciation_in_slashes_when_the_edition_leaves_it_bare() {
        // Given
        WiktionaryEntry entry = new WiktionaryEntry("λέξη", "el", "noun", List.of(), List.of())
                .withSounds(List.of(new WiktionarySound("ˈle.ksi")));
        StringBuilder html = new StringBuilder();

        // When
        Headline.appendTo(html, "el", entry);

        // Then
        assertThat(html).hasToString("<div><i>ουσιαστικό</i> /ˈle.ksi/</div>");
    }

    @Test
    void should_take_the_gender_from_the_senses_when_the_entry_has_no_tags_of_its_own() {
        // Given
        WiktionarySense cat = new WiktionarySense(List.of("cat"), List.of(), List.of())
                .withTags(List.of("masculine"));
        WiktionarySense tomcat = new WiktionarySense(List.of("tomcat"), List.of(), List.of())
                .withTags(List.of("masculine", "specifically"));
        WiktionarySense glossless = new WiktionarySense(List.of(), List.of(), List.of());
        WiktionaryEntry entry = new WiktionaryEntry("chat", "fr", "noun", List.of(cat, tomcat, glossless), List.of());
        StringBuilder html = new StringBuilder();

        // When
        Headline.appendTo(html, "en", entry);

        // Then
        assertThat(html).hasToString("<div><i>noun, m.</i></div>");
    }

    @Test
    void should_show_the_pronunciation_alone_when_nothing_else_is_known() {
        // Given
        WiktionaryEntry entry = new WiktionaryEntry("x", "en", null, List.of(), List.of())
                .withSounds(List.of(new WiktionarySound("/ɛks/")));
        StringBuilder html = new StringBuilder();

        // When
        Headline.appendTo(html, "en", entry);

        // Then
        assertThat(html).hasToString("<div>/ɛks/</div>");
    }

    @Test
    void should_append_nothing_when_the_entry_has_no_part_of_speech_grammar_or_pronunciation() {
        // Given
        WiktionaryEntry entry = new WiktionaryEntry("x", "en", null, List.of(), List.of());
        StringBuilder html = new StringBuilder();

        // When
        Headline.appendTo(html, "en", entry);

        // Then
        assertThat(html).isEmpty();
    }
}
