package edu.self.w2k.kindling;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KindlingDownloader {

    private static final String RELEASE_BASE = "https://github.com/ciscoriordan/kindling/releases/download";
    private static final String API_BASE = "https://api.github.com/repos/ciscoriordan/kindling/releases/tags";

    private final HttpFetcher fetcher;

    public KindlingDownloader(HttpClient httpClient) {
        this.fetcher = new DefaultHttpFetcher(httpClient);
    }

    KindlingDownloader(HttpFetcher fetcher) {
        this.fetcher = fetcher;
    }

    public Path download(String version, KindlingPlatform platform, Path destDir)
            throws IOException, KindlingException {
        String assetName = platform.assetName();
        String expectedSha256 = resolveDigest(version, platform, assetName);

        URI downloadUri = URI.create("%s/%s/%s".formatted(RELEASE_BASE, version, assetName));
        Path partPath = destDir.resolve("." + assetName + ".part");
        Path finalPath = destDir.resolve(assetName);

        Files.createDirectories(destDir);
        log.info("Downloading kindling {} ({}) from {}", version, assetName, downloadUri);

        Path downloaded = fetcher.getFile(downloadUri, partPath);
        try {
            long sizeMb = Files.size(downloaded) / (1024 * 1024);
            String actualSha256 = sha256(downloaded);
            log.info("SHA-256 expected={}, actual={}, size={} MB", expectedSha256, actualSha256, sizeMb);

            if (!expectedSha256.equalsIgnoreCase(actualSha256)) {
                Files.deleteIfExists(downloaded);
                throw new KindlingException(
                        "SHA-256 mismatch for %s: expected %s but got %s"
                                .formatted(assetName, expectedSha256, actualSha256));
            }

            Files.move(downloaded, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            markExecutable(finalPath);
            log.info("kindling binary ready at {}", finalPath);
            return finalPath;
        } catch (KindlingException e) {
            throw e;
        } catch (IOException e) {
            Files.deleteIfExists(partPath);
            throw e;
        }
    }

    private String resolveDigest(String version, KindlingPlatform platform, String assetName)
            throws IOException, KindlingException {
        if (version.equals(KindlingRelease.DEFAULT_VERSION)
                && KindlingRelease.DEFAULT_ASSETS.containsKey(platform)) {
            return KindlingRelease.DEFAULT_ASSETS.get(platform).sha256();
        }
        String json = fetcher.getString(URI.create("%s/%s".formatted(API_BASE, version)));
        return parseDigestFromJson(json, assetName, version);
    }

    private static String parseDigestFromJson(String json, String assetName, String version)
            throws KindlingException {
        String nameToken = "\"name\":\"" + assetName + "\"";
        int nameIdx = json.indexOf(nameToken);
        if (nameIdx < 0) {
            throw new KindlingException(
                    "kindling release %s has no asset %s".formatted(version, assetName));
        }
        String after = json.substring(nameIdx);
        int digestIdx = after.indexOf("\"digest\":\"sha256:");
        if (digestIdx < 0) {
            throw new KindlingException(
                    "kindling release %s asset %s has no digest field".formatted(version, assetName));
        }
        int start = digestIdx + "\"digest\":\"sha256:".length();
        int end = after.indexOf("\"", start);
        if (end < 0) {
            throw new KindlingException(
                    "Malformed digest in GitHub API response for %s %s".formatted(version, assetName));
        }
        return after.substring(start, end);
    }

    static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available", e);
        }
    }

    private static void markExecutable(Path path) {
        try {
            var perms = new java.util.HashSet<>(Files.getPosixFilePermissions(path));
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(path, perms);
        } catch (UnsupportedOperationException ignored) {
            // Windows — no POSIX permissions
        } catch (IOException e) {
            log.warn("Could not mark {} as executable: {}", path, e.getMessage());
        }
    }

    private static final class DefaultHttpFetcher implements HttpFetcher {

        private final HttpClient client;

        DefaultHttpFetcher(HttpClient client) {
            this.client = client;
        }

        @Override
        public Path getFile(URI uri, Path dest) throws IOException {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(60)).build();
                HttpResponse<Path> resp = client.send(req, HttpResponse.BodyHandlers.ofFile(dest));
                if (resp.statusCode() != 200) {
                    Files.deleteIfExists(dest);
                    throw new IOException("HTTP " + resp.statusCode() + " downloading " + uri);
                }
                return dest;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Download interrupted", e);
            }
        }

        @Override
        public String getString(URI uri) throws IOException {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofSeconds(60))
                        .header("Accept", "application/vnd.github+json")
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    throw new IOException("HTTP " + resp.statusCode() + " fetching " + uri);
                }
                return resp.body();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }
    }
}
