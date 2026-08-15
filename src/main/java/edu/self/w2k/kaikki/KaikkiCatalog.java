package edu.self.w2k.kaikki;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import edu.self.w2k.config.AppPaths;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KaikkiCatalog {

    static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private static final String BASE_URL = "https://kaikki.org";

    private static final Pattern SAFE_EDITION = Pattern.compile("[a-z]+");

    private static final char FIELD_SEPARATOR = '\t';

    private final PageFetcher fetcher;
    private final Path cacheRoot;
    private final Duration ttl;

    public KaikkiCatalog() {
        this(new HttpPageFetcher(defaultHttpClient()), AppPaths.cacheDir().resolve("catalog"), DEFAULT_TTL);
    }

    public KaikkiCatalog(PageFetcher fetcher, Path cacheRoot, Duration ttl) {
        this.fetcher = fetcher;
        this.cacheRoot = cacheRoot;
        this.ttl = ttl;
    }

    private static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public List<String> editions() {
        return resolve(cacheRoot.resolve("editions.txt"), BASE_URL + "/",
                KaikkiHtml::parseEditions, KaikkiCatalog::decodeEdition, code -> code);
    }

    public List<KaikkiLanguage> languagesFor(String edition) {
        if (edition == null || !SAFE_EDITION.matcher(edition).matches()) {
            return List.of();
        }
        return resolve(cacheRoot.resolve("languages-" + edition + ".txt"),
                "%s/%s/".formatted(BASE_URL, KaikkiHtml.editionPath(edition)),
                KaikkiHtml::parseLanguages, KaikkiCatalog::decodeLanguage, KaikkiCatalog::encodeLanguage);
    }

    private <T> List<T> resolve(Path cacheFile, String url, Function<String, List<T>> parse,
                                Function<String, T> decode, Function<T, String> encode) {
        List<T> cached = readCache(cacheFile, decode);
        if (!cached.isEmpty() && isFresh(cacheFile)) {
            return cached;
        }
        try {
            List<T> fetched = parse.apply(fetcher.fetch(URI.create(url)));
            if (fetched.isEmpty()) {
                log.debug("Parsed nothing usable from {}; keeping {} cached entries", url, cached.size());
                return cached;
            }
            writeCache(cacheFile, fetched, encode);
            return fetched;
        }
        catch (IOException | RuntimeException e) {
            log.debug("Could not refresh {}: {}", url, e.toString());
            return cached;
        }
    }

    private boolean isFresh(Path cacheFile) {
        try {
            Instant modified = Files.getLastModifiedTime(cacheFile).toInstant();
            return modified.plus(ttl).isAfter(Instant.now());
        }
        catch (IOException _) {
            return false;
        }
    }

    private <T> List<T> readCache(Path cacheFile, Function<String, T> decode) {
        if (!Files.isRegularFile(cacheFile)) {
            return List.of();
        }
        try {
            List<T> entries = new ArrayList<>();
            for (String line : Files.readAllLines(cacheFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                T entry = decode.apply(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return List.copyOf(entries);
        }
        catch (IOException | RuntimeException e) {
            log.debug("Could not read cache {}: {}", cacheFile, e.toString());
            return List.of();
        }
    }

    private <T> void writeCache(Path cacheFile, List<T> entries, Function<T, String> encode) {
        Path part = cacheFile.resolveSibling("." + cacheFile.getFileName() + ".part");
        try {
            Files.createDirectories(cacheFile.getParent());
            Files.write(part, entries.stream().map(encode).toList(), StandardCharsets.UTF_8);
            Files.move(part, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException | RuntimeException e) {
            log.debug("Could not write cache {}: {}", cacheFile, e.toString());
            discard(part);
        }
    }

    private void discard(Path part) {
        try {
            Files.deleteIfExists(part);
        }
        catch (IOException e) {
            log.debug("Could not remove {}: {}", part, e.toString());
        }
    }

    private static String decodeEdition(String line) {
        String code = line.strip();
        return SAFE_EDITION.matcher(code).matches() ? code : null;
    }

    private static String encodeLanguage(KaikkiLanguage language) {
        return language.name() + FIELD_SEPARATOR + language.senses();
    }

    private static KaikkiLanguage decodeLanguage(String line) {
        int separator = line.lastIndexOf(FIELD_SEPARATOR);
        if (separator <= 0) {
            return null;
        }
        try {
            return new KaikkiLanguage(line.substring(0, separator),
                    Long.parseLong(line.substring(separator + 1).strip()));
        }
        catch (NumberFormatException _) {
            return null;
        }
    }

    record HttpPageFetcher(HttpClient client) implements PageFetcher {

        @Override
        public String fetch(URI uri) throws IOException {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofSeconds(30))
                        .header("Accept", "text/html")
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode() + " fetching " + uri);
                }
                return response.body();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }
    }
}
