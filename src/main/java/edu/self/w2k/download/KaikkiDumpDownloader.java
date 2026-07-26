package edu.self.w2k.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import edu.self.w2k.progress.ProgressListener;
import edu.self.w2k.progress.ProgressListener.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KaikkiDumpDownloader implements DumpDownloader {

    private static final String BASE_URL = "https://kaikki.org";
    private static final String DUMP_FILENAME = "raw-wiktextract-data.jsonl.gz";

    /** Applies to the HEAD probe only — a metadata round-trip should be quick. */
    private static final Duration HEAD_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Upper bound for the body transfer. Dumps are multi-gigabyte, so this is
     * deliberately generous: it exists only to stop a wedged connection hanging forever.
     */
    private static final Duration BODY_TIMEOUT = Duration.ofHours(6);

    private static final long BYTES_PER_MB = 1024L * 1024L;
    private static final int TRANSFER_BUFFER_BYTES = 64 * 1024;

    /** Progress is emitted at most once per this many bytes, to keep listeners cheap. */
    private static final long PROGRESS_INTERVAL_BYTES = 4 * BYTES_PER_MB;

    private final String lang;
    private final HttpClient httpClient;
    private final Path dumpsDir;
    private final ProgressListener progress;

    public KaikkiDumpDownloader(String lang) {
        this(lang, Path.of("dumps"), ProgressListener.NOOP);
    }

    public KaikkiDumpDownloader(String lang, Path dumpsDir, ProgressListener progress) {
        this(lang, defaultHttpClient(), dumpsDir, progress);
    }

    KaikkiDumpDownloader(String lang, HttpClient httpClient, Path dumpsDir) {
        this(lang, httpClient, dumpsDir, ProgressListener.NOOP);
    }

    KaikkiDumpDownloader(String lang, HttpClient httpClient, Path dumpsDir, ProgressListener progress) {
        this.lang = lang;
        this.httpClient = httpClient;
        this.dumpsDir = dumpsDir;
        this.progress = progress;
    }

    private static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public DownloadResult download() throws IOException {
        Files.createDirectories(dumpsDir);

        String url = buildUrl(lang);
        log.info("Checking {}", url);

        Path partPath = dumpsDir.resolve("raw-wiktextract-data-" + lang + ".jsonl.gz.part");

        try {
            HttpResponse<Void> head = httpClient.send(headRequest(url), HttpResponse.BodyHandlers.discarding());
            if (head.statusCode() != 200) {
                throw new IOException("HTTP %d for %s".formatted(head.statusCode(), url));
            }

            String generatedDate = buildGeneratedDate(head.headers());
            Path dumpPath = dumpsDir.resolve("raw-wiktextract-data-%s-%s.jsonl.gz".formatted(lang, generatedDate));
            if (Files.exists(dumpPath)) {
                log.info("Dump already exists at {}. Delete it to re-download.", dumpPath);
                return new DownloadResult(dumpPath, true);
            }

            long total = contentLength(head.headers());
            log.info("Downloading {} MB to {} (generated: {})", megabytes(total), dumpPath, generatedDate);

            HttpResponse<InputStream> response =
                    httpClient.send(getRequest(url), HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw new IOException("HTTP %d for %s".formatted(response.statusCode(), url));
                }
                try (OutputStream out = Files.newOutputStream(partPath)) {
                    transfer(body, out, total);
                }
            }

            Files.move(partPath, dumpPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Download complete: {} ({} MB, generated: {})",
                     dumpPath, Files.size(dumpPath) / BYTES_PER_MB, generatedDate);
            return new DownloadResult(dumpPath, false);
        }
        catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Download interrupted for lang: " + lang);
        }
        finally {
            discardPartialFile(partPath);
        }
    }

    /**
     * Streams the body into {@code out}, reporting progress and honouring interruption. A manual loop
     * is used rather than {@code BodyHandlers.ofFile}, which offers neither a byte-level callback nor
     * a cancellation point. The atomic {@code .part} rename stays in the caller, unchanged.
     */
    private void transfer(InputStream in, OutputStream out, long total) throws IOException {
        byte[] buffer = new byte[TRANSFER_BUFFER_BYTES];
        long done = 0;
        long lastReported = 0;

        int read;
        while ((read = in.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("Download cancelled after %d bytes".formatted(done));
            }
            out.write(buffer, 0, read);
            done += read;
            if (done - lastReported >= PROGRESS_INTERVAL_BYTES) {
                lastReported = done;
                progress.onProgress(Stage.DOWNLOAD, done, total);
            }
        }
        progress.onProgress(Stage.DOWNLOAD, done, total);
    }

    private static void discardPartialFile(Path partPath) {
        try {
            Files.deleteIfExists(partPath);
        }
        catch (IOException e) {
            // Never mask the outcome of the download itself with a cleanup failure.
            log.warn("Failed to delete partial file {}: {}", partPath, e.getLocalizedMessage());
        }
    }

    private static HttpRequest headRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(HEAD_TIMEOUT)
                .build();
    }

    private static HttpRequest getRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(BODY_TIMEOUT)
                .build();
    }

    private static long contentLength(HttpHeaders headers) {
        return headers.firstValueAsLong("content-length").orElse(ProgressListener.TOTAL_UNKNOWN);
    }

    private static String megabytes(long bytes) {
        return bytes < 0 ? "?" : Long.toString(bytes / BYTES_PER_MB);
    }

    private String buildGeneratedDate(HttpHeaders headers) {
        String lastModified = headers.firstValue("last-modified").orElse(null);
        if (lastModified == null) {
            return "unknown";
        }

        try {
            return ZonedDateTime
                    .parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception _) {
            return lastModified;
        }
    }

    /**
     * Builds the kaikki.org download URL for the given language edition.
     * English maps to the main {@code /dictionary/} path; all other editions use
     * {@code /{lang}wiktionary/}.
     */
    static String buildUrl(String lang) {
        String path = "en".equals(lang)
                ? "/dictionary/"
                : "/%swiktionary/".formatted(lang);
        return BASE_URL + path + DUMP_FILENAME;
    }
}
