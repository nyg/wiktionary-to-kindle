package edu.self.w2k.kindling;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import edu.self.w2k.kindling.KindlingDictionaryConverter.ProcessRunner;
import edu.self.w2k.progress.ProgressListener.Stage;
import edu.self.w2k.write.opf.OpfDictionaryWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KindlingDictionaryConverterTest {

    @Mock
    private OpfDictionaryWriter opfWriter;

    @Mock
    private KindlingCliResolver resolver;

    @Mock
    private ProcessRunner runner;

    @TempDir
    Path outputDir;

    private final List<Stage> stages = new ArrayList<>();

    private KindlingDictionaryConverter unit;

    @BeforeEach
    void setUp() {
        // Wired explicitly rather than with @InjectMocks: the progress listener is an optional
        // collaborator with a NOOP default, which reflective injection would supply as null.
        unit = new KindlingDictionaryConverter(opfWriter, resolver, runner,
                                               (stage, _, _) -> stages.add(stage));
    }

    @Test
    void should_run_kindling_cli_and_return_mobi_path_when_write() throws Exception {
        // Given
        Path opfPath = outputDir.resolve("dictionary-en-fr.opf");
        Path binPath = outputDir.resolve("kindling-cli");
        when(opfWriter.write(any(), eq("en"), eq("fr"), eq("Title"), eq(outputDir))).thenReturn(opfPath);
        when(resolver.resolve()).thenReturn(binPath);
        when(runner.run(anyList())).thenReturn(0);

        // When
        Path result = unit.write(new TreeMap<>(), "en", "fr", "Title", outputDir);

        // Then
        assertThat(result).isEqualTo(outputDir.resolve("dictionary-en-fr.mobi"));

        ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.captor();
        verify(runner).run(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .contains("build")
                .contains("-o")
                .anyMatch(s -> s.endsWith("dictionary-en-fr.opf"))
                .anyMatch(s -> s.endsWith("dictionary-en-fr.mobi"));
    }

    @Test
    void should_throw_io_exception_when_runner_returns_non_zero_exit_code() throws Exception {
        // Given
        Path opfPath = outputDir.resolve("dictionary-en-fr.opf");
        Path binPath = outputDir.resolve("kindling-cli");
        when(opfWriter.write(any(), eq("en"), eq("fr"), eq("Title"), eq(outputDir))).thenReturn(opfPath);
        when(resolver.resolve()).thenReturn(binPath);
        when(runner.run(anyList())).thenReturn(1);

        // When / Then
        assertThatThrownBy(() -> unit.write(new TreeMap<>(), "en", "fr", "Title", outputDir))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("exit");
    }

    @Test
    void should_report_kindling_stage_when_invoking_the_binary() throws Exception {
        // Given
        when(opfWriter.write(any(), eq("en"), eq("fr"), eq("Title"), eq(outputDir)))
                .thenReturn(outputDir.resolve("dictionary-en-fr.opf"));
        when(resolver.resolve()).thenReturn(outputDir.resolve("kindling-cli"));
        when(runner.run(anyList())).thenReturn(0);

        // When
        unit.write(new TreeMap<>(), "en", "fr", "Title", outputDir);

        // Then
        assertThat(stages).containsExactly(Stage.KINDLING);
    }

    @Test
    void should_relay_process_output_to_the_log_when_using_default_runner() throws Exception {
        // Given a command whose stdout and stderr both produce output. inheritIO() would send this
        // nowhere in a windowed app, so the default runner must read the streams itself.
        List<String> command = List.of("sh", "-c", "echo to-stdout; echo to-stderr >&2; exit 0");

        // When
        int exitCode = KindlingDictionaryConverter.defaultRunner().run(command);

        // Then
        assertThat(exitCode).isZero();
    }

    @Test
    void should_expose_started_process_when_default_runner_is_given_a_callback() throws Exception {
        // Given
        List<Process> started = new ArrayList<>();

        // When
        int exitCode = KindlingDictionaryConverter.defaultRunner(started::add)
                .run(List.of("sh", "-c", "exit 3"));

        // Then
        assertThat(exitCode).isEqualTo(3);
        assertThat(started).hasSize(1);
    }
}
