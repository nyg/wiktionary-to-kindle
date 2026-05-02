package edu.self.w2k.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.self.w2k.model.WiktionaryEntry;

class JsonlDictionaryParserTest {

    private static Path writeGzipJsonl(Path dir, String filename, List<String> lines) throws IOException {
        Path gzip = dir.resolve(filename);
        try (GZIPOutputStream gz = new GZIPOutputStream(new FileOutputStream(gzip.toFile()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gz, StandardCharsets.UTF_8))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
        return gzip;
    }

    @Test
    void parse_filtersCorrectLanguage(@TempDir Path tempDir) throws IOException {
        List<String> jsonlLines = List.of(
                "{\"word\":\"gatos\",\"lang_code\":\"es\",\"senses\":[{\"glosses\":[\"cats\"],\"examples\":[]}]}",
                "{\"word\":\"\u03c3\u03ba\u03cd\u03bb\u03bf\u03c2\",\"lang_code\":\"el\",\"senses\":[{\"glosses\":[\"dog\"],\"examples\":[]}]}",
                "{\"word\":\"Hund\",\"lang_code\":\"de\",\"senses\":[{\"glosses\":[\"dog\"],\"examples\":[]}]}"
        );
        Path dumpFile = writeGzipJsonl(tempDir, "test.jsonl.gz", jsonlLines);

        List<WiktionaryEntry> result = new JsonlDictionaryParser().parse(dumpFile, "el").collect(Collectors.toList());

        assertEquals(1, result.size(), "only the Greek entry should be returned");
        assertEquals("\u03c3\u03ba\u03cd\u03bb\u03bf\u03c2", result.get(0).word());
        assertEquals("el", result.get(0).langCode());
    }

    @Test
    void parse_skipsNullWord(@TempDir Path tempDir) throws IOException {
        List<String> jsonlLines = List.of(
                "{\"lang_code\":\"el\",\"senses\":[{\"glosses\":[\"something\"],\"examples\":[]}]}",
                "{\"word\":\"\u03c4\u03c1\u03ad\u03c7\u03c9\",\"lang_code\":\"el\",\"senses\":[{\"glosses\":[\"to run\"],\"examples\":[]}]}"
        );
        Path dumpFile = writeGzipJsonl(tempDir, "test.jsonl.gz", jsonlLines);

        List<WiktionaryEntry> result = new JsonlDictionaryParser().parse(dumpFile, "el").collect(Collectors.toList());

        assertEquals(1, result.size(), "entry with null word should be skipped");
        assertEquals("\u03c4\u03c1\u03ad\u03c7\u03c9", result.get(0).word());
    }

    @Test
    void parse_skipsOtherLanguages(@TempDir Path tempDir) throws IOException {
        List<String> jsonlLines = List.of(
                "{\"word\":\"cat\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"feline\"],\"examples\":[]}]}",
                "{\"word\":\"chat\",\"lang_code\":\"fr\",\"senses\":[{\"glosses\":[\"cat\"],\"examples\":[]}]}",
                "{\"word\":\"gato\",\"lang_code\":\"es\",\"senses\":[{\"glosses\":[\"cat\"],\"examples\":[]}]}"
        );
        Path dumpFile = writeGzipJsonl(tempDir, "test.jsonl.gz", jsonlLines);

        List<WiktionaryEntry> result = new JsonlDictionaryParser().parse(dumpFile, "fr").collect(Collectors.toList());

        assertEquals(1, result.size(), "only the French entry should be returned");
        assertEquals("chat", result.get(0).word());
    }
}
