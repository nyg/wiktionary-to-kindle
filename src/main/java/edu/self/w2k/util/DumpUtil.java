package edu.self.w2k.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DumpUtil {

    private static final Logger LOG = Logger.getLogger(DumpUtil.class.getName());

    private static final String KAIKKI_URL = "https://kaikki.org/dictionary/raw-wiktextract-data.jsonl.gz";
    private static final Path DUMP_PATH = Paths.get("dumps/raw-wiktextract-data.jsonl.gz");

    /**
     * Downloads the kaikki.org raw wiktextract JSONL dump to {@code dumps/}.
     * If the file already exists the download is skipped.
     */
    public static void download() {

        if (Files.exists(DUMP_PATH)) {
            LOG.info("Dump already exists at " + DUMP_PATH + ". Delete it to re-download.");
            return;
        }

        LOG.info("Downloading " + KAIKKI_URL + " to " + DUMP_PATH + " …");

        Path partPath = DUMP_PATH.resolveSibling(DUMP_PATH.getFileName() + ".part");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(KAIKKI_URL))
                .timeout(Duration.ofMinutes(60))
                .build();

        try {
            HttpResponse<Path> response = client.send(
                    request, HttpResponse.BodyHandlers.ofFile(partPath));

            if (response.statusCode() != 200) {
                LOG.severe("HTTP " + response.statusCode() + " — download failed.");
                Files.deleteIfExists(partPath);
                return;
            }

            Files.move(partPath, DUMP_PATH, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Download complete: " + DUMP_PATH + " (" + Files.size(DUMP_PATH) + " bytes)");
        }
        catch (Exception e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
            try { Files.deleteIfExists(partPath); } catch (Exception ignored) {}
        }
    }

    public static Path getDumpPath() {
        return DUMP_PATH;
    }

    private DumpUtil() {
        throw new RuntimeException("Class should not be instantiated.");
    }
}

