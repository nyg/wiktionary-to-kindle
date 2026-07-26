package edu.self.w2k;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CLITest {

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
}
