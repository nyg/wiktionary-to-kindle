package edu.self.w2k.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.self.w2k.progress.ProgressListener;
import edu.self.w2k.progress.ProgressListener.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KaikkiDumpDownloaderTest {

    private static final String LAST_MODIFIED = "Fri, 01 May 2026 10:00:00 GMT";
    private static final String DUMP_NAME = "raw-wiktextract-data-en-2026-05-01.jsonl.gz";
    private static final String PART_NAME = "raw-wiktextract-data-en.jsonl.gz.part";
    private static final byte[] BODY = "compressed-dump-bytes".getBytes(StandardCharsets.UTF_8);

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
        stubHeadOk(headers(LAST_MODIFIED, BODY.length));
        stubGet(200, BODY);

        // When
        DownloadResult result = unit.download();

        // Then
        assertThat(result.dumpPath()).isEqualTo(tmp.resolve(DUMP_NAME));
        assertThat(result.alreadyPresent()).isFalse();
        assertThat(tmp.resolve(DUMP_NAME)).exists().hasBinaryContent(BODY);
        assertThat(tmp.resolve(PART_NAME)).doesNotExist();
    }

    @Test
    void should_keep_existing_dump_when_target_already_exists() throws Exception {
        // Given
        Path existingDump = tmp.resolve(DUMP_NAME);
        Files.createFile(existingDump);
        stubHeadOk(headers(LAST_MODIFIED, BODY.length));

        // When
        DownloadResult result = unit.download();

        // Then
        assertThat(result.dumpPath()).isEqualTo(existingDump);
        assertThat(result.alreadyPresent()).isTrue();
        assertThat(existingDump).exists();
        assertThat(tmp.resolve(PART_NAME)).doesNotExist();
    }

    @Test
    void should_not_request_body_when_target_already_exists() throws Exception {
        // Given
        Files.createFile(tmp.resolve(DUMP_NAME));
        stubHeadOk(headers(LAST_MODIFIED, BODY.length));

        // When
        unit.download();

        // Then
        verify(httpClient, never()).send(argThat(request -> "GET".equals(request.method())), any());
    }

    @Test
    void should_fail_without_requesting_body_when_head_returns_error() throws Exception {
        // Given
        doReturn(headResponse).when(httpClient).send(argThat(request -> "HEAD".equals(request.method())), any());
        when(headResponse.statusCode()).thenReturn(404);

        // When / Then
        assertThatIOException()
                .isThrownBy(() -> unit.download())
                .withMessageContaining("HTTP 404");
        verify(httpClient, never()).send(argThat(request -> "GET".equals(request.method())), any());
        assertThat(tmp).isEmptyDirectory();
    }

    @Test
    void should_fail_and_discard_partial_file_when_body_returns_error() throws Exception {
        // Given
        stubHeadOk(headers(LAST_MODIFIED, BODY.length));
        stubGet(500, BODY);

        // When / Then
        assertThatIOException()
                .isThrownBy(() -> unit.download())
                .withMessageContaining("HTTP 500");
        assertThat(tmp).isEmptyDirectory();
    }

    @Test
    void should_allow_hours_for_body_transfer_when_downloading() throws Exception {
        // Given
        stubHeadOk(headers(LAST_MODIFIED, BODY.length));
        stubGet(200, BODY);

        // When
        unit.download();

        // Then
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(captor.capture(), any());
        assertThat(captor.getAllValues().getLast().timeout())
                .hasValueSatisfying(timeout -> assertThat(timeout).isGreaterThanOrEqualTo(Duration.ofHours(1)));
    }

    @Test
    void should_report_transferred_bytes_against_content_length_when_downloading() throws Exception {
        // Given
        List<long[]> reports = new ArrayList<>();
        unit = new KaikkiDumpDownloader("en", httpClient, tmp,
                                        (stage, done, total) -> {
                                            assertThat(stage).isEqualTo(Stage.DOWNLOAD);
                                            reports.add(new long[] {done, total});
                                        });
        stubHeadOk(headers(LAST_MODIFIED, BODY.length));
        stubGet(200, BODY);

        // When
        unit.download();

        // Then
        assertThat(reports.getLast()).containsExactly(BODY.length, BODY.length);
    }

    @Test
    void should_report_unknown_total_when_content_length_is_absent() throws Exception {
        // Given
        List<long[]> reports = new ArrayList<>();
        unit = new KaikkiDumpDownloader("en", httpClient, tmp,
                                        (_, done, total) -> reports.add(new long[] {done, total}));
        stubHeadOk(HttpHeaders.of(Map.of("last-modified", List.of(LAST_MODIFIED)), (k, v) -> true));
        stubGet(200, BODY);

        // When
        unit.download();

        // Then
        assertThat(reports.getLast()).containsExactly(BODY.length, ProgressListener.TOTAL_UNKNOWN);
    }

    @Test
    void should_abort_and_discard_partial_file_when_thread_is_interrupted() throws Exception {
        // Given
        stubHeadOk(headers(LAST_MODIFIED, BODY.length));
        stubGet(200, BODY);
        Thread.currentThread().interrupt();

        try {
            // When / Then
            assertThatExceptionOfType(InterruptedIOException.class)
                    .isThrownBy(() -> unit.download())
                    .withMessageContaining("cancelled");
            assertThat(tmp).isEmptyDirectory();
        }
        finally {
            Thread.interrupted(); // clear the flag so it cannot leak into other tests
        }
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

    private static HttpHeaders headers(String lastModified, long contentLength) {
        return HttpHeaders.of(Map.of("last-modified", List.of(lastModified),
                                     "content-length", List.of(Long.toString(contentLength))),
                              (k, v) -> true);
    }

    private void stubHeadOk(HttpHeaders headers) throws Exception {
        doReturn(headResponse).when(httpClient).send(argThat(request -> "HEAD".equals(request.method())), any());
        when(headResponse.statusCode()).thenReturn(200);
        when(headResponse.headers()).thenReturn(headers);
    }

    private void stubGet(int statusCode, byte[] body) throws Exception {
        HttpResponse<InputStream> bodyResponse = mock();
        when(bodyResponse.statusCode()).thenReturn(statusCode);
        when(bodyResponse.body()).thenReturn(new ByteArrayInputStream(body));
        doReturn(bodyResponse).when(httpClient).send(argThat(request -> "GET".equals(request.method())), any());
    }
}
