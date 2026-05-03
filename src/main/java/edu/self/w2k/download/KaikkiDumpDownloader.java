package edu.self.w2k.download;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class KaikkiDumpDownloader implements DumpDownloader {

    private static final String BASE_URL = "https://kaikki.org";
    private static final String DUMP_FILENAME = "raw-wiktextract-data.jsonl.gz";

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
        log.info("Downloading {} (checking headers...)", url);

        Path partPath = dumpsDir.resolve("raw-wiktextract-data-" + lang + ".jsonl.gz.part");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .build();

        try {
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(partPath));
            if (response.statusCode() != 200) {
                log.error("Download failed — HTTP {}", response.statusCode());
                Files.deleteIfExists(partPath);
                return;
            }

            String lastModified = response.headers().firstValue("last-modified").orElse(null);
            String generatedDate = "unknown";
            if (lastModified != null) {
                try {
                    ZonedDateTime parsed = ZonedDateTime.parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME);
                    generatedDate = parsed.format(DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception e) {
                    generatedDate = lastModified;
                }
            }

            Path dumpPath = dumpsDir.resolve("raw-wiktextract-data-" + lang + "-" + generatedDate + ".jsonl.gz");
            if (Files.exists(dumpPath)) {
                log.info("Dump already exists at {}. Delete it to re-download.", dumpPath);
                Files.deleteIfExists(partPath);
                return;
            }

            Files.move(partPath, dumpPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Download complete: {} ({} MB, generated: {})", dumpPath, Files.size(dumpPath) / (1024 * 1024), generatedDate);
        } catch (Exception e) {
            log.error("Download failed: {}", e.getLocalizedMessage(), e);
            try {
                Files.deleteIfExists(partPath);
            } catch (Exception nested) {
                log.warn("Failed to delete partial file", nested);
            }
        }
    }

    /**
     * Builds the kaikki.org download URL for the given language edition.
     * English maps to the main {@code /dictionary/} path; all other editions use
     * {@code /{lang}wiktionary/}.
     */
    static String buildUrl(String lang) {
        String path = "en".equals(lang)
                ? "/dictionary/" + DUMP_FILENAME
                : "/" + lang + "wiktionary/" + DUMP_FILENAME;
        return BASE_URL + path;
    }
}
