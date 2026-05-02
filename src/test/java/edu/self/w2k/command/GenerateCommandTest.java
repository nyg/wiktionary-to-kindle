package edu.self.w2k.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.self.w2k.opf.KindleOpfGenerator;
import edu.self.w2k.parse.JsonlDictionaryParser;
import edu.self.w2k.render.HtmlDefinitionRenderer;

class GenerateCommandTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Path writeGzipJsonl(Path dir, List<String> lines) throws IOException {
        Path gzip = dir.resolve("dump.jsonl.gz");
        try (GZIPOutputStream gz = new GZIPOutputStream(new FileOutputStream(gzip.toFile()));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(gz, StandardCharsets.UTF_8))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
        return gzip;
    }

    private static GenerateCommand buildCommand(Path dumpFile, Path outputDir, String lang) {
        return new GenerateCommand(
                new JsonlDictionaryParser(),
                new HtmlDefinitionRenderer(),
                new KindleOpfGenerator(),
                dumpFile,
                outputDir,
                lang,
                lang,
                KindleOpfGenerator.autoTitle(lang, lang)
        );
    }

    // ── pipeline tests ────────────────────────────────────────────────────────

    @Test
    void run_jsonlDumpToOpf(@TempDir Path tempDir) throws Exception {
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"hello\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"a greeting\"],\"examples\":[{\"text\":\"Hello, world!\"}]}]}",
                "{\"word\":\"world\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"the earth\"],\"examples\":[]}]}"
        ));

        buildCommand(dump, tempDir, "en").run();

        assertTrue(Files.exists(tempDir.resolve("dictionary-en-en-0.html")), "HTML file must be created");
        assertTrue(Files.exists(tempDir.resolve("dictionary-en-en.opf")), "OPF file must be created");
    }

    @Test
    void run_langFilteringApplied(@TempDir Path tempDir) throws Exception {
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"hello\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"a greeting\"],\"examples\":[]}]}",
                "{\"word\":\"Hund\",\"lang_code\":\"de\",\"senses\":[{\"glosses\":[\"dog\"],\"examples\":[]}]}",
                "{\"word\":\"gato\",\"lang_code\":\"es\",\"senses\":[{\"glosses\":[\"cat\"],\"examples\":[]}]}"
        ));

        buildCommand(dump, tempDir, "en").run();

        String html = Files.readString(tempDir.resolve("dictionary-en-en-0.html"), StandardCharsets.UTF_8);
        assertTrue(html.contains("hello"), "English entry must appear in output");
        assertFalse(html.contains("Hund"), "German entry must be excluded");
        assertFalse(html.contains("gato"), "Spanish entry must be excluded");
    }

    @Test
    void run_definitionContentPreserved(@TempDir Path tempDir) throws Exception {
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"run\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"move at speed\"],\"examples\":[{\"text\":\"She runs every morning.\"}]}]}"
        ));

        buildCommand(dump, tempDir, "en").run();

        String html = Files.readString(tempDir.resolve("dictionary-en-en-0.html"), StandardCharsets.UTF_8);
        assertTrue(html.contains("move at speed"), "gloss must appear in HTML");
        assertTrue(html.contains("She runs every morning."), "example must appear in HTML");
    }

    @Test
    void run_formOfEntriesExcluded(@TempDir Path tempDir) throws Exception {
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"ran\",\"lang_code\":\"en\",\"senses\":[{\"form_of\":[{\"word\":\"run\"}]}]}",
                "{\"word\":\"run\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"to move fast\"],\"examples\":[]}]}"
        ));

        buildCommand(dump, tempDir, "en").run();

        String html = Files.readString(tempDir.resolve("dictionary-en-en-0.html"), StandardCharsets.UTF_8);
        assertTrue(html.contains("value=\"run\""), "base form must appear");
        assertFalse(html.contains("value=\"ran\""), "form_of entry must be excluded");
    }

    @Test
    void run_xmlSpecialCharsEscaped(@TempDir Path tempDir) throws Exception {
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"ampersand\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"bread & butter\"],\"examples\":[]}]}"
        ));

        buildCommand(dump, tempDir, "en").run();

        String html = Files.readString(tempDir.resolve("dictionary-en-en-0.html"), StandardCharsets.UTF_8);
        assertTrue(html.contains("bread &amp; butter"), "& must be XML-escaped in HTML");
        assertFalse(html.contains("bread & butter"), "unescaped & must not appear in HTML");
    }

    @Test
    void run_entriesAreSortedInOutput(@TempDir Path tempDir) throws Exception {
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"zebra\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"striped animal\"],\"examples\":[]}]}",
                "{\"word\":\"apple\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"fruit\"],\"examples\":[]}]}",
                "{\"word\":\"mango\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"tropical fruit\"],\"examples\":[]}]}"
        ));

        buildCommand(dump, tempDir, "en").run();

        String html = Files.readString(tempDir.resolve("dictionary-en-en-0.html"), StandardCharsets.UTF_8);
        int applePos = html.indexOf("value=\"apple\"");
        int mangoPos = html.indexOf("value=\"mango\"");
        int zebraPos = html.indexOf("value=\"zebra\"");

        assertTrue(applePos < mangoPos, "apple must precede mango in output");
        assertTrue(mangoPos < zebraPos, "mango must precede zebra in output");
    }

    // ── normaliseKey unit tests ───────────────────────────────────────────────

    @Test
    void normaliseKey_lowercasesWord() {
        assertEquals("hello", GenerateCommand.normaliseKey("Hello"));
    }

    @Test
    void normaliseKey_stripsWhitespace() {
        assertEquals("hello", GenerateCommand.normaliseKey("  hello  "));
    }

    @Test
    void normaliseKey_replacesDoubleQuote() {
        assertEquals("it's", GenerateCommand.normaliseKey("it\"s"));
    }

    @Test
    void normaliseKey_escapesAngleBrackets() {
        assertEquals("\\<b\\>", GenerateCommand.normaliseKey("<b>"));
    }

    @Test
    void normaliseKey_emptyStringRemainsEmpty() {
        assertEquals("", GenerateCommand.normaliseKey("   "));
    }
}
