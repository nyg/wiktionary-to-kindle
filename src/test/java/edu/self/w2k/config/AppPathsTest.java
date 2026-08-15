package edu.self.w2k.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(strings = {"  ", "relative/config"})
    void should_ignore_an_unusable_xdg_config_home(String value) {
        // Given blank means unset, and the XDG spec calls a relative value invalid rather than
        // resolving it against the working directory
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", value, "HOME", "/h");

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

    @Test
    void should_use_xdg_cache_home_when_set_on_unix() {
        // Given
        Map<String, String> env = Map.of("XDG_CACHE_HOME", "/c", "HOME", "/h");

        // When
        Path result = AppPaths.cacheDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/c/wiktionary-to-kindle"));
    }

    @Test
    void should_fallback_to_home_cache_when_xdg_is_unset_on_unix() {
        // Given
        Map<String, String> env = Map.of("HOME", "/h");

        // When
        Path result = AppPaths.cacheDir(env, "Mac OS X");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/.cache/wiktionary-to-kindle"));
    }

    @Test
    void should_put_cache_beside_config_on_windows() {
        // Given
        Map<String, String> env = Map.of("LOCALAPPDATA", "C:\\local");

        // When
        Path result = AppPaths.cacheDir(env, "Windows 11");

        // Then
        assertThat(result.toString())
                .startsWith("C:\\local")
                .contains("wiktionary-to-kindle")
                .contains("Cache");
    }

    @Test
    void should_use_xdg_state_home_when_set_on_unix() {
        // Given
        Map<String, String> env = Map.of("XDG_STATE_HOME", "/s", "HOME", "/h");

        // When
        Path result = AppPaths.stateDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/s/wiktionary-to-kindle"));
    }

    @Test
    void should_fallback_to_local_state_when_xdg_is_unset_on_unix() {
        // Given
        Map<String, String> env = Map.of("HOME", "/h");

        // When
        Path result = AppPaths.stateDir(env, "Mac OS X");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/.local/state/wiktionary-to-kindle"));
    }

    @Test
    void should_put_state_beside_config_on_windows() {
        // Given
        Map<String, String> env = Map.of("LOCALAPPDATA", "C:\\local");

        // When
        Path result = AppPaths.stateDir(env, "Windows 11");

        // Then
        assertThat(result.toString())
                .startsWith("C:\\local")
                .contains("wiktionary-to-kindle")
                .contains("State");
    }

    @Test
    void should_prefer_xdg_documents_dir_from_the_environment() {
        // Given
        Map<String, String> env = Map.of("XDG_DOCUMENTS_DIR", "/h/Documenten", "HOME", "/h");

        // When
        Path result = AppPaths.defaultDataDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/Documenten/wiktionary-to-kindle"));
    }

    @Test
    void should_expand_home_in_xdg_documents_dir(@TempDir Path config) throws IOException {
        // Given xdg-user-dirs writes the value as "$HOME/…"
        Files.writeString(config.resolve("user-dirs.dirs"), """
                XDG_DESKTOP_DIR="$HOME/Bureau"
                XDG_DOCUMENTS_DIR="$HOME/Documents perso"
                """);
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", config.toString(), "HOME", "/h");

        // When
        Path result = AppPaths.defaultDataDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/Documents perso/wiktionary-to-kindle"));
    }

    @Test
    void should_fallback_to_documents_when_user_dirs_has_no_documents_entry(@TempDir Path config)
            throws IOException {
        // Given
        Files.writeString(config.resolve("user-dirs.dirs"), "XDG_DESKTOP_DIR=\"$HOME/Bureau\"\n");
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", config.toString(), "HOME", "/h");

        // When
        Path result = AppPaths.defaultDataDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/Documents/wiktionary-to-kindle"));
    }

    @Test
    void should_ignore_a_relative_xdg_cache_home() {
        // Given
        Map<String, String> env = Map.of("XDG_CACHE_HOME", "relative/cache", "HOME", "/h");

        // When
        Path result = AppPaths.cacheDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/.cache/wiktionary-to-kindle"));
    }

    @Test
    void should_ignore_a_relative_xdg_state_home() {
        // Given
        Map<String, String> env = Map.of("XDG_STATE_HOME", "relative/state", "HOME", "/h");

        // When
        Path result = AppPaths.stateDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/.local/state/wiktionary-to-kindle"));
    }

    @Test
    void should_ignore_a_relative_xdg_documents_dir() {
        // Given
        Map<String, String> env = Map.of("XDG_DOCUMENTS_DIR", "Documenten", "HOME", "/h");

        // When
        Path result = AppPaths.defaultDataDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/Documents/wiktionary-to-kindle"));
    }

    @Test
    void should_ignore_a_relative_documents_entry_in_user_dirs(@TempDir Path config) throws IOException {
        // Given a hand-edited user-dirs.dirs whose value is neither absolute nor $HOME-prefixed
        Files.writeString(config.resolve("user-dirs.dirs"), "XDG_DOCUMENTS_DIR=\"Documenten\"\n");
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", config.toString(), "HOME", "/h");

        // When
        Path result = AppPaths.defaultDataDir(env, "Linux");

        // Then
        assertThat(result).isEqualTo(Path.of("/h/Documents/wiktionary-to-kindle"));
    }

    @Test
    void should_ignore_a_relative_home_on_unix() {
        // Given
        Map<String, String> env = Map.of("HOME", "relative/home");

        // When
        Path result = AppPaths.configDir(env, "Linux");

        // Then — the tmpdir fallback, exactly as if HOME had been unset
        assertThat(result.toString()).startsWith(System.getProperty("java.io.tmpdir"));
    }

    @Test
    void should_ignore_user_dirs_on_windows() {
        // Given a Windows environment that happens to carry XDG variables, e.g. under Git Bash
        Map<String, String> env = Map.of("XDG_DOCUMENTS_DIR", "/h/Documenten",
                                         "USERPROFILE", "C:\\Users\\me");

        // When
        Path result = AppPaths.defaultDataDir(env, "Windows 11");

        // Then
        assertThat(result.toString()).startsWith("C:\\Users\\me").contains("Documents");
    }
}
