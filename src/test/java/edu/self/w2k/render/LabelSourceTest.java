package edu.self.w2k.render;

import java.util.List;

import edu.self.w2k.model.WiktionarySense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LabelSourceTest {

    @Test
    void should_read_labels_from_raw_glosses_when_edition_is_english() {
        // When
        LabelSource unit = LabelSource.forEdition("en");

        // Then
        assertThat(unit).isEqualTo(LabelSource.RAW_GLOSSES);
    }

    @Test
    void should_read_labels_from_raw_tags_when_edition_is_greek() {
        // When
        LabelSource unit = LabelSource.forEdition("el");

        // Then
        assertThat(unit).isEqualTo(LabelSource.RAW_TAGS);
    }

    @Test
    void should_read_labels_from_all_tags_when_edition_is_any_other() {
        // When
        LabelSource unit = LabelSource.forEdition("fr");

        // Then
        assertThat(unit).isEqualTo(LabelSource.ALL_TAGS);
    }

    @Test
    void should_join_raw_tags_tags_and_topics_without_structural_ones_when_source_is_all_tags() {
        // Given
        LabelSource unit = LabelSource.ALL_TAGS;
        WiktionarySense sense = new WiktionarySense(List.of("Clavardage."), List.of(), List.of())
                .withRawTags(List.of("Internet", " "))
                .withTags(List.of("familiar", "form-of", "European-Union", "no-gloss"))
                .withTopics(List.of("computing", "familiar"));

        // When
        List<String> result = unit.labelsOf(sense);

        // Then
        assertThat(result).containsExactly("Internet", "familiar", "European Union", "computing");
    }

    @Test
    void should_keep_only_raw_tags_when_source_is_raw_tags() {
        // Given
        LabelSource unit = LabelSource.RAW_TAGS;
        WiktionarySense sense = new WiktionarySense(List.of("φράση, κουβέντα"), List.of(), List.of())
                .withRawTags(List.of("μεταφορικά"))
                .withTags(List.of("figuratively"));

        // When
        List<String> result = unit.labelsOf(sense);

        // Then
        assertThat(result).containsExactly("μεταφορικά");
    }

    @Test
    void should_give_no_labels_when_source_is_raw_glosses() {
        // Given
        LabelSource unit = LabelSource.RAW_GLOSSES;
        WiktionarySense sense = new WiktionarySense(List.of("A free transfer."), List.of(), List.of())
                .withTags(List.of("uncountable"))
                .withTopics(List.of("soccer"));

        // When
        List<String> result = unit.labelsOf(sense);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_display_plain_glosses_when_raw_glosses_do_not_line_up() {
        // Given
        LabelSource unit = LabelSource.RAW_GLOSSES;
        WiktionarySense sense = new WiktionarySense(List.of("Parent.", "Child."), List.of(), List.of())
                .withRawGlosses(List.of("(rare) Child."));

        // When
        List<String> result = unit.displayGlosses(sense, sense.glosses());

        // Then
        assertThat(result).containsExactly("Parent.", "Child.");
    }
}
