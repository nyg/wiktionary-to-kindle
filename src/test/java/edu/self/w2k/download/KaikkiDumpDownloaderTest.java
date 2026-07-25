package edu.self.w2k.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KaikkiDumpDownloaderTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<Void> headResponse;

    @TempDir
    Path tmp;

    private KaikkiDumpDownloader unit;

    @BeforeEach
    void setUp() {
        unit = new KaikkiDumpDownloader("en", httpClient, tmp);
    }

    @Test
    void should_save_dump_with_date_when_download_succeeds() throws Exception {
        // Given
        Path partPath = tmp.resolve("raw-wiktextract-data-en.jsonl.gz.part");
        stubHeadOk(lastModified("Fri, 01 May 2026 10:00:00 GMT"));
        stubGet(200, partPath);

        // When
        unit.download();

        // Then
        assertThat(tmp.resolve("raw-wiktextract-data-en-2026-05-01.jsonl.gz")).exists();
        assertThat(partPath).doesNotExist();
    }

    @Test
    void should_keep_existing_dump_when_target_already_exists() throws Exception {
        // Given
        Path existingDump = tmp.resolve("raw-wiktextract-data-en-2026-05-01.jsonl.gz");
        Files.createFile(existingDump);
        stubHeadOk(lastModified("Fri, 01 May 2026 10:00:00 GMT"));

        // When
        unit.download();

        // Then
        assertThat(existingDump).exists();
        assertThat(tmp.resolve("raw-wiktextract-data-en.jsonl.gz.part")).doesNotExist();
    }

    @Test
    void should_not_request_body_when_target_already_exists() throws Exception {
        // Given
        Files.createFile(tmp.resolve("raw-wiktextract-data-en-2026-05-01.jsonl.gz"));
        stubHeadOk(lastModified("Fri, 01 May 2026 10:00:00 GMT"));

        // When
        unit.download();

        // Then
        verify(httpClient, never()).send(argThat(request -> "GET".equals(request.method())), any());
    }

    @Test
    void should_not_request_body_when_head_returns_error() throws Exception {
        // Given
        doReturn(headResponse).when(httpClient).send(argThat(request -> "HEAD".equals(request.method())), any());
        when(headResponse.statusCode()).thenReturn(404);

        // When
        unit.download();

        // Then
        verify(httpClient, never()).send(argThat(request -> "GET".equals(request.method())), any());
        assertThat(tmp).isEmptyDirectory();
    }

    @Test
    void should_discard_partial_file_when_body_returns_error() throws Exception {
        // Given
        Path partPath = tmp.resolve("raw-wiktextract-data-en.jsonl.gz.part");
        stubHeadOk(lastModified("Fri, 01 May 2026 10:00:00 GMT"));
        stubGet(500, partPath);

        // When
        unit.download();

        // Then
        assertThat(tmp).isEmptyDirectory();
    }

    @Test
    void should_allow_hours_for_body_transfer_when_downloading() throws Exception {
        // Given
        Path partPath = tmp.resolve("raw-wiktextract-data-en.jsonl.gz.part");
        stubHeadOk(lastModified("Fri, 01 May 2026 10:00:00 GMT"));
        stubGet(200, partPath);

        // When
        unit.download();

        // Then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(captor.capture(), any());
        assertThat(captor.getAllValues().getLast().timeout())
                .hasValueSatisfying(timeout -> assertThat(timeout).isGreaterThanOrEqualTo(Duration.ofHours(1)));
    }

    @Test
    void should_use_dictionary_path_when_lang_is_english() {
        // When
        String url = KaikkiDumpDownloader.buildUrl("en");

        // Then
        assertThat(url).endsWith("/dictionary/raw-wiktextract-data.jsonl.gz");
    }

    @Test
    void should_use_lang_wiktionary_path_when_lang_is_other() {
        // When
        String url = KaikkiDumpDownloader.buildUrl("fr");

        // Then
        assertThat(url).endsWith("/frwiktionary/raw-wiktextract-data.jsonl.gz");
    }

    private static HttpHeaders lastModified(String value) {
        return HttpHeaders.of(Map.of("last-modified", List.of(value)), (k, v) -> true);
    }

    private void stubHeadOk(HttpHeaders headers) throws Exception {
        doReturn(headResponse).when(httpClient).send(argThat(request -> "HEAD".equals(request.method())), any());
        when(headResponse.statusCode()).thenReturn(200);
        when(headResponse.headers()).thenReturn(headers);
    }

    private void stubGet(int statusCode, Path partPath) throws Exception {
        HttpResponse<Path> bodyResponse = mock();
        when(bodyResponse.statusCode()).thenReturn(statusCode);
        doAnswer(invocation -> {
            Files.write(partPath, new byte[0]);
            return bodyResponse;
        }).when(httpClient).send(argThat(request -> "GET".equals(request.method())), any());
    }
}
