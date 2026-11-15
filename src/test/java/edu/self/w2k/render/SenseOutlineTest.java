package edu.self.w2k.render;

import java.util.List;

import edu.self.w2k.model.WiktionaryExample;
import edu.self.w2k.model.WiktionarySense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SenseOutlineTest {

    @Test
    void should_render_the_gloss_without_a_list_when_there_is_one_sense() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("A round fruit."), List.of(), List.of());
        SenseOutline unit = SenseOutline.of(List.of(sense), LabelSource.ALL_TAGS);
        StringBuilder html = new StringBuilder();

        // When
        unit.appendTo(html);

        // Then
        assertThat(html).hasToString("<div>A round fruit.</div>");
    }

    @Test
    void should_number_the_senses_when_there_are_several() {
        // Given
        WiktionarySense first = new WiktionarySense(List.of("A round fruit."), List.of(), List.of());
        WiktionarySense second = new WiktionarySense(List.of("The tree bearing it."), List.of(), List.of());
        SenseOutline unit = SenseOutline.of(List.of(first, second), LabelSource.ALL_TAGS);
        StringBuilder html = new StringBuilder();

        // When
        unit.appendTo(html);

        // Then
        assertThat(html).hasToString("<ol><li>A round fruit.</li><li>The tree bearing it.</li></ol>");
    }

    @Test
    void should_nest_a_sub_sense_under_its_parent_when_glosses_list_the_parent_first() {
        // Given
        WiktionarySense parent = new WiktionarySense(List.of("A reference work."), List.of(), List.of());
        WiktionarySense child = new WiktionarySense(
                List.of("A reference work.", "One held to list a whole language."), List.of(), List.of());
        WiktionarySense other = new WiktionarySense(List.of("An associative array."), List.of(), List.of());
        SenseOutline unit = SenseOutline.of(List.of(parent, child, other), LabelSource.ALL_TAGS);
        StringBuilder html = new StringBuilder();

        // When
        unit.appendTo(html);

        // Then
        assertThat(html).hasToString("<ol><li>A reference work."
                + "<ol><li>One held to list a whole language.</li></ol></li>"
                + "<li>An associative array.</li></ol>");
    }

    @Test
    void should_put_labels_before_the_gloss_and_examples_after_it_when_the_sense_has_both() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("Discussion amicale."),
                List.of(new WiktionaryExample("Quick chat.")), List.of())
                .withTags(List.of("familiar"));
        SenseOutline unit = SenseOutline.of(List.of(sense), LabelSource.ALL_TAGS);
        StringBuilder html = new StringBuilder();

        // When
        unit.appendTo(html);

        // Then
        assertThat(html).hasToString(
                "<div><i>(familiar)</i> Discussion amicale.<ul><li>Quick chat.</li></ul></div>");
    }

    @Test
    void should_show_the_raw_gloss_when_the_label_source_is_raw_glosses() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("A free transfer."), List.of(), List.of())
                .withRawGlosses(List.of("(soccer) A free transfer."))
                .withTags(List.of("uncountable"));
        SenseOutline unit = SenseOutline.of(List.of(sense), LabelSource.RAW_GLOSSES);
        StringBuilder html = new StringBuilder();

        // When
        unit.appendTo(html);

        // Then
        assertThat(html).hasToString("<div>(soccer) A free transfer.</div>");
    }

    @Test
    void should_escape_markup_and_join_lines_when_a_gloss_carries_them() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("A & B\nC"), List.of(), List.of());
        SenseOutline unit = SenseOutline.of(List.of(sense), LabelSource.ALL_TAGS);
        StringBuilder html = new StringBuilder();

        // When
        unit.appendTo(html);

        // Then
        assertThat(html).hasToString("<div>A &amp; B; C</div>");
    }
}
