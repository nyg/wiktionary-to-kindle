package edu.self.w2k.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PreferencesTest {

    @TempDir
    Path tmp;

    @Test
    void should_round_trip_all_values_when_stored_and_loaded() throws Exception {
        // Given
        Preferences original = new Preferences(Path.of("/data/dumps"),
                                               Path.of("/data/dictionaries"),
                                               Optional.of(Path.of("/usr/local/bin/kindling-cli")),
                                               Optional.of("v0.28.0"),
                                               AppTheme.CUPERTINO,
                                               true);
        Path file = tmp.resolve("preferences.properties");

        // When
        original.store(file);
        Preferences loaded = Preferences.load(file);

        // Then
        assertThat(loaded).isEqualTo(original);
    }

    @Test
    void should_round_trip_empty_optionals_when_stored_and_loaded() throws Exception {
        // Given
        Preferences original = new Preferences(Path.of("/data/dumps"),
                                               Path.of("/data/dictionaries"),
                                               Optional.empty(),
                                               Optional.empty(),
                                               AppTheme.JAVAFX, false);
        Path file = tmp.resolve("preferences.properties");

        // When
        original.store(file);
        Preferences loaded = Preferences.load(file);

        // Then
        assertThat(loaded.kindlingCliPath()).isEmpty();
        assertThat(loaded.kindlingVersion()).isEmpty();
        assertThat(loaded.dumpsDir()).isEqualTo(Path.of("/data/dumps"));
    }

    @Test
    void should_keep_intermediate_files_by_default() {
        // Then — the working files stay unless the user asks for them to go
        assertThat(Preferences.defaults().deleteIntermediateFiles()).isFalse();
    }

    @Test
    void should_read_the_intermediate_files_flag_when_set() throws Exception {
        // Given
        Path file = tmp.resolve("preferences.properties");
        Files.writeString(file, "deleteIntermediateFiles=true\n", StandardCharsets.UTF_8);

        // When
        Preferences loaded = Preferences.load(file);

        // Then
        assertThat(loaded.deleteIntermediateFiles()).isTrue();
    }

    @Test
    void should_return_defaults_when_file_does_not_exist() {
        // When
        Preferences loaded = Preferences.load(tmp.resolve("absent.properties"));

        // Then
        assertThat(loaded).isEqualTo(Preferences.defaults());
    }

    @Test
    void should_fall_back_per_key_when_file_is_partial() throws Exception {
        // Given a hand-edited file that sets only one of the four keys
        Path file = tmp.resolve("preferences.properties");
        Files.writeString(file, "dumpsDir=/custom/dumps\n", StandardCharsets.UTF_8);

        // When
        Preferences loaded = Preferences.load(file);

        // Then
        assertThat(loaded.dumpsDir()).isEqualTo(Path.of("/custom/dumps"));
        assertThat(loaded.dictionariesDir()).isEqualTo(Preferences.defaults().dictionariesDir());
        assertThat(loaded.kindlingCliPath()).isEmpty();
    }

    @Test
    void should_treat_blank_values_as_absent_when_loading() throws Exception {
        // Given
        Path file = tmp.resolve("preferences.properties");
        Files.writeString(file, """
                dumpsDir=
                dictionariesDir=   \s
                kindlingVersion=
                """, StandardCharsets.UTF_8);

        // When
        Preferences loaded = Preferences.load(file);

        // Then
        assertThat(loaded).isEqualTo(Preferences.defaults());
    }

    @Test
    void should_strip_surrounding_whitespace_when_loading_values() throws Exception {
        // Given
        Path file = tmp.resolve("preferences.properties");
        Files.writeString(file, "kindlingVersion=  v1.2.3  \n", StandardCharsets.UTF_8);

        // When
        Preferences loaded = Preferences.load(file);

        // Then
        assertThat(loaded.kindlingVersion()).contains("v1.2.3");
    }

    @Test
    void should_keep_the_platform_default_theme_when_the_file_names_an_unknown_one() throws Exception {
        // Given
        Path file = tmp.resolve("preferences.properties");
        Files.writeString(file, "theme=solarized\n", StandardCharsets.UTF_8);

        // When
        Preferences loaded = Preferences.load(file);

        // Then
        assertThat(loaded.theme()).isEqualTo(AppTheme.defaultForThisPlatform());
    }

    @Test
    void should_read_a_theme_named_in_any_case() throws Exception {
        // Given
        Path file = tmp.resolve("preferences.properties");
        Files.writeString(file, "theme=Cupertino\n", StandardCharsets.UTF_8);

        // When
        Preferences loaded = Preferences.load(file);

        // Then
        assertThat(loaded.theme()).isEqualTo(AppTheme.CUPERTINO);
    }

    @Test
    void should_create_parent_directory_when_storing() throws Exception {
        // Given
        Path file = tmp.resolve("nested").resolve("deeper").resolve("preferences.properties");

        // When
        Preferences.defaults().store(file);

        // Then
        assertThat(file).exists();
    }

    @Test
    void should_make_relative_directories_absolute_when_loading() throws Exception {
        // Given a hand-edited file, which would otherwise mean the filesystem root to the bundled app
        Path file = tmp.resolve("preferences.properties");
        Files.writeString(file, """
                dumpsDir=dumps
                dictionariesDir=./out/../dictionaries
                kindlingCliPath=bin/kindling-cli
                """, StandardCharsets.UTF_8);

        // When
        Preferences loaded = Preferences.load(file);

        // Then — compared by file name because the directories need not exist, and AssertJ's
        // Path.endsWith resolves the real path on disk
        assertThat(loaded.dumpsDir()).isAbsolute().hasFileName("dumps");
        assertThat(loaded.dictionariesDir()).isAbsolute().hasFileName("dictionaries");
        assertThat(loaded.kindlingCliPath()).hasValueSatisfying(path -> assertThat(path).isAbsolute());
    }

    @Test
    void should_make_relative_directories_absolute_when_constructed_directly() {
        // Given the preferences dialog hands over whatever text the user typed
        Preferences unit = new Preferences(Path.of("dumps"),
                                           Path.of("dictionaries"),
                                           Optional.empty(),
                                           Optional.empty(),
                                           AppTheme.JAVAFX, false);

        // Then
        assertThat(unit.dumpsDir()).isAbsolute();
        assertThat(unit.dictionariesDir()).isAbsolute();
    }

    @Test
    void should_default_dumps_and_dictionaries_under_a_common_parent() {
        // When
        Preferences defaults = Preferences.defaults();

        // Then
        assertThat(defaults.dumpsDir()).hasParentRaw(defaults.dictionariesDir().getParent());
        assertThat(defaults.dumpsDir().getFileName()).hasToString("dumps");
        assertThat(defaults.dictionariesDir().getFileName()).hasToString("dictionaries");
    }
}
