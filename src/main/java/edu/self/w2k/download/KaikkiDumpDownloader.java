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

    private static final String KAIKKI_URL = "https://kaikki.org/dictionary/raw-wiktextract-data.jsonl.gz";

    private final Path dumpPath;

    @Override
    public void download() {

        if (Files.exists(dumpPath)) {
            log.info("Dump already exists at {}. Delete it to re-download.", dumpPath);
            return;
        }

        log.info("Downloading {} → {}", KAIKKI_URL, dumpPath);

        Path partPath = dumpPath.resolveSibling(dumpPath.getFileName() + ".part");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(KAIKKI_URL))
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
}
