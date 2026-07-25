package edu.self.w2k.kindling;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class KindlingReleaseTest {

    @Test
    void should_load_bundled_release_when_resource_is_on_classpath() {
        // When
        KindlingRelease result = KindlingRelease.load();

        // Then
        assertThat(result.version()).matches("v\\d+\\.\\d+\\.\\d+");
        for (KindlingPlatform platform : KindlingPlatform.values()) {
            assertThat(result.digest(platform)).matches("[0-9a-f]{64}");
        }
    }

    @Test
    void should_parse_version_and_digests_when_all_keys_present() throws Exception {
        // Given
        String props = """
                version=v9.9.9
                sha256.kindling-cli-linux=%s
                sha256.kindling-cli-mac-apple-silicon=%s
                sha256.kindling-cli-mac-intel=%s
                sha256.kindling-cli-windows.exe=%s
                """.formatted("1".repeat(64), "2".repeat(64), "3".repeat(64), "4".repeat(64));

        // When
        KindlingRelease result = KindlingRelease.parse(
                new ByteArrayInputStream(props.getBytes(StandardCharsets.ISO_8859_1)));

        // Then
        assertThat(result.version()).isEqualTo("v9.9.9");
        assertThat(result.digest(KindlingPlatform.LINUX_X64)).isEqualTo("1".repeat(64));
        assertThat(result.digest(KindlingPlatform.MAC_APPLE_SILICON)).isEqualTo("2".repeat(64));
        assertThat(result.digest(KindlingPlatform.MAC_INTEL)).isEqualTo("3".repeat(64));
        assertThat(result.digest(KindlingPlatform.WINDOWS_X64)).isEqualTo("4".repeat(64));
    }

    @Test
    void should_throw_when_version_key_is_missing() {
        // Given
        String props = """
                sha256.kindling-cli-linux=%s
                sha256.kindling-cli-mac-apple-silicon=%s
                sha256.kindling-cli-mac-intel=%s
                sha256.kindling-cli-windows.exe=%s
                """.formatted("1".repeat(64), "2".repeat(64), "3".repeat(64), "4".repeat(64));

        // When / Then
        assertThatThrownBy(() -> KindlingRelease.parse(
                new ByteArrayInputStream(props.getBytes(StandardCharsets.ISO_8859_1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version");
    }

    @Test
    void should_throw_when_digest_is_missing_for_platform() {
        // Given
        String props = """
                version=v9.9.9
                sha256.kindling-cli-linux=%s
                sha256.kindling-cli-mac-apple-silicon=%s
                sha256.kindling-cli-mac-intel=%s
                """.formatted("1".repeat(64), "2".repeat(64), "3".repeat(64));

        // When / Then
        assertThatThrownBy(() -> KindlingRelease.parse(
                new ByteArrayInputStream(props.getBytes(StandardCharsets.ISO_8859_1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sha256.kindling-cli-windows.exe");
    }

    @Test
    void should_throw_when_digest_is_not_64_hex_chars() {
        // Given
        String props = """
                version=v9.9.9
                sha256.kindling-cli-linux=sha256:%s
                sha256.kindling-cli-mac-apple-silicon=%s
                sha256.kindling-cli-mac-intel=%s
                sha256.kindling-cli-windows.exe=%s
                """.formatted("1".repeat(64), "2".repeat(64), "3".repeat(64), "4".repeat(64));

        // When / Then
        assertThatThrownBy(() -> KindlingRelease.parse(
                new ByteArrayInputStream(props.getBytes(StandardCharsets.ISO_8859_1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sha256.kindling-cli-linux");
    }
}
