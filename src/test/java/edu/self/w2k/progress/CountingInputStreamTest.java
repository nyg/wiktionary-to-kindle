package edu.self.w2k.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CountingInputStreamTest {

    private final List<Long> reports = new ArrayList<>();

    @Test
    void should_report_once_per_threshold_when_reading_in_chunks() throws Exception {
        // Given
        byte[] data = new byte[10];
        try (CountingInputStream unit = new CountingInputStream(new ByteArrayInputStream(data), 4, reports::add)) {

            // When
            assertThat(unit.readNBytes(3)).hasSize(3);   // count 3, below threshold
            assertThat(unit.readNBytes(3)).hasSize(3);   // count 6, first report
            assertThat(unit.readNBytes(4)).hasSize(4);   // count 10, second report

            // Then
            assertThat(reports).containsExactly(6L, 10L);
            assertThat(unit.count()).isEqualTo(10);
        }
    }

    @Test
    void should_count_every_byte_when_reading_one_at_a_time() throws Exception {
        // Given
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        try (CountingInputStream unit = new CountingInputStream(new ByteArrayInputStream(data), 1, reports::add)) {

            // When
            while (unit.read() != -1) {
                // drain
            }

            // Then
            assertThat(reports).containsExactly(1L, 2L, 3L);
            assertThat(unit.count()).isEqualTo(3);
        }
    }

    @Test
    void should_not_count_end_of_stream_when_exhausted() throws Exception {
        // Given
        try (CountingInputStream unit = new CountingInputStream(InputStream.nullInputStream(), 1, reports::add)) {

            // When
            int first = unit.read();

            // Then
            assertThat(first).isEqualTo(-1);
            assertThat(unit.count()).isZero();
            assertThat(reports).isEmpty();
        }
    }

    @Test
    void should_count_skipped_bytes_when_skipping() throws Exception {
        // Given
        byte[] data = new byte[8];
        try (CountingInputStream unit = new CountingInputStream(new ByteArrayInputStream(data), 4, reports::add)) {

            // When
            long skipped = unit.skip(8);

            // Then
            assertThat(skipped).isEqualTo(8);
            assertThat(unit.count()).isEqualTo(8);
            assertThat(reports).containsExactly(8L);
        }
    }

    @Test
    void should_not_support_mark_because_rewinding_desynchronises_the_count() throws Exception {
        // Given
        try (CountingInputStream unit = new CountingInputStream(new ByteArrayInputStream(new byte[4]), 1, reports::add)) {

            // When / Then
            assertThat(unit.markSupported()).isFalse();
        }
    }

    @Test
    void should_reject_non_positive_threshold_when_constructed() {
        // When / Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CountingInputStream(InputStream.nullInputStream(), 0, reports::add))
                .withMessageContaining("must be positive");
    }
}
