package edu.self.w2k.kindling;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

import edu.self.w2k.kindling.KindlingDictionaryConverter.ProcessRunner;
import edu.self.w2k.write.opf.OpfDictionaryWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private KindlingDictionaryConverter unit;

    @TempDir
    Path outputDir;

    @Test
    @SuppressWarnings("unchecked")
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

        ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.forClass(List.class);
        verify(runner).run(commandCaptor.capture());
        List<String> command = commandCaptor.getValue();
        assertThat(command)
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
}
