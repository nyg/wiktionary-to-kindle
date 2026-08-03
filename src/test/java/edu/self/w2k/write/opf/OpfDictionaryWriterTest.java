package edu.self.w2k.write.opf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

import edu.self.w2k.model.LexiconEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OpfDictionaryWriterTest {

    private final OpfDictionaryWriter unit = new OpfDictionaryWriter();

    @TempDir
    Path tmp;

    @Test
    void should_write_opf_html_and_return_opf_path_when_called() throws Exception {
        // Given
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        defs.put("apple", List.of(new LexiconEntry("apple", "<ol><li>fruit</li></ol>", List.of(), List.of())));
        defs.put("banana", List.of(new LexiconEntry("banana", "<ol><li>tropical fruit</li></ol>", List.of(), List.of())));
        defs.put("cherry", List.of(new LexiconEntry("cherry", "<ol><li>small fruit</li></ol>", List.of(), List.of())));

        // When
        Path result = unit.write(defs, "en", "fr", "English-French Dictionary", tmp);

        // Then
        assertThat(result)
                .isEqualTo(tmp.resolve("w2k-dictionary-en-fr.opf"))
                .exists();
        String opfContent = Files.readString(result);
        assertThat(opfContent)
                .contains("<DictionaryInLanguage>en</DictionaryInLanguage>")
                .contains("<DictionaryOutLanguage>fr</DictionaryOutLanguage>");
        assertThat(tmp.resolve("w2k-dictionary-en-fr-0.html")).exists();
    }

    @Test
    void should_name_the_ncx_and_cover_after_the_language_pair() throws Exception {
        // Given two dictionaries generated into one directory, as the app does
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        defs.put("apple", List.of(new LexiconEntry("apple", "<ol><li>fruit</li></ol>", List.of(), List.of())));

        // When
        unit.write(defs, "en", "fr", "W2K English–French Dictionary", tmp);
        unit.write(defs, "en", "de", "W2K English–German Dictionary", tmp);

        // Then neither run has overwritten the other's side-artefacts
        assertThat(tmp.resolve("w2k-dictionary-en-fr-toc.ncx")).exists();
        assertThat(tmp.resolve("w2k-dictionary-en-fr-cover.jpg")).exists();
        assertThat(tmp.resolve("w2k-dictionary-en-de-toc.ncx")).exists();
        assertThat(tmp.resolve("w2k-dictionary-en-de-cover.jpg")).exists();

        // And the OPF points at its own copies
        assertThat(Files.readString(tmp.resolve("w2k-dictionary-en-fr.opf")))
                .contains("href=\"w2k-dictionary-en-fr-toc.ncx\"")
                .contains("href=\"w2k-dictionary-en-fr-cover.jpg\"");
    }

    @Test
    void should_chunk_html_files_when_entries_exceed_chapter_limit() throws Exception {
        // Given
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        for (int i = 0; i <= HtmlChapterRenderer.ENTRIES_PER_CHAPTER; i++) {
            String key = String.format("word%05d", i);
            defs.put(key, List.of(new LexiconEntry(key, "<ol><li>def</li></ol>", List.of(), List.of())));
        }

        // When
        unit.write(defs, "en", "fr", "English-French Dictionary", tmp);

        // Then
        assertThat(tmp.resolve("w2k-dictionary-en-fr-0.html")).exists();
        assertThat(tmp.resolve("w2k-dictionary-en-fr-1.html")).exists();
    }
}
