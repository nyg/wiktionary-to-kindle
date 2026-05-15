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
        defs.put("apple", List.of(new LexiconEntry("apple", "<ol><li>fruit</li></ol>", List.of())));
        defs.put("banana", List.of(new LexiconEntry("banana", "<ol><li>tropical fruit</li></ol>", List.of())));
        defs.put("cherry", List.of(new LexiconEntry("cherry", "<ol><li>small fruit</li></ol>", List.of())));

        // When
        Path result = unit.write(defs, "en", "fr", "English-French Dictionary", tmp);

        // Then
        assertThat(result)
                .isEqualTo(tmp.resolve("dictionary-en-fr.opf"))
                .exists();
        String opfContent = Files.readString(result);
        assertThat(opfContent)
                .contains("<DictionaryInLanguage>en</DictionaryInLanguage>")
                .contains("<DictionaryOutLanguage>fr</DictionaryOutLanguage>");
        assertThat(tmp.resolve("dictionary-en-fr-0.html")).exists();
    }

    @Test
    void should_chunk_html_files_when_entries_exceed_chapter_limit() throws Exception {
        // Given
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        for (int i = 0; i <= HtmlChapterRenderer.ENTRIES_PER_CHAPTER; i++) {
            String key = String.format("word%05d", i);
            defs.put(key, List.of(new LexiconEntry(key, "<ol><li>def</li></ol>", List.of())));
        }

        // When
        unit.write(defs, "en", "fr", "English-French Dictionary", tmp);

        // Then
        assertThat(tmp.resolve("dictionary-en-fr-0.html")).exists();
        assertThat(tmp.resolve("dictionary-en-fr-1.html")).exists();
    }
}
