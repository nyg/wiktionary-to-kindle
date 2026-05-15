package edu.self.w2k.render;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InflectionTagAbbreviatorTest {

    @Test
    void should_abbreviate_known_tags_when_called() {
        // When
        String result = InflectionTagAbbreviator.abbreviate(List.of("plural", "nominative"));

        // Then
        assertThat(result).isEqualTo("pl. nom.");
    }

    @Test
    void should_pass_unknown_tags_through_when_no_abbreviation_exists() {
        // When
        String result = InflectionTagAbbreviator.abbreviate(List.of("singular", "instrumental"));

        // Then
        assertThat(result).isEqualTo("sg. instrumental");
    }

    @Test
    void should_return_empty_string_when_tags_null_or_empty() {
        assertThat(InflectionTagAbbreviator.abbreviate(null)).isEmpty();
        assertThat(InflectionTagAbbreviator.abbreviate(List.of())).isEmpty();
    }

    @Test
    void should_skip_blank_tags_when_called() {
        // When
        String result = InflectionTagAbbreviator.abbreviate(List.of("singular", "  ", "genitive"));

        // Then
        assertThat(result).isEqualTo("sg. gen.");
    }
}
