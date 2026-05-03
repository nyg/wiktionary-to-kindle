package edu.self.w2k.command;

import edu.self.w2k.download.DumpDownloader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DownloadCommandTest {

    @Mock
    private DumpDownloader downloader;

    @InjectMocks
    private DownloadCommand unit;

    @Test
    void should_invoke_downloader_when_run_is_called() {
        // Given (no setup needed)

        // When
        unit.run();

        // Then
        verify(downloader).download();
    }
}
