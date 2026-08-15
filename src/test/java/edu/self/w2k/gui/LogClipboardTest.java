package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class LogClipboardTest {

    @Test
    void should_join_the_lines_the_way_a_console_reads() {
        // Given
        List<String> lines = List.of("12:00:00 INFO  - started", "12:00:01 INFO  - done");

        // When
        String text = LogClipboard.textOf(lines);

        // Then
        assertThat(text).isEqualTo("12:00:00 INFO  - started"
                                   + System.lineSeparator()
                                   + "12:00:01 INFO  - done");
    }

    @Test
    void should_produce_nothing_to_copy_when_no_line_is_selected() {
        // When / Then
        assertThat(LogClipboard.textOf(List.of())).isEmpty();
    }

    @Test
    void should_skip_a_null_line() {
        // When / Then
        assertThat(LogClipboard.textOf(Arrays.asList("first", null, "second")))
                .isEqualTo("first" + System.lineSeparator() + "second");
    }
}
