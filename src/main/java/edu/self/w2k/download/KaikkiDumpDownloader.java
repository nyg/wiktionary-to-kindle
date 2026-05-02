package edu.self.w2k.download;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KaikkiDumpDownloader implements DumpDownloader {

    private static final String BASE_URL = "https://kaikki.org";
    private static final String DUMP_FILENAME = "raw-wiktextract-data.jsonl.gz";

    private final String lang;
    private final Path dumpPath;

    @Override
    public void download() {

        if (Files.exists(dumpPath)) {
            log.info("Dump already exists at {}. Delete it to re-download.", dumpPath);
            return;
        }

        String url = buildUrl(lang);
        log.info("Downloading {} → {}", url, dumpPath);

        Path partPath = dumpPath.resolveSibling(dumpPath.getFileName() + ".part");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .build();

        try {
            HttpResponse<Path> response = client.send(
                    request, HttpResponse.BodyHandlers.ofFile(partPath));

            if (response.statusCode() != 200) {
                log.error("Download failed — HTTP {}", response.statusCode());
                Files.deleteIfExists(partPath);
                return;
            }

            Files.move(partPath, dumpPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Download complete: {} ({} MB)", dumpPath, Files.size(dumpPath) / (1024 * 1024));
        }
        catch (Exception e) {
            log.error("Download failed: {}", e.getLocalizedMessage(), e);
            try {
                Files.deleteIfExists(partPath);
            }
            catch (Exception ignored) {
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
