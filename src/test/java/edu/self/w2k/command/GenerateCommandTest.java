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
        when(renderer.render(any())).thenReturn(Optional.of(new RenderedEntry("<def>", List.of(), List.of())));

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
                .thenReturn(Optional.of(new RenderedEntry("<def>", List.of(), List.of())))
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
                .thenReturn(Optional.of(new RenderedEntry("<def>", List.of("σύντροφοι", "συντρόφου"), List.of())));

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
                        List.of("σύντροφοι", "συντρόφισσα"), List.of())))
                .thenReturn(Optional.of(new RenderedEntry("<def>συντρόφισσα body</def>", List.of(), List.of())));

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
                        List.of("ingénieurs", "ingénieure"), List.of())))
                .thenReturn(Optional.of(new RenderedEntry("<def/>", List.of(), List.of())));

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
    void should_fold_form_of_entry_into_lemma_iforms_when_lemma_exists() throws Exception {
        // Given — "suis" is a form-of-only entry pointing at lemma "suus"
        GenerateCommand unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "fr", "la", "Test Title");
        WiktionaryEntry lemma = new WiktionaryEntry("suus", "la", "adj", List.of(), List.of());
        WiktionaryEntry formOf = new WiktionaryEntry("suis", "la", "adj", List.of(), List.of());
        when(parser.parse(any(Path.class), eq("fr"))).thenReturn(Stream.of(lemma, formOf));
        when(renderer.render(any()))
                .thenReturn(Optional.of(new RenderedEntry("<def>suus</def>", List.of("sua", "suum"), List.of())))
                .thenReturn(Optional.of(new RenderedEntry("<def>Datif pluriel de suus.</def>", List.of(), List.of("suus"))));

        // When
        unit.run();

        // Then — standalone "suis" headword is gone; its word became an iform on the lemma
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor = ArgumentCaptor.captor();
        verify(writer).write(captor.capture(), eq("fr"), eq("la"), eq("Test Title"), eq(tmp));
        TreeMap<String, List<LexiconEntry>> captured = captor.getValue();
        assertThat(captured).containsOnlyKeys("suus");
        assertThat(captured.get("suus").getFirst().inflectionForms()).containsExactly("sua", "suum", "suis");
    }

    @Test
    void should_keep_form_of_entry_when_lemma_is_absent() throws Exception {
        // Given — the referenced lemma has no entry in the dictionary
        GenerateCommand unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "fr", "la", "Test Title");
        WiktionaryEntry formOf = new WiktionaryEntry("petit", "la", "verb", List.of(), List.of());
        when(parser.parse(any(Path.class), eq("fr"))).thenReturn(Stream.of(formOf));
        when(renderer.render(any()))
                .thenReturn(Optional.of(new RenderedEntry("<def>Forme de petere.</def>", List.of(), List.of("petere"))));

        // When
        unit.run();

        // Then — a dead-end entry is better than nothing
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor = ArgumentCaptor.captor();
        verify(writer).write(captor.capture(), eq("fr"), eq("la"), eq("Test Title"), eq(tmp));
        assertThat(captor.getValue()).containsOnlyKeys("petit");
    }

    @Test
    void should_keep_form_of_entry_when_lemma_group_is_itself_form_of_only() throws Exception {
        // Given — a chain: "b" is a form of "a", but "a" is itself only a form of a missing lemma
        GenerateCommand unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "fr", "la", "Test Title");
        WiktionaryEntry intermediate = new WiktionaryEntry("a", "la", "verb", List.of(), List.of());
        WiktionaryEntry formOf = new WiktionaryEntry("b", "la", "verb", List.of(), List.of());
        when(parser.parse(any(Path.class), eq("fr"))).thenReturn(Stream.of(intermediate, formOf));
        when(renderer.render(any()))
                .thenReturn(Optional.of(new RenderedEntry("<def>Forme de missing.</def>", List.of(), List.of("missing"))))
                .thenReturn(Optional.of(new RenderedEntry("<def>Forme de a.</def>", List.of(), List.of("a"))));

        // When
        unit.run();

        // Then — neither entry is folded; both keep their dead-end definitions
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor = ArgumentCaptor.captor();
        verify(writer).write(captor.capture(), eq("fr"), eq("la"), eq("Test Title"), eq(tmp));
        assertThat(captor.getValue()).containsOnlyKeys("a", "b");
    }

    @Test
    void should_keep_real_entry_and_fold_form_of_entry_when_word_group_is_mixed() throws Exception {
        // Given — "page" is both a real Latin noun and (as a separate entry) a form of "pagus";
        // the lemma "pagus" also exists
        GenerateCommand unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "fr", "la", "Test Title");
        WiktionaryEntry realNoun = new WiktionaryEntry("page", "la", "noun", List.of(), List.of());
        WiktionaryEntry formOf = new WiktionaryEntry("page", "la", "noun", List.of(), List.of());
        WiktionaryEntry lemma = new WiktionaryEntry("pagus", "la", "noun", List.of(), List.of());
        when(parser.parse(any(Path.class), eq("fr"))).thenReturn(Stream.of(realNoun, formOf, lemma));
        when(renderer.render(any()))
                .thenReturn(Optional.of(new RenderedEntry("<def>Une page.</def>", List.of(), List.of())))
                .thenReturn(Optional.of(new RenderedEntry("<def>Vocatif de pagus.</def>", List.of(), List.of("pagus"))))
                .thenReturn(Optional.of(new RenderedEntry("<def>pagus</def>", List.of(), List.of())));

        // When
        unit.run();

        // Then — the form-of entry is folded away, the real noun stays; the collision filter then
        // strips the freshly added "page" iform because "page" remains a headword
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor = ArgumentCaptor.captor();
        verify(writer).write(captor.capture(), eq("fr"), eq("la"), eq("Test Title"), eq(tmp));
        TreeMap<String, List<LexiconEntry>> captured = captor.getValue();
        assertThat(captured).containsOnlyKeys("page", "pagus");
        assertThat(captured.get("page")).hasSize(1);
        assertThat(captured.get("page").getFirst().definition()).isEqualTo("<def>Une page.</def>");
        assertThat(captured.get("pagus").getFirst().inflectionForms()).isEmpty();
    }

    @Test
    void should_keep_form_of_entry_when_it_only_references_itself() throws Exception {
        // Given — a form-of entry whose lemma normalises to its own key (e.g. case difference)
        GenerateCommand unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "fr", "la", "Test Title");
        WiktionaryEntry selfRef = new WiktionaryEntry("Roma", "la", "noun", List.of(), List.of());
        when(parser.parse(any(Path.class), eq("fr"))).thenReturn(Stream.of(selfRef));
        when(renderer.render(any()))
                .thenReturn(Optional.of(new RenderedEntry("<def>Forme de roma.</def>", List.of(), List.of("roma"))));

        // When
        unit.run();

        // Then
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor = ArgumentCaptor.captor();
        verify(writer).write(captor.capture(), eq("fr"), eq("la"), eq("Test Title"), eq(tmp));
        assertThat(captor.getValue()).containsOnlyKeys("roma");
    }

    @Test
    void should_keep_form_of_entry_when_its_word_is_not_a_usable_lookup_key() throws Exception {
        // Given — a multi-word form-of entry cannot enter the iform index
        GenerateCommand unit = new GenerateCommand(parser, renderer, writer, tmp.resolve("dump.jsonl.gz"), tmp, "fr", "la", "Test Title");
        WiktionaryEntry lemma = new WiktionaryEntry("res", "la", "noun", List.of(), List.of());
        WiktionaryEntry multiWord = new WiktionaryEntry("res publica", "la", "noun", List.of(), List.of());
        when(parser.parse(any(Path.class), eq("fr"))).thenReturn(Stream.of(lemma, multiWord));
        when(renderer.render(any()))
                .thenReturn(Optional.of(new RenderedEntry("<def>res</def>", List.of(), List.of())))
                .thenReturn(Optional.of(new RenderedEntry("<def>Forme de res.</def>", List.of(), List.of("res"))));

        // When
        unit.run();

        // Then — kept as its own entry; no iform added to the lemma
        ArgumentCaptor<TreeMap<String, List<LexiconEntry>>> captor = ArgumentCaptor.captor();
        verify(writer).write(captor.capture(), eq("fr"), eq("la"), eq("Test Title"), eq(tmp));
        TreeMap<String, List<LexiconEntry>> captured = captor.getValue();
        assertThat(captured).containsOnlyKeys("res", "res publica");
        assertThat(captured.get("res").getFirst().inflectionForms()).isEmpty();
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
