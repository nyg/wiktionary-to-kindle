package edu.self.w2k;

import edu.self.w2k.util.OpfUtil;
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

/**
 * End-to-end integration tests for the generate pipeline:
 *
 *   gzip JSONL dump  →  WiktionaryUtil.generateDictionaryToFile
 *                    →  OpfUtil.generateOpf
 *                    →  HTML + OPF files
 *
 * The download step (DumpUtil) requires a live HTTP connection and is not
 * covered here. All other steps run with synthetic in-memory test data.
 */
class FullPipelineTest {

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

    private static String readFile(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void fullPipeline_jsonlDumpToOpf(@TempDir Path tempDir) throws IOException {
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"hello\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"a greeting\"],\"examples\":[{\"text\":\"Hello, world!\"}]}]}",
                "{\"word\":\"world\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"the earth\"],\"examples\":[]}]}"
        ));
        Path lexicon = tempDir.resolve("lexicon.txt");

        WiktionaryUtil.generateDictionaryToFile(dump, "en", lexicon);
        OpfUtil.generateOpf(lexicon, "en", "en", "English–English Dictionary", tempDir);

        assertTrue(Files.exists(tempDir.resolve("dictionary-en-en-0.html")), "HTML file must be created");
        assertTrue(Files.exists(tempDir.resolve("dictionary-en-en.opf")), "OPF file must be created");
    }

    @Test
    void fullPipeline_langFilteringApplied(@TempDir Path tempDir) throws IOException {
        // Dump has English, German and Spanish entries.
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"hello\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"a greeting\"],\"examples\":[]}]}",
                "{\"word\":\"Hund\",\"lang_code\":\"de\",\"senses\":[{\"glosses\":[\"dog\"],\"examples\":[]}]}",
                "{\"word\":\"gato\",\"lang_code\":\"es\",\"senses\":[{\"glosses\":[\"cat\"],\"examples\":[]}]}"
        ));
        Path lexicon = tempDir.resolve("lexicon.txt");

        WiktionaryUtil.generateDictionaryToFile(dump, "en", lexicon);
        OpfUtil.generateOpf(lexicon, "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        assertTrue(html.contains("hello"), "English entry must appear in output");
        assertFalse(html.contains("Hund"), "German entry must be excluded");
        assertFalse(html.contains("gato"), "Spanish entry must be excluded");
    }

    @Test
    void fullPipeline_definitionContentPreserved(@TempDir Path tempDir) throws IOException {
        // Glosses and example text should survive the full pipeline unchanged.
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"run\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"move at speed\"],\"examples\":[{\"text\":\"She runs every morning.\"}]}]}"
        ));
        Path lexicon = tempDir.resolve("lexicon.txt");

        WiktionaryUtil.generateDictionaryToFile(dump, "en", lexicon);
        OpfUtil.generateOpf(lexicon, "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        assertTrue(html.contains("move at speed"), "gloss must appear in HTML");
        assertTrue(html.contains("She runs every morning."), "example must appear in HTML");
    }

    @Test
    void fullPipeline_formOfEntriesExcluded(@TempDir Path tempDir) throws IOException {
        // form_of entries have no glosses and must be silently skipped.
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"ran\",\"lang_code\":\"en\",\"senses\":[{\"form_of\":[{\"word\":\"run\"}]}]}",
                "{\"word\":\"run\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"to move fast\"],\"examples\":[]}]}"
        ));
        Path lexicon = tempDir.resolve("lexicon.txt");

        WiktionaryUtil.generateDictionaryToFile(dump, "en", lexicon);
        OpfUtil.generateOpf(lexicon, "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        assertTrue(html.contains("value=\"run\""), "base form must appear");
        assertFalse(html.contains("value=\"ran\""), "form_of entry must be excluded");
    }

    @Test
    void fullPipeline_xmlSpecialCharsEscaped(@TempDir Path tempDir) throws IOException {
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"ampersand\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"bread & butter\"],\"examples\":[]}]}"
        ));
        Path lexicon = tempDir.resolve("lexicon.txt");

        WiktionaryUtil.generateDictionaryToFile(dump, "en", lexicon);
        OpfUtil.generateOpf(lexicon, "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        assertTrue(html.contains("bread &amp; butter"), "& must be XML-escaped in HTML");
        assertFalse(html.contains("bread & butter"), "unescaped & must not appear in HTML");
    }

    @Test
    void fullPipeline_multipleEntriesProduceValidOpf(@TempDir Path tempDir) throws IOException {
        List<String> entries = List.of(
                "{\"word\":\"alpha\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"first letter\"],\"examples\":[]}]}",
                "{\"word\":\"beta\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"second letter\"],\"examples\":[]}]}",
                "{\"word\":\"gamma\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"third letter\"],\"examples\":[]}]}"
        );
        Path dump = writeGzipJsonl(tempDir, entries);
        Path lexicon = tempDir.resolve("lexicon.txt");

        WiktionaryUtil.generateDictionaryToFile(dump, "en", lexicon);
        OpfUtil.generateOpf(lexicon, "en", "fr", "English–French Dictionary", tempDir);

        String opf = readFile(tempDir.resolve("dictionary-en-fr.opf"));
        String html = readFile(tempDir.resolve("dictionary-en-fr-0.html"));

        assertTrue(opf.contains("<DictionaryInLanguage>en</DictionaryInLanguage>"), "DictionaryInLanguage must be 'en'");
        assertTrue(opf.contains("<DictionaryOutLanguage>fr</DictionaryOutLanguage>"), "DictionaryOutLanguage must be 'fr'");
        assertTrue(opf.contains("<dc:title>English\u2013French Dictionary</dc:title>"), "OPF title must match");
        assertTrue(html.contains("value=\"alpha\""));
        assertTrue(html.contains("value=\"beta\""));
        assertTrue(html.contains("value=\"gamma\""));
    }

    @Test
    void fullPipeline_entriesAreSortedInOutput(@TempDir Path tempDir) throws IOException {
        // Dump is in reverse alphabetical order; output must be sorted.
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"zebra\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"striped animal\"],\"examples\":[]}]}",
                "{\"word\":\"apple\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"fruit\"],\"examples\":[]}]}",
                "{\"word\":\"mango\",\"lang_code\":\"en\",\"senses\":[{\"glosses\":[\"tropical fruit\"],\"examples\":[]}]}"
        ));
        Path lexicon = tempDir.resolve("lexicon.txt");

        WiktionaryUtil.generateDictionaryToFile(dump, "en", lexicon);
        OpfUtil.generateOpf(lexicon, "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        int applePos = html.indexOf("value=\"apple\"");
        int mangoPos = html.indexOf("value=\"mango\"");
        int zebraPos = html.indexOf("value=\"zebra\"");

        assertTrue(applePos < mangoPos, "apple must precede mango in output");
        assertTrue(mangoPos < zebraPos, "mango must precede zebra in output");
    }

    @Test
    void fullPipeline_greekToEnglish(@TempDir Path tempDir) throws IOException {
        Path dump = writeGzipJsonl(tempDir, List.of(
                "{\"word\":\"σκύλος\",\"lang_code\":\"el\",\"senses\":[{\"glosses\":[\"dog\"],\"examples\":[{\"text\":\"Ο σκύλος τρέχει.\"}]}]}",
                "{\"word\":\"γάτα\",\"lang_code\":\"el\",\"senses\":[{\"glosses\":[\"cat\"],\"examples\":[]}]}"
        ));
        Path lexicon = tempDir.resolve("lexicon.txt");

        WiktionaryUtil.generateDictionaryToFile(dump, "el", lexicon);

        String title = OpfUtil.autoTitle("el", "en");
        OpfUtil.generateOpf(lexicon, "el", "en", title, tempDir);

        String opf = readFile(tempDir.resolve("dictionary-el-en.opf"));
        String html = readFile(tempDir.resolve("dictionary-el-en-0.html"));

        assertTrue(opf.contains("<dc:language>el</dc:language>"));
        assertTrue(html.contains("<strong>σκύλος</strong>"), "Greek term must appear as display word");
        assertTrue(html.contains("dog"), "English gloss must be present");
        assertTrue(html.contains("Ο σκύλος τρέχει."), "Greek example must pass through");
    }
}
