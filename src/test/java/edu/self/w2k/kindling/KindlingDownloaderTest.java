package edu.self.w2k.kindling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KindlingDownloaderTest {

    /**
     * Stub HttpFetcher that serves fixed byte[] payloads keyed by URI string.
     * Writing bytes to dest simulates a real download.
     */
    private static final class StubFetcher implements HttpFetcher {
        private final Map<String, byte[]> filePayloads = new ConcurrentHashMap<>();
        private final Map<String, String> stringResponses = new ConcurrentHashMap<>();

        StubFetcher file(String uri, byte[] payload) {
            filePayloads.put(uri, payload);
            return this;
        }

        StubFetcher json(String uri, String json) {
            stringResponses.put(uri, json);
            return this;
        }

        @Override
        public Path getFile(URI uri, Path dest) throws IOException {
            byte[] payload = filePayloads.get(uri.toString());
            if (payload == null) throw new IOException("No stub for " + uri);
            Files.write(dest, payload);
            return dest;
        }

        @Override
        public String getString(URI uri) throws IOException {
            String response = stringResponses.get(uri.toString());
            if (response == null) throw new IOException("No stub for " + uri);
            return response;
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // ── happy path: non-default version, digest fetched from API JSON ─────────

    @Test
    void download_happyPath(@TempDir Path tempDir) throws Exception {
        String version = "v9.99.0";
        KindlingPlatform platform = KindlingPlatform.LINUX_X64;
        String assetName = platform.assetName();

        byte[] fakeBytes = "fake-binary-content".getBytes();
        String expectedSha256 = sha256(fakeBytes);

        String apiUrl = "https://api.github.com/repos/ciscoriordan/kindling/releases/tags/" + version;
        String downloadUrl = "https://github.com/ciscoriordan/kindling/releases/download/" + version + "/" + assetName;

        String fakeApiJson = """
                {"assets":[{"name":"%s","digest":"sha256:%s"}]}
                """.formatted(assetName, expectedSha256);

        StubFetcher stub = new StubFetcher()
                .json(apiUrl, fakeApiJson)
                .file(downloadUrl, fakeBytes);

        KindlingDownloader downloader = new KindlingDownloader(stub);
        Path result = downloader.download(version, platform, tempDir);

        assertEquals(tempDir.resolve(assetName), result);
        assertTrue(Files.exists(result), "Binary file must exist after download");
        assertFalse(Files.exists(tempDir.resolve("." + assetName + ".part")),
                "Part file must be removed after successful download");
    }

    // ── tampered file: sha256 mismatch → KindlingException + part file removed

    @Test
    void download_sha256Mismatch_throwsAndCleansUp(@TempDir Path tempDir) throws Exception {
        String version = "v9.99.0";
        KindlingPlatform platform = KindlingPlatform.LINUX_X64;
        String assetName = platform.assetName();

        byte[] fakeBytes = "fake-binary-content".getBytes();
        String expectedSha256 = sha256("different-content".getBytes());

        String apiUrl = "https://api.github.com/repos/ciscoriordan/kindling/releases/tags/" + version;
        String downloadUrl = "https://github.com/ciscoriordan/kindling/releases/download/" + version + "/" + assetName;

        String fakeApiJson = """
                {"assets":[{"name":"%s","digest":"sha256:%s"}]}
                """.formatted(assetName, expectedSha256);

        StubFetcher stub = new StubFetcher()
                .json(apiUrl, fakeApiJson)
                .file(downloadUrl, fakeBytes);

        KindlingDownloader downloader = new KindlingDownloader(stub);

        KindlingException ex = assertThrows(KindlingException.class,
                () -> downloader.download(version, platform, tempDir));
        assertTrue(ex.getMessage().contains("SHA-256 mismatch"), "exception must mention SHA-256 mismatch");
        // The downloaded (part) file must be removed on mismatch
        assertFalse(Files.exists(tempDir.resolve("." + assetName + ".part")),
                "Part file must be deleted on sha256 mismatch");
        assertFalse(Files.exists(tempDir.resolve(assetName)),
                "Final binary must not exist on sha256 mismatch");
    }

    // ── non-default version uses API path (same as happy path, verifying the branch) ─

    @Test
    void download_nonDefaultVersion_usesApiForDigest(@TempDir Path tempDir) throws Exception {
        String version = "v1.2.3-custom";
        KindlingPlatform platform = KindlingPlatform.MAC_APPLE_SILICON;
        String assetName = platform.assetName();

        byte[] fakeBytes = "mac-binary".getBytes();
        String expectedSha256 = sha256(fakeBytes);

        String apiUrl = "https://api.github.com/repos/ciscoriordan/kindling/releases/tags/" + version;
        String downloadUrl = "https://github.com/ciscoriordan/kindling/releases/download/" + version + "/" + assetName;

        String fakeApiJson = """
                {"assets":[{"name":"%s","digest":"sha256:%s"}]}
                """.formatted(assetName, expectedSha256);

        StubFetcher stub = new StubFetcher()
                .json(apiUrl, fakeApiJson)
                .file(downloadUrl, fakeBytes);

        KindlingDownloader downloader = new KindlingDownloader(stub);
        Path result = downloader.download(version, platform, tempDir);

        assertTrue(Files.exists(result), "Binary must exist");
        assertEquals(tempDir.resolve(assetName), result);
    }
}
