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
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos);
             Writer writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
            writer.write(jsonl);
        }
        Path dump = tmp.resolve("dump.jsonl.gz");
        Files.write(dump, bos.toByteArray());

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
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos);
             Writer writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
            writer.write(jsonl);
        }
        Path dump = tmp.resolve("dump.jsonl.gz");
        Files.write(dump, bos.toByteArray());

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
}
