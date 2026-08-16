package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import edu.self.w2k.progress.ProgressListener;
import edu.self.w2k.progress.ProgressListener.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProgressSnapshotTest {

    private static final long MB = 1024L * 1024L;

    @Test
    void should_report_fraction_and_megabytes_when_downloading_with_known_length() {
        // When
        ProgressSnapshot snapshot = ProgressSnapshot.of(Stage.DOWNLOAD, 25 * MB, 100 * MB);

        // Then
        assertThat(snapshot.fraction()).isCloseTo(0.25, within(1e-9));
        assertThat(snapshot.message()).isEqualTo("Downloading dump… 25% (25 of 100 MB)");
    }

    @Test
    void should_be_indeterminate_when_content_length_is_unknown() {
        // When
        ProgressSnapshot snapshot =
                ProgressSnapshot.of(Stage.DOWNLOAD, 7 * MB, ProgressListener.TOTAL_UNKNOWN);

        // Then
        assertThat(snapshot.isIndeterminate()).isTrue();
        assertThat(snapshot.fraction()).isEqualTo(ProgressSnapshot.INDETERMINATE);
        assertThat(snapshot.message()).isEqualTo("Downloading dump… 7 MB");
    }

    @Test
    void should_report_percentage_when_parsing() {
        // When
        ProgressSnapshot snapshot = ProgressSnapshot.of(Stage.PARSE, 3, 4);

        // Then
        assertThat(snapshot.fraction()).isCloseTo(0.75, within(1e-9));
        assertThat(snapshot.message()).isEqualTo("Parsing entries… 75%");
    }

    @Test
    void should_report_chapter_counts_when_writing_html() {
        // When
        ProgressSnapshot snapshot = ProgressSnapshot.of(Stage.WRITE_HTML, 3, 7);

        // Then
        assertThat(snapshot.message()).isEqualTo("Writing HTML 3 of 7…");
    }

    @Test
    void should_be_indeterminate_when_folding() {
        // When
        ProgressSnapshot snapshot = ProgressSnapshot.of(Stage.FOLD, 0, ProgressListener.TOTAL_UNKNOWN);

        // Then
        assertThat(snapshot.isIndeterminate()).isTrue();
        assertThat(snapshot.message()).isEqualTo("Folding inflected forms…");
    }

    @Test
    void should_be_indeterminate_when_running_kindling() {
        // When
        ProgressSnapshot snapshot = ProgressSnapshot.of(Stage.KINDLING, 0, ProgressListener.TOTAL_UNKNOWN);

        // Then
        assertThat(snapshot.isIndeterminate()).isTrue();
        assertThat(snapshot.message()).contains("kindling-cli");
    }

    @Test
    void should_clamp_fraction_when_more_bytes_arrive_than_advertised() {
        // Given a server whose content-length understates the body
        ProgressSnapshot snapshot = ProgressSnapshot.of(Stage.DOWNLOAD, 150 * MB, 100 * MB);

        // Then the bar must not exceed full
        assertThat(snapshot.fraction()).isEqualTo(1.0);
    }

    @ParameterizedTest
    @EnumSource(Stage.class)
    void should_produce_a_message_for_every_stage(Stage stage) {
        // When
        ProgressSnapshot snapshot = ProgressSnapshot.of(stage, 1, 2);

        // Then — a new stage must not silently render as a blank status line
        assertThat(snapshot.message()).isNotBlank();
    }

    @Test
    void should_start_idle_and_not_indeterminate() {
        // When
        ProgressSnapshot idle = ProgressSnapshot.idle();

        // Then
        assertThat(idle.fraction()).isZero();
        assertThat(idle.isIndeterminate()).isFalse();
        assertThat(idle.message()).isEqualTo("Ready");
    }
}
