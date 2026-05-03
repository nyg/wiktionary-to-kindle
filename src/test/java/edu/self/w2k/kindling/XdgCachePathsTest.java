package edu.self.w2k.kindling;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class XdgCachePathsTest {

    @Test
    void should_use_xdg_cache_home_when_set_on_unix() {
        // Given
        Map<String, String> env = Map.of("XDG_CACHE_HOME", "/c");

        // When
        Path result = XdgCachePaths.kindlingCacheDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/c/wiktionary-to-kindle/kindling"));
    }

    @Test
    void should_fallback_to_home_when_xdg_is_unset_on_unix() {
        // Given
        Map<String, String> env = Map.of("HOME", "/h");

        // When
        Path result = XdgCachePaths.kindlingCacheDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/.cache/wiktionary-to-kindle/kindling"));
    }

    @Test
    void should_use_localappdata_when_set_on_windows() {
        // Given
        Map<String, String> env = Map.of("LOCALAPPDATA", "C:\\local");

        // When
        Path result = XdgCachePaths.kindlingCacheDir(env, "Windows 10");

        // Then
        assertThat(result.toString())
                .startsWith("C:\\local")
                .contains("wiktionary-to-kindle")
                .contains("Cache")
                .contains("kindling");
    }
}
