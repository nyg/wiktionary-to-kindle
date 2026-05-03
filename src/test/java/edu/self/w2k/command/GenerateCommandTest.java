package edu.self.w2k.command;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import edu.self.w2k.model.LexiconEntry;
import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.parse.DictionaryParser;
import edu.self.w2k.render.DefinitionRenderer;
import edu.self.w2k.write.DictionaryWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateCommandTest {

    @Mock
    private DictionaryParser parser;

    @Mock
    private DefinitionRenderer renderer;

    @Mock
    private DictionaryWriter writer;

    @TempDir
    Path tmp;

    private GenerateCommand unit;

    @BeforeEach
    void setUp() {
        unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "el", "en", "Test Title");
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_group_entries_and_write_dictionary_when_run() throws Exception {
        // Given
        WiktionaryEntry entry1 = new WiktionaryEntry("Apple", "el", List.of());
        WiktionaryEntry entry2 = new WiktionaryEntry("apple", "el", List.of());
        WiktionaryEntry entry3 = new WiktionaryEntry("banana", "el", List.of());
        when(parser.parse(any(Path.class), eq("el"))).thenReturn(Stream.of(entry1, entry2, entry3));
        when(renderer.render(any())).thenReturn(Optional.of("<def>"));

        // When
        unit.run();

        // Then
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor =
                ArgumentCaptor.forClass((Class<TreeMap<String, List<LexiconEntry>>>) (Class<?>) TreeMap.class);
        verify(writer).write(captor.capture(), eq("el"), eq("en"), eq("Test Title"), eq(tmp));
        TreeMap<String, List<LexiconEntry>> captured = captor.getValue();
        assertThat(captured).containsKeys("apple", "banana");
        assertThat(captured.get("apple")).hasSize(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_skip_entries_when_renderer_returns_empty() throws Exception {
        // Given
        WiktionaryEntry entry1 = new WiktionaryEntry("apple", "el", List.of());
        WiktionaryEntry entry2 = new WiktionaryEntry("banana", "el", List.of());
        when(parser.parse(any(Path.class), eq("el"))).thenReturn(Stream.of(entry1, entry2));
        when(renderer.render(any()))
                .thenReturn(Optional.of("<def>"))
                .thenReturn(Optional.empty());

        // When
        unit.run();

        // Then
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor =
                ArgumentCaptor.forClass((Class<TreeMap<String, List<LexiconEntry>>>) (Class<?>) TreeMap.class);
        verify(writer).write(captor.capture(), eq("el"), eq("en"), eq("Test Title"), eq(tmp));
        TreeMap<String, List<LexiconEntry>> captured = captor.getValue();
        assertThat(captured).containsOnlyKeys("apple");
    }

    @Test
    void should_lowercase_and_strip_when_normalising_key() {
        // Given / When
        String result = GenerateCommand.normaliseKey("  Hello  ");

        // Then
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void should_replace_quotes_and_escape_angle_brackets_when_normalising_key() {
        // Given / When
        String result = GenerateCommand.normaliseKey("\"<a>\"");

        // Then
        assertThat(result).isEqualTo("'\\<a\\>'");
    }
}
