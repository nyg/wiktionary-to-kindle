package edu.self.w2k;

import edu.self.w2k.util.WiktionaryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class WiktionaryUtilTest {

    /**
     * Writes the given JSONL lines as a gzip-compressed file.
     */
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
    void buildDefinition_basicGloss() {
        var senses = List.of(parseSense("{\"glosses\":[\"a dog\"],\"examples\":[]}"));
        String def = WiktionaryUtil.buildDefinition(senses);
        assertNotNull(def);
        assertTrue(def.contains("<ol>"), "should wrap in <ol>");
        assertTrue(def.contains("<li><span>a dog</span>"), "should contain gloss");
        assertFalse(def.contains("<ul>"), "no examples → no <ul>");
    }

    @Test
    void buildDefinition_xmlEscaping() {
        var senses = List.of(parseSense("{\"glosses\":[\"bread & butter <food>\"],\"examples\":[]}"));
        String def = WiktionaryUtil.buildDefinition(senses);
        assertNotNull(def);
        assertTrue(def.contains("bread &amp; butter &lt;food&gt;"), "XML characters should be escaped");
    }

    @Test
    void buildDefinition_withExample() {
        var senses = List.of(parseSense("{\"glosses\":[\"run\"],\"examples\":[{\"text\":\"He runs fast.\"}]}"));
        String def = WiktionaryUtil.buildDefinition(senses);
        assertNotNull(def);
        assertTrue(def.contains("<ul>"), "examples present → <ul>");
        assertTrue(def.contains("<li>He runs fast.</li>"));
    }

    @Test
    void buildDefinition_stripsNewlines() {
        var senses = List.of(parseSense("{\"glosses\":[\"line1\\nline2\"],\"examples\":[]}"));
        String def = WiktionaryUtil.buildDefinition(senses);
        assertNotNull(def);
        assertTrue(def.contains("line1; line2"), "newlines should be replaced with '; '");
    }

    @Test
    void buildDefinition_multipleGlossesInOneSense() {
        var senses = List.of(parseSense("{\"glosses\":[\"first meaning\",\"second meaning\"],\"examples\":[]}"));
        String def = WiktionaryUtil.buildDefinition(senses);
        assertNotNull(def);
        assertTrue(def.contains("first meaning"));
        assertTrue(def.contains("second meaning"));
    }

    @Test
    void buildDefinition_nullSensesSkipped() {
        // entry with no glosses at all → form_of-only entry
        var senses = List.of(parseSense("{\"glosses\":[],\"examples\":[]}"));
        assertNull(WiktionaryUtil.buildDefinition(senses), "form_of-only entries should return null");
    }

    @Test
    void buildDefinition_blankGlossSkipped() {
        var senses = List.of(parseSense("{\"glosses\":[\"  \"],\"examples\":[]}"));
        assertNull(WiktionaryUtil.buildDefinition(senses), "blank gloss should be skipped → null");
    }

    @Test
    void buildDefinition_blankExampleSkipped() {
        var senses = List.of(parseSense("{\"glosses\":[\"gloss\"],\"examples\":[{\"text\":\"  \"}]}"));
        String def = WiktionaryUtil.buildDefinition(senses);
        assertNotNull(def);
        assertFalse(def.contains("<ul>"), "blank example should be skipped → no <ul>");
    }

    @Test
    void buildDefinition_greekContent() {
        var senses = List.of(parseSense("{\"glosses\":[\"σκύλος (dog)\"],\"examples\":[{\"text\":\"Ο σκύλος τρέχει.\"}]}"));
        String def = WiktionaryUtil.buildDefinition(senses);
        assertNotNull(def);
        assertTrue(def.contains("σκύλος (dog)"), "Greek gloss should pass through unchanged");
        assertTrue(def.contains("Ο σκύλος τρέχει."), "Greek example should pass through unchanged");
    }

    @Test
    void generateDictionary_langFiltering(@TempDir Path tempDir) throws IOException {
        List<String> jsonlLines = List.of(
                "{\"word\":\"gatos\",\"lang_code\":\"es\",\"senses\":[{\"glosses\":[\"cats\"],\"examples\":[]}]}",
                "{\"word\":\"σκύλος\",\"lang_code\":\"el\",\"senses\":[{\"glosses\":[\"dog\"],\"examples\":[]}]}",
                "{\"word\":\"Hund\",\"lang_code\":\"de\",\"senses\":[{\"glosses\":[\"dog\"],\"examples\":[]}]}"
        );
        Path dumpFile = writeGzipJsonl(tempDir, "test.jsonl.gz", jsonlLines);
        Path outputFile = tempDir.resolve("lexicon.txt");

        WiktionaryUtil.generateDictionaryToFile(dumpFile, "el", outputFile);

        List<String> outputLines = Files.readAllLines(outputFile, StandardCharsets.UTF_8);
        assertEquals(1, outputLines.size(), "only the Greek entry should be written");
        assertTrue(outputLines.get(0).startsWith("σκύλος\t<ol>"));
    }

    @Test
    void generateDictionary_skipsFormOfEntries(@TempDir Path tempDir) throws IOException {
        List<String> jsonlLines = List.of(
                "{\"word\":\"έτρεξε\",\"lang_code\":\"el\",\"senses\":[{\"form_of\":[{\"word\":\"τρέχω\"}]}]}",
                "{\"word\":\"τρέχω\",\"lang_code\":\"el\",\"senses\":[{\"glosses\":[\"to run\"],\"examples\":[]}]}"
        );
        Path dumpFile = writeGzipJsonl(tempDir, "test.jsonl.gz", jsonlLines);
        Path outputFile = tempDir.resolve("lexicon.txt");

        WiktionaryUtil.generateDictionaryToFile(dumpFile, "el", outputFile);

        List<String> outputLines = Files.readAllLines(outputFile, StandardCharsets.UTF_8);
        assertEquals(1, outputLines.size(), "form_of entry should be skipped");
        assertTrue(outputLines.get(0).startsWith("τρέχω\t"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static edu.self.w2k.model.WiktionarySense parseSense(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readerFor(edu.self.w2k.model.WiktionarySense.class)
                    .readValue(json);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
