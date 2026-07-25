package edu.self.w2k.download;

import java.io.IOException;
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
import java.util.OptionalLong;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
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

    private final String lang;
    private final HttpClient httpClient;
    private final Path dumpsDir;

    public KaikkiDumpDownloader(String lang) {
        this(lang,
             HttpClient.newBuilder()
                     .followRedirects(HttpClient.Redirect.NORMAL)
                     .connectTimeout(Duration.ofSeconds(30))
                     .build(),
             Path.of("dumps"));
    }

    @Override
    public void download() {
        try {
            Files.createDirectories(dumpsDir);
        } catch (Exception e) {
            log.error("Failed to create dump directory: {}", e.getLocalizedMessage(), e);
            return;
        }

        String url = buildUrl(lang);
        log.info("Checking {} (headers...)", url);

        Path partPath = dumpsDir.resolve("raw-wiktextract-data-" + lang + ".jsonl.gz.part");

        try {
            HttpResponse<Void> head = httpClient.send(headRequest(url), HttpResponse.BodyHandlers.discarding());
            if (head.statusCode() != 200) {
                log.error("Download failed — HTTP {}", head.statusCode());
                return;
            }

            String generatedDate = buildGeneratedDate(head.headers());
            Path dumpPath = dumpsDir.resolve("raw-wiktextract-data-%s-%s.jsonl.gz".formatted(lang, generatedDate));
            if (Files.exists(dumpPath)) {
                log.info("Dump already exists at {}. Delete it to re-download.", dumpPath);
                return;
            }

            log.info("Downloading {} MB to {} (generated: {})", contentLengthMb(head.headers()), dumpPath, generatedDate);
            HttpResponse<Path> response = httpClient.send(getRequest(url), HttpResponse.BodyHandlers.ofFile(partPath));
            if (response.statusCode() != 200) {
                log.error("Download failed — HTTP {}", response.statusCode());
                return;
            }

            Files.move(partPath, dumpPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Download complete: {} ({} MB, generated: {})", dumpPath, Files.size(dumpPath) / (1024 * 1024), generatedDate);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            log.warn("Download interrupted for lang: {}", lang);
        } catch (IOException e) {
            log.error("Download failed", e);
        } finally {
            try {
                Files.deleteIfExists(partPath);
            } catch (IOException e) {
                log.warn("Failed to delete partial file", e);
            }
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

    private static String contentLengthMb(HttpHeaders headers) {
        OptionalLong length = headers.firstValueAsLong("content-length");
        return length.isPresent() ? Long.toString(length.getAsLong() / (1024 * 1024)) : "?";
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
