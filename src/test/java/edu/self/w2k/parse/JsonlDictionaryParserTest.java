package edu.self.w2k.parse;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.model.WiktionaryForm;

@ExtendWith(MockitoExtension.class)
class JsonlDictionaryParserTest {

    private final JsonlDictionaryParser unit = new JsonlDictionaryParser();

    @TempDir
    Path tmp;

    @Test
    void should_return_only_entries_matching_lang_when_parsing() throws Exception {
        // Given
        String jsonl = """
                {"word":"γεια","lang_code":"el","senses":[]}
                {"word":"hello","lang_code":"en","senses":[]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "el")) {
            result = stream.toList();
        }

        // Then
        assertThat(result)
                .extracting(WiktionaryEntry::word)
                .containsExactly("γεια");
    }

    @Test
    void should_skip_entries_with_blank_word_when_parsing() throws Exception {
        // Given
        String jsonl = """
                {"word":"","lang_code":"el","senses":[]}
                {"word":"καλή","lang_code":"el","senses":[]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "el")) {
            result = stream.toList();
        }

        // Then
        assertThat(result)
                .extracting(WiktionaryEntry::word)
                .containsExactly("καλή");
    }

    @Test
    void should_deserialise_pos_and_forms_when_parsing() throws Exception {
        // Given
        String jsonl = """
                {"word":"σύντροφος","lang_code":"el","pos":"noun","senses":[],"forms":[\
                {"form":"σύντροφοι","tags":["plural","nominative"],"article":"οι"},\
                {"form":"συντρόφου","tags":["singular","genitive"],"article":"του"},\
                {"form":"συντρόφισσα","tags":["feminine"],"source":"form line template 'équiv-pour'"}]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "el")) {
            result = stream.toList();
        }

        // Then
        assertThat(result).hasSize(1);
        WiktionaryEntry entry = result.getFirst();
        assertThat(entry.pos()).isEqualTo("noun");
        assertThat(entry.forms())
                .extracting(WiktionaryForm::form)
                .containsExactly("σύντροφοι", "συντρόφου", "συντρόφισσα");
        assertThat(entry.forms().get(0).tags()).containsExactly("plural", "nominative");
        assertThat(entry.forms().get(0).article()).isEqualTo("οι");
        assertThat(entry.forms().get(2).source()).contains("équiv-pour");
    }

    @Test
    void should_default_forms_to_empty_when_field_missing() throws Exception {
        // Given
        String jsonl = """
                {"word":"foo","lang_code":"el","senses":[]}
                """;
        Path dump = writeGzippedJsonl(jsonl);

        // When
        List<WiktionaryEntry> result;
        try (Stream<WiktionaryEntry> stream = unit.parse(dump, "el")) {
            result = stream.toList();
        }

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().forms()).isEmpty();
        // Jackson's Nulls.AS_EMPTY converts a missing String field to "" rather than null.
        // The renderer treats empty pos the same as null (neither equals "verb").
        assertThat(result.getFirst().pos()).isEmpty();
    }

    private Path writeGzippedJsonl(String jsonl) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos);
             Writer writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
            writer.write(jsonl);
        }
        Path dump = tmp.resolve("dump.jsonl.gz");
        Files.write(dump, bos.toByteArray());
        return dump;
    }
}
