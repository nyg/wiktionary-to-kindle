package edu.self.w2k.command;

import edu.self.w2k.download.DownloadResult;
import edu.self.w2k.download.DumpDownloader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadCommandTest {

    @Mock
    private DumpDownloader downloader;

    @InjectMocks
    private DownloadCommand unit;

    @Test
    void should_invoke_downloader_when_run_is_called() throws Exception {
        // When
        unit.run();

        // Then
        verify(downloader).download();
    }

    @Test
    void should_return_downloader_result_when_execute_is_called() throws Exception {
        // Given
        DownloadResult expected = new DownloadResult(Path.of("dumps", "raw-wiktextract-data-en-2026-05-01.jsonl.gz"), false);
        when(downloader.download()).thenReturn(expected);

        // When
        DownloadResult actual = unit.execute();

        // Then
        assertThat(actual).isSameAs(expected);
    }

    @Test
    void should_propagate_failure_when_downloader_throws() throws Exception {
        // Given
        when(downloader.download()).thenThrow(new IOException("HTTP 503"));

        // When / Then
        assertThatIOException()
                .isThrownBy(() -> unit.run())
                .withMessage("HTTP 503");
    }
}
