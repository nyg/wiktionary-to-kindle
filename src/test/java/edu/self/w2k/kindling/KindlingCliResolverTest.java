package edu.self.w2k.kindling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KindlingCliResolverTest {

    @Mock
    private KindlingDownloader downloader;

    @Mock
    private Function<String, Optional<Path>> pathProbe;

    @Mock
    private BiFunction<String, KindlingPlatform, String> digestProvider;

    @TempDir
    Path tmp;

    @Test
    void should_return_override_when_override_is_executable() throws Exception {
        // Given
        Path executable = Files.createFile(tmp.resolve("kindling-cli"));
        executable.toFile().setExecutable(true);
        KindlingCliResolver unit = new KindlingCliResolver(
                "v0.14.5", Optional.of(executable), downloader, pathProbe, digestProvider);

        // When
        Path result = unit.resolve();

        // Then
        assertThat(result).isEqualTo(executable);
        verify(downloader, never()).download(any(), any(), any());
    }

    @Test
    void should_return_path_binary_when_found_on_path() throws Exception {
        // Given
        Path executableOnPath = Files.createFile(tmp.resolve("kindling-cli-on-path"));
        executableOnPath.toFile().setExecutable(true);
        when(pathProbe.apply("kindling-cli")).thenReturn(Optional.of(executableOnPath));
        KindlingCliResolver unit = new KindlingCliResolver(
                "v0.14.5", Optional.empty(), downloader, pathProbe, digestProvider);

        // When
        Path result = unit.resolve();

        // Then
        assertThat(result).isEqualTo(executableOnPath);
        verify(downloader, never()).download(any(), any(), any());
    }

    @Test
    void should_download_when_cache_is_empty() throws Exception {
        // Given
        Path downloadedBin = tmp.resolve("kindling-cli-linux");
        when(pathProbe.apply(anyString())).thenReturn(Optional.empty());
        when(downloader.download(any(), any(), any())).thenReturn(downloadedBin);
        KindlingCliResolver unit = new KindlingCliResolver(
                "v999.999.999", Optional.empty(), downloader, pathProbe, digestProvider);

        // When
        Path result = unit.resolve();

        // Then
        assertThat(result).isEqualTo(downloadedBin);
        verify(downloader).download(any(), any(), any());
    }
}
