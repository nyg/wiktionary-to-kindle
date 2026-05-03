package edu.self.w2k.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import edu.self.w2k.parse.JsonlDictionaryParser;
import edu.self.w2k.render.HtmlDefinitionRenderer;
import edu.self.w2k.write.DictionaryTitles;
import edu.self.w2k.write.opf.OpfDictionaryWriter;

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
                new OpfDictionaryWriter(),
                dumpFile,
                outputDir,
                lang,
                lang,
                DictionaryTitles.autoTitle(lang, lang)
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

        assertTrue(Files.exists(tempDir.resolve("dictionary-en-en.opf")), "OPF must be created");
    }

    @Test
    void run_formOfEntriesExcluded(@TempDir Path tempDir) throws Exception {
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"ran\",\"lang_code\":\"en\",\"senses\":[{\"form_of\":[{\"word\":\"run\"}]}]}",
                "{\"word\":\"run\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"to move fast\"],\"examples\":[]}]}"
        ));

        buildCommand(dump, tempDir, "en").run();

        assertTrue(Files.exists(tempDir.resolve("dictionary-en-en.opf")), "OPF must be created");
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
