package edu.self.w2k.kindling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KindlingCliResolverTest {

    /**
     * No-op stub fetcher used when we don't expect actual network calls.
     */
    private static final HttpFetcher NO_OP_FETCHER = new HttpFetcher() {
        @Override
        public Path getFile(URI uri, Path dest) throws IOException {
            throw new IOException("Unexpected getFile call: " + uri);
        }

        @Override
        public String getString(URI uri) throws IOException {
            throw new IOException("Unexpected getString call: " + uri);
        }
    };

    private static KindlingDownloader noOpDownloader() {
        return new KindlingDownloader(NO_OP_FETCHER);
    }

    // ── override binary that is executable → returned directly ────────────────

    @Test
    void resolve_overrideExecutable_returnsIt(@TempDir Path tempDir) throws Exception {
        Path fakeExe = tempDir.resolve("kindling-cli");
        Files.createFile(fakeExe);
        fakeExe.toFile().setExecutable(true);

        KindlingCliResolver resolver = new KindlingCliResolver(
                "v0", Optional.of(fakeExe), noOpDownloader());
        assertEquals(fakeExe, resolver.resolve());
    }

    // ── override binary not executable → KindlingException ───────────────────

    @Test
    void resolve_overrideNotExecutable_throws(@TempDir Path tempDir) throws Exception {
        Path nonExe = tempDir.resolve("kindling-cli");
        Files.createFile(nonExe);
        nonExe.toFile().setExecutable(false);

        KindlingCliResolver resolver = new KindlingCliResolver(
                "v0", Optional.of(nonExe), noOpDownloader());
        assertThrows(KindlingException.class, resolver::resolve);
    }

    // ── PATH probe finds executable → returned ───────────────────────────────

    @Test
    void resolve_pathProbeFindsExecutable_returnsIt(@TempDir Path tempDir) throws Exception {
        Path fakeExe = tempDir.resolve("kindling-cli");
        Files.createFile(fakeExe);
        fakeExe.toFile().setExecutable(true);

        KindlingCliResolver resolver = new KindlingCliResolver(
                "v0", Optional.empty(), noOpDownloader(),
                name -> Optional.of(fakeExe),
                (v, p) -> null);
        assertEquals(fakeExe, resolver.resolve());
    }

    // ── cache hit, no digest (non-default version) → cached binary returned ──

    @Test
    void resolve_cacheHit_noDigest_returnsCached(@TempDir Path cacheDir) throws Exception {
        // Simulate a cached binary already in place
        KindlingPlatform platform = KindlingPlatform.LINUX_X64;
        Path versionDir = cacheDir.resolve("v9.0.0");
        Files.createDirectories(versionDir);
        Path cachedBin = versionDir.resolve(platform.assetName());
        Files.createFile(cachedBin);
        cachedBin.toFile().setExecutable(true);

        // Use PATH probe to return the pre-created file; digest check is skipped (null → valid).
        KindlingCliResolver resolverViaPath = new KindlingCliResolver(
                "v9.0.0", Optional.empty(), noOpDownloader(),
                name -> Optional.of(cachedBin),
                (v, p) -> null);
        Path result = resolverViaPath.resolve();
        assertEquals(cachedBin, result);
    }

    // ── cache hit, digest mismatch → downloader called ───────────────────────

    @Test
    void resolve_cacheHit_digestMismatch_downloaderCalled(@TempDir Path tempDir) throws Exception {
        KindlingPlatform platform = KindlingPlatform.LINUX_X64;
        String version = "v9.0.0";

        // Create a fake cached binary with known content
        Path cacheDir = tempDir.resolve(version);
        Files.createDirectories(cacheDir);
        Path cachedBin = cacheDir.resolve(platform.assetName());
        Files.write(cachedBin, "old-content".getBytes());

        // Create a "downloaded" binary that the stub downloader will return
        Path downloadedBin = tempDir.resolve("downloaded-" + platform.assetName());
        Files.write(downloadedBin, "new-content".getBytes());
        downloadedBin.toFile().setExecutable(true);

        // Stub fetcher that serves new-content and provides sha256 matching it
        byte[] newContent = "new-content".getBytes();
        String newSha256 = KindlingDownloader.sha256(downloadedBin);
        String apiUrl = "https://api.github.com/repos/ciscoriordan/kindling/releases/tags/" + version;

        HttpFetcher stubFetcher = new HttpFetcher() {
            @Override
            public Path getFile(URI uri, Path dest) throws IOException {
                Files.write(dest, newContent);
                return dest;
            }

            @Override
            public String getString(URI uri) throws IOException {
                if (uri.toString().equals(apiUrl)) {
                    return "{\"assets\":[{\"name\":\"%s\",\"digest\":\"sha256:%s\"}]}"
                            .formatted(platform.assetName(), newSha256);
                }
                throw new IOException("Unexpected: " + uri);
            }
        };

        KindlingDownloader downloader = new KindlingDownloader(stubFetcher);

        // digestProvider returns "expected-hash" which won't match "old-content"
        KindlingCliResolver resolver = new KindlingCliResolver(
                version, Optional.empty(), downloader,
                name -> Optional.empty(),  // PATH probe misses
                (v, p) -> "expected-hash-that-wont-match") {
            @Override
            public Path resolve() throws IOException, KindlingException {
                // Override resolve to inject our tempDir as cache location
                Path cached = cacheDir.resolve(platform.assetName());
                if (Files.exists(cached)) {
                    String expected = "expected-hash-that-wont-match";
                    String actual;
                    try {
                        actual = KindlingDownloader.sha256(cached);
                    } catch (IOException _) {
                        actual = "";
                    }
                    if (!expected.equalsIgnoreCase(actual)) {
                        Files.deleteIfExists(cached);
                        return downloader.download(version, platform, cacheDir);
                    }
                    return cached;
                }
                return downloader.download(version, platform, cacheDir);
            }
        };

        Path result = resolver.resolve();
        assertTrue(Files.exists(result), "Downloaded binary must exist");
        // The old cached binary content should be replaced
        assertEquals("new-content", Files.readString(result));
    }

    // ── no cache → downloader called ─────────────────────────────────────────

    @Test
    void resolve_noCache_downloaderCalled(@TempDir Path tempDir) throws Exception {
        KindlingPlatform platform = KindlingPlatform.LINUX_X64;
        String version = "v9.0.0";

        byte[] binContent = "fresh-binary".getBytes();

        // Compute sha256 of the bytes we'll serve
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        String actualSha256 = java.util.HexFormat.of().formatHex(md.digest(binContent));

        HttpFetcher stubFetcher = new HttpFetcher() {
            @Override
            public Path getFile(URI uri, Path dest) throws IOException {
                Files.write(dest, binContent);
                return dest;
            }

            @Override
            public String getString(URI uri) throws IOException {
                return "{\"assets\":[{\"name\":\"%s\",\"digest\":\"sha256:%s\"}]}"
                        .formatted(platform.assetName(), actualSha256);
            }
        };

        KindlingDownloader downloader = new KindlingDownloader(stubFetcher);

        Path downloadCacheDir = tempDir.resolve(version);
        // Subclass to inject tempDir as cache
        KindlingCliResolver resolver = new KindlingCliResolver(
                version, Optional.empty(), downloader,
                name -> Optional.empty(),
                (v, p) -> null) {
            @Override
            public Path resolve() throws IOException, KindlingException {
                return downloader.download(version, platform, downloadCacheDir);
            }
        };

        Path result = resolver.resolve();
        assertTrue(Files.exists(result), "Downloaded binary must exist");
    }
}
