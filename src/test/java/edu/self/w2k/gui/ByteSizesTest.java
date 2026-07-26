package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ByteSizesTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0 KB",
            "2048, 2 KB",
            "1048575, 1023 KB",
            "1048576, 1 MB",
            "110100480, 105 MB",
            "1073741824, 1.0 GB",
            "1610612736, 1.5 GB",
    })
    void should_scale_unit_to_magnitude(long bytes, String expected) {
        // When / Then
        assertThat(ByteSizes.format(bytes)).isEqualTo(expected);
    }

    @Test
    void should_render_question_mark_when_size_is_unknown() {
        // Given the sentinel DumpCatalog uses when a file's size cannot be read
        assertThat(ByteSizes.format(-1)).isEqualTo("?");
    }
}
