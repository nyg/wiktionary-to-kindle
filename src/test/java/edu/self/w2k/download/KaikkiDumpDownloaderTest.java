package edu.self.w2k.download;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KaikkiDumpDownloaderTest {

    @Mock
    private HttpClient httpClient;

    @TempDir
    Path tmp;

    @Test
    @SuppressWarnings("unchecked")
    void should_save_dump_with_date_when_download_succeeds() throws Exception {
        // Given
        Path partPath = tmp.resolve("raw-wiktextract-data-en.jsonl.gz.part");

        HttpHeaders headers = HttpHeaders.of(
                Map.of("last-modified", List.of("Fri, 01 May 2026 10:00:00 GMT")),
                (k, v) -> true);
        HttpResponse<Path> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.headers()).thenReturn(headers);
        doAnswer(inv -> {
            Files.write(partPath, new byte[0]);
            return mockResponse;
        }).when(httpClient).send(any(), any());

        KaikkiDumpDownloader unit = new KaikkiDumpDownloader("en", httpClient, tmp);

        // When
        unit.download();

        // Then
        assertThat(tmp.resolve("raw-wiktextract-data-en-2026-05-01.jsonl.gz")).exists();
        assertThat(partPath).doesNotExist();
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_keep_existing_dump_when_target_already_exists() throws Exception {
        // Given
        Path existingDump = tmp.resolve("raw-wiktextract-data-en-2026-05-01.jsonl.gz");
        Files.createFile(existingDump);

        HttpHeaders headers = HttpHeaders.of(
                Map.of("last-modified", List.of("Fri, 01 May 2026 10:00:00 GMT")),
                (k, v) -> true);
        HttpResponse<Path> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.headers()).thenReturn(headers);
        doAnswer(inv -> mockResponse).when(httpClient).send(any(), any());

        KaikkiDumpDownloader unit = new KaikkiDumpDownloader("en", httpClient, tmp);

        // When
        unit.download();

        // Then
        assertThat(existingDump).exists();
        assertThat(tmp.resolve("raw-wiktextract-data-en.jsonl.gz.part")).doesNotExist();
    }

    @Test
    void should_use_dictionary_path_when_lang_is_english() {
        // Given / When
        String url = KaikkiDumpDownloader.buildUrl("en");

        // Then
        assertThat(url).endsWith("/dictionary/raw-wiktextract-data.jsonl.gz");
    }

    @Test
    void should_use_lang_wiktionary_path_when_lang_is_other() {
        // Given / When
        String url = KaikkiDumpDownloader.buildUrl("fr");

        // Then
        assertThat(url).endsWith("/frwiktionary/raw-wiktextract-data.jsonl.gz");
    }
}
