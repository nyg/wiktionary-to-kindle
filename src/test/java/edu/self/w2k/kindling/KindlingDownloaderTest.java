package edu.self.w2k.kindling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KindlingDownloaderTest {

    @Mock
    private HttpFetcher fetcher;

    @InjectMocks
    private KindlingDownloader unit;

    @TempDir
    Path destDir;

    @Test
    void should_download_and_return_final_path_when_sha256_matches() throws Exception {
        // Given
        byte[] payload = "test-payload-content".getBytes(StandardCharsets.UTF_8);
        String sha256 = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(payload));
        String version = "v1.0.0-test";
        KindlingPlatform platform = KindlingPlatform.LINUX_X64;
        String assetName = platform.assetName();
        String json = "{\"name\":\"" + assetName + "\",\"digest\":\"sha256:" + sha256 + "\"}";

        when(fetcher.getString(any(URI.class))).thenReturn(json);
        when(fetcher.getFile(any(URI.class), any(Path.class))).thenAnswer(inv -> {
            Path dest = inv.getArgument(1, Path.class);
            Files.write(dest, payload);
            return dest;
        });

        // When
        Path result = unit.download(version, platform, destDir);

        // Then
        assertThat(result)
                .isEqualTo(destDir.resolve(assetName))
                .exists();
        assertThat(destDir.resolve("." + assetName + ".part")).doesNotExist();
    }

    @Test
    void should_throw_when_sha256_mismatch() throws Exception {
        // Given
        byte[] correctPayload = "correct-content".getBytes(StandardCharsets.UTF_8);
        String correctSha256 = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(correctPayload));
        String version = "v1.0.0-test";
        KindlingPlatform platform = KindlingPlatform.LINUX_X64;
        String assetName = platform.assetName();
        String json = "{\"name\":\"" + assetName + "\",\"digest\":\"sha256:" + correctSha256 + "\"}";

        when(fetcher.getString(any(URI.class))).thenReturn(json);
        when(fetcher.getFile(any(URI.class), any(Path.class))).thenAnswer(inv -> {
            Path dest = inv.getArgument(1, Path.class);
            Files.write(dest, "wrong-content".getBytes(StandardCharsets.UTF_8));
            return dest;
        });

        // When / Then
        assertThatThrownBy(() -> unit.download(version, platform, destDir))
                .isInstanceOf(KindlingException.class)
                .hasMessageContaining("SHA-256 mismatch");
        assertThat(destDir.resolve("." + assetName + ".part")).doesNotExist();
    }
}
