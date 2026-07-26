package edu.self.w2k.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AppPathsTest {

    @Test
    void should_use_xdg_config_home_when_set_on_unix() {
        // Given
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", "/c");

        // When
        Path result = AppPaths.configDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/c/wiktionary-to-kindle"));
    }

    @Test
    void should_fallback_to_home_config_when_xdg_is_unset_on_unix() {
        // Given
        Map<String, String> env = Map.of("HOME", "/h");

        // When
        Path result = AppPaths.configDir(env, "Mac OS X");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/.config/wiktionary-to-kindle"));
    }

    @Test
    void should_prefer_xdg_over_home_when_both_are_set_on_unix() {
        // Given
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", "/c", "HOME", "/h");

        // When
        Path result = AppPaths.configDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/c/wiktionary-to-kindle"));
    }

    @Test
    void should_ignore_blank_xdg_when_resolving_config_dir() {
        // Given
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", "  ", "HOME", "/h");

        // When
        Path result = AppPaths.configDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/.config/wiktionary-to-kindle"));
    }

    @Test
    void should_use_localappdata_when_set_on_windows() {
        // Given
        Map<String, String> env = Map.of("LOCALAPPDATA", "C:\\local");

        // When
        Path result = AppPaths.configDir(env, "Windows 11");

        // Then
        assertThat(result.toString())
                .startsWith("C:\\local")
                .contains("wiktionary-to-kindle")
                .contains("Config");
    }

    @Test
    void should_fallback_to_userprofile_when_localappdata_is_unset_on_windows() {
        // Given
        Map<String, String> env = Map.of("USERPROFILE", "C:\\Users\\me");

        // When
        Path result = AppPaths.configDir(env, "Windows 11");

        // Then
        assertThat(result.toString())
                .startsWith("C:\\Users\\me")
                .contains("AppData")
                .contains("Local")
                .contains("wiktionary-to-kindle");
    }

    @Test
    void should_fallback_to_tmpdir_when_no_home_is_known() {
        // When
        Path result = AppPaths.configDir(Map.of(), "Linux");

        // Then — compared as text because the path need not exist, and AssertJ's Path.startsWith
        // resolves the real path on disk
        assertThat(result.toString()).startsWith(System.getProperty("java.io.tmpdir"));
    }

    @Test
    void should_put_data_under_documents_when_resolving_default_data_dir_on_unix() {
        // Given
        Map<String, String> env = Map.of("HOME", "/h");

        // When
        Path result = AppPaths.defaultDataDir(env, "Mac OS X");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/Documents/wiktionary-to-kindle"));
    }

    @Test
    void should_put_data_under_documents_when_resolving_default_data_dir_on_windows() {
        // Given
        Map<String, String> env = Map.of("USERPROFILE", "C:\\Users\\me");

        // When
        Path result = AppPaths.defaultDataDir(env, "Windows 11");

        // Then
        assertThat(result.toString())
                .startsWith("C:\\Users\\me")
                .contains("Documents")
                .endsWith("wiktionary-to-kindle");
    }

    @Test
    void should_not_use_xdg_config_home_when_resolving_data_dir() {
        // Given data must be user-visible, XDG_CONFIG_HOME must not divert it
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", "/c", "HOME", "/h");

        // When
        Path result = AppPaths.defaultDataDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/Documents/wiktionary-to-kindle"));
    }
}
