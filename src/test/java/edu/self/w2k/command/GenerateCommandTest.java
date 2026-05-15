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
import edu.self.w2k.render.RenderedEntry;
import edu.self.w2k.write.DictionaryWriter;
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

    @Test
    void should_group_entries_and_write_dictionary_when_run() throws Exception {
        // Given
        GenerateCommand unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "el", "en", "Test Title");
        WiktionaryEntry entry1 = new WiktionaryEntry("Apple", "el", "noun", List.of(), List.of());
        WiktionaryEntry entry2 = new WiktionaryEntry("apple", "el", "noun", List.of(), List.of());
        WiktionaryEntry entry3 = new WiktionaryEntry("banana", "el", "noun", List.of(), List.of());
        when(parser.parse(any(Path.class), eq("el"))).thenReturn(Stream.of(entry1, entry2, entry3));
        when(renderer.render(any())).thenReturn(Optional.of(new RenderedEntry("<def>", List.of())));

        // When
        unit.run();

        // Then
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor = ArgumentCaptor.captor();
        verify(writer).write(captor.capture(), eq("el"), eq("en"), eq("Test Title"), eq(tmp));
        TreeMap<String, List<LexiconEntry>> captured = captor.getValue();
        assertThat(captured).containsKeys("apple", "banana");
        assertThat(captured.get("apple")).hasSize(2);
    }

    @Test
    void should_skip_entries_when_renderer_returns_empty() throws Exception {
        // Given
        GenerateCommand unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "el", "en", "Test Title");
        WiktionaryEntry entry1 = new WiktionaryEntry("apple", "el", "noun", List.of(), List.of());
        WiktionaryEntry entry2 = new WiktionaryEntry("banana", "el", "noun", List.of(), List.of());
        when(parser.parse(any(Path.class), eq("el"))).thenReturn(Stream.of(entry1, entry2));
        when(renderer.render(any()))
                .thenReturn(Optional.of(new RenderedEntry("<def>", List.of())))
                .thenReturn(Optional.empty());

        // When
        unit.run();

        // Then
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor = ArgumentCaptor.captor();
        verify(writer).write(captor.capture(), eq("el"), eq("en"), eq("Test Title"), eq(tmp));
        assertThat(captor.getValue()).containsOnlyKeys("apple");
    }

    @Test
    void should_thread_inflection_forms_into_lexicon_entry_when_renderer_returns_them() throws Exception {
        // Given
        GenerateCommand unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "el", "fr", "Test Title");
        WiktionaryEntry entry = new WiktionaryEntry("σύντροφος", "el", "noun", List.of(), List.of());
        when(parser.parse(any(Path.class), eq("el"))).thenReturn(Stream.of(entry));
        when(renderer.render(any()))
                .thenReturn(Optional.of(new RenderedEntry("<def>", List.of("σύντροφοι", "συντρόφου"))));

        // When
        unit.run();

        // Then
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor = ArgumentCaptor.captor();
        verify(writer).write(captor.capture(), eq("el"), eq("fr"), eq("Test Title"), eq(tmp));
        LexiconEntry lex = captor.getValue().get("σύντροφος").getFirst();
        assertThat(lex.inflectionForms()).containsExactly("σύντροφοι", "συντρόφου");
    }

    @Test
    void should_drop_inflection_forms_that_collide_with_existing_headwords() throws Exception {
        // Given — σύντροφος lists συντρόφισσα as a (gender-equivalent) form, but συντρόφισσα
        // also exists as its own standalone headword in the dump. The visible Forms: table in
        // the rendered HTML stays untouched; only the iform lookup index is filtered.
        GenerateCommand unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "el", "fr", "Test Title");
        WiktionaryEntry lemma = new WiktionaryEntry("σύντροφος", "el", "noun", List.of(), List.of());
        WiktionaryEntry equiv = new WiktionaryEntry("συντρόφισσα", "el", "noun", List.of(), List.of());
        when(parser.parse(any(Path.class), eq("el"))).thenReturn(Stream.of(lemma, equiv));
        when(renderer.render(any()))
                .thenReturn(Optional.of(new RenderedEntry("<def>σύντροφος body mentions συντρόφισσα</def>",
                        List.of("σύντροφοι", "συντρόφισσα"))))
                .thenReturn(Optional.of(new RenderedEntry("<def>συντρόφισσα body</def>", List.of())));

        // When
        unit.run();

        // Then
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor = ArgumentCaptor.captor();
        verify(writer).write(captor.capture(), eq("el"), eq("fr"), eq("Test Title"), eq(tmp));
        TreeMap<String, List<LexiconEntry>> captured = captor.getValue();

        LexiconEntry lemmaEntry = captured.get("σύντροφος").getFirst();
        assertThat(lemmaEntry.inflectionForms()).containsExactly("σύντροφοι");
        assertThat(lemmaEntry.definition()).contains("συντρόφισσα");

        LexiconEntry equivEntry = captured.get("συντρόφισσα").getFirst();
        assertThat(equivEntry.inflectionForms()).isEmpty();
    }

    @Test
    void should_match_collision_filter_case_insensitively_via_normalised_key() throws Exception {
        // Given — headword key normalisation lowercases; the filter should use the same rule.
        GenerateCommand unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "fr", "en", "Test Title");
        WiktionaryEntry lemma = new WiktionaryEntry("ingénieur", "fr", "noun", List.of(), List.of());
        WiktionaryEntry feminine = new WiktionaryEntry("Ingénieure", "fr", "noun", List.of(), List.of());
        when(parser.parse(any(Path.class), eq("fr"))).thenReturn(Stream.of(lemma, feminine));
        when(renderer.render(any()))
                .thenReturn(Optional.of(new RenderedEntry("<def/>",
                        List.of("ingénieurs", "ingénieure"))))
                .thenReturn(Optional.of(new RenderedEntry("<def/>", List.of())));

        // When
        unit.run();

        // Then — "ingénieure" collides with normalised key of "Ingénieure" and is dropped;
        // the real plural "ingénieurs" is kept.
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor = ArgumentCaptor.captor();
        verify(writer).write(captor.capture(), eq("fr"), eq("en"), eq("Test Title"), eq(tmp));
        LexiconEntry lex = captor.getValue().get("ingénieur").getFirst();
        assertThat(lex.inflectionForms()).containsExactly("ingénieurs");
    }

    @Test
    void should_lowercase_and_strip_when_normalising_key() {
        // When
        String result = GenerateCommand.normaliseKey("  Hello  ");

        // Then
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void should_replace_quotes_and_escape_angle_brackets_when_normalising_key() {
        // When
        String result = GenerateCommand.normaliseKey("\"<a>\"");

        // Then
        assertThat(result).isEqualTo("'\\<a\\>'");
    }
}
