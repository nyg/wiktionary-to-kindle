package edu.self.w2k;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import edu.self.w2k.config.AppTheme;
import edu.self.w2k.config.Preferences;
import edu.self.w2k.download.DumpDownloader;
import edu.self.w2k.pipeline.DictionaryPipeline;
import edu.self.w2k.progress.ProgressListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CLITest {

    @Mock
    private DumpDownloader downloader;

    @Mock
    private DictionaryPipeline.DownloaderFactory downloaderFactory;

    @Test
    void should_report_the_build_version_when_version_is_requested() {
        // Given
        CLI.BuildVersion unit = new CLI.BuildVersion();

        // When
        String[] result = unit.getVersion();

        // Then — the version comes from the filtered build resource, not a literal in @Command
        assertThat(result).hasSize(1);
        assertThat(result[0]).startsWith("wiktionary-to-kindle ")
                .doesNotContain("unknown")
                .matches("wiktionary-to-kindle \\d+\\.\\d+\\.\\d+(-SNAPSHOT)?");
    }

    @Test
    void should_wire_the_version_provider_into_the_command_spec() {
        // When
        CommandLine commandLine = new CommandLine(new CLI());

        // Then — guards against the annotation reverting to a hardcoded version string
        assertThat(commandLine.getCommandSpec().versionProvider()).isInstanceOf(CLI.BuildVersion.class);
    }

    @Test
    void should_return_bundled_version_when_option_is_kindling_version() {
        // Given
        CLI.Generate.KindlingVersionDefault unit = new CLI.Generate.KindlingVersionDefault();
        CommandLine.Model.OptionSpec option = CommandLine.Model.OptionSpec.builder("--kindling-version").build();

        // When
        String result = unit.defaultValue(option);

        // Then
        assertThat(result).matches("v\\d+\\.\\d+\\.\\d+");
    }

    @Test
    void should_return_null_when_option_is_not_kindling_version() {
        // Given
        CLI.Generate.KindlingVersionDefault unit = new CLI.Generate.KindlingVersionDefault();
        CommandLine.Model.OptionSpec option = CommandLine.Model.OptionSpec.builder("--other").build();

        // When
        String result = unit.defaultValue(option);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void should_apply_bundled_default_when_kindling_version_not_given() {
        // Given
        CommandLine cmd = new CommandLine(new CLI());

        // When
        CommandLine.ParseResult result = cmd.parseArgs("generate", "el", "en");

        // Then
        String version = result.subcommand().commandSpec().findOption("--kindling-version").getValue();
        assertThat(version).matches("v\\d+\\.\\d+\\.\\d+");
    }

    @Test
    void should_download_into_the_preferences_dumps_directory_when_no_override_is_given() {
        // Given
        Preferences preferences = new Preferences(Path.of("/prefs/dumps"),
                                                  Path.of("/prefs/dictionaries"),
                                                  Optional.empty(),
                                                  Optional.empty(),
                                                  AppTheme.JAVAFX);
        CommandLine.ParseResult parsed = new CommandLine(new CLI()).parseArgs("download", "fr");
        CLI.Download unit = (CLI.Download) parsed.subcommand().commandSpec().userObject();

        // When
        Path result = unit.dumpsDir(preferences);

        // Then — no longer the CWD-relative dumps/ the CLI used before it shared the GUI's locations
        assertThat(result).isEqualTo(Path.of("/prefs/dumps"));
    }

    @Test
    void should_download_into_the_given_directory_when_dumps_dir_is_overridden() {
        // Given
        Preferences preferences = new Preferences(Path.of("/prefs/dumps"),
                                                  Path.of("/prefs/dictionaries"),
                                                  Optional.empty(),
                                                  Optional.empty(),
                                                  AppTheme.JAVAFX);
        CommandLine.ParseResult parsed =
                new CommandLine(new CLI()).parseArgs("download", "fr", "--dumps-dir", "/override/dumps");
        CLI.Download unit = (CLI.Download) parsed.subcommand().commandSpec().userObject();

        // When
        Path result = unit.dumpsDir(preferences);

        // Then
        assertThat(result).isEqualTo(Path.of("/override/dumps"));
    }

    @Test
    void should_generate_from_and_into_the_preferences_directories_when_no_override_is_given() {
        // Given
        Preferences preferences = new Preferences(Path.of("/prefs/dumps"),
                                                  Path.of("/prefs/dictionaries"),
                                                  Optional.empty(),
                                                  Optional.empty(),
                                                  AppTheme.JAVAFX);
        CommandLine.ParseResult parsed = new CommandLine(new CLI()).parseArgs("generate", "el", "en");
        CLI.Generate unit = (CLI.Generate) parsed.subcommand().commandSpec().userObject();

        // When / Then
        assertThat(unit.dumpsDir(preferences)).isEqualTo(Path.of("/prefs/dumps"));
        assertThat(unit.dictionariesDir(preferences)).isEqualTo(Path.of("/prefs/dictionaries"));
    }

    @Test
    void should_generate_from_and_into_the_given_directories_when_both_are_overridden() {
        // Given
        Preferences preferences = new Preferences(Path.of("/prefs/dumps"),
                                                  Path.of("/prefs/dictionaries"),
                                                  Optional.empty(),
                                                  Optional.empty(),
                                                  AppTheme.JAVAFX);
        CommandLine.ParseResult parsed = new CommandLine(new CLI())
                .parseArgs("generate", "el", "en",
                           "--dumps-dir", "/override/dumps",
                           "--dictionaries-dir", "/override/dictionaries");
        CLI.Generate unit = (CLI.Generate) parsed.subcommand().commandSpec().userObject();

        // When / Then
        assertThat(unit.dumpsDir(preferences)).isEqualTo(Path.of("/override/dumps"));
        assertThat(unit.dictionariesDir(preferences)).isEqualTo(Path.of("/override/dictionaries"));
    }

    @Test
    void should_pick_the_most_recent_dump_when_the_language_has_several(@TempDir Path dumpsDir) throws IOException {
        // Given
        Files.createFile(dumpsDir.resolve("raw-wiktextract-data-el-2026-01-09.jsonl.gz"));
        Files.createFile(dumpsDir.resolve("raw-wiktextract-data-el-2026-05-31.jsonl.gz"));
        Files.createFile(dumpsDir.resolve("raw-wiktextract-data-fr-2026-08-12.jsonl.gz"));

        // When
        Optional<Path> result = CLI.findLatestDump("el", dumpsDir);

        // Then
        assertThat(result).contains(dumpsDir.resolve("raw-wiktextract-data-el-2026-05-31.jsonl.gz"));
    }

    @Test
    void should_find_nothing_when_the_dumps_directory_holds_no_dump_for_the_language(@TempDir Path dumpsDir)
            throws IOException {
        // Given
        Files.createFile(dumpsDir.resolve("raw-wiktextract-data-fr-2026-08-12.jsonl.gz"));

        // When
        Optional<Path> result = CLI.findLatestDump("el", dumpsDir);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_exit_with_one_when_generate_finds_no_dump(@TempDir Path dumpsDir) {
        // Given
        CommandLine unit = new CommandLine(new CLI());

        // When
        int result = unit.execute("generate", "el", "en", "--dumps-dir", dumpsDir.toString());

        // Then — the missing dump is reported before any kindling-cli resolution is attempted
        assertThat(result).isEqualTo(1);
    }

    @Test
    void should_download_into_the_resolved_dumps_directory_when_download_is_run(@TempDir Path dumpsDir)
            throws IOException {
        // Given
        CommandLine.ParseResult parsed =
                new CommandLine(new CLI()).parseArgs("download", "fr", "--dumps-dir", dumpsDir.toString());
        CLI.Download unit = (CLI.Download) parsed.subcommand().commandSpec().userObject();
        unit.downloaderFactory = downloaderFactory;
        when(downloaderFactory.create("fr", dumpsDir, ProgressListener.NOOP)).thenReturn(downloader);

        // When
        int result = unit.call();

        // Then
        assertThat(result).isZero();
        verify(downloader).download();
    }

    @Test
    void should_exit_with_one_when_the_download_fails() throws IOException {
        // Given
        CLI.Download unit = new CLI.Download();
        when(downloader.download()).thenThrow(new IOException("HTTP 503"));

        // When
        int result = unit.run(downloader);

        // Then — the README documents this exit code for scripts that chain download into generate
        assertThat(result).isEqualTo(1);
    }
}
