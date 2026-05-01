package edu.self.w2k.lexicon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class TsvLexiconReaderTest {

    private final TsvLexiconReader reader = new TsvLexiconReader();

    // ── normaliseKey unit tests ───────────────────────────────────────────────

    @Test
    void normaliseKey_lowercases() {
        assertEquals("hello", TsvLexiconReader.normaliseKey("Hello"));
        assertEquals("hello", TsvLexiconReader.normaliseKey("HELLO"));
    }

    @Test
    void normaliseKey_stripsWhitespace() {
        assertEquals("hello", TsvLexiconReader.normaliseKey("  hello  "));
    }

    @Test
    void normaliseKey_replacesDoubleQuotes() {
        assertEquals("it's", TsvLexiconReader.normaliseKey("it\"s"));
    }

    @Test
    void normaliseKey_escapesAngleBrackets() {
        assertEquals("a \\< b \\> c", TsvLexiconReader.normaliseKey("A < B > C"));
    }

    // ── read() unit tests ─────────────────────────────────────────────────────

    @Test
    void read_singleEntry(@TempDir Path tempDir) throws IOException {
        Path file = writeLexicon(tempDir, "hello\t<ol><li>a greeting</li></ol>");

        TreeMap<String, List<LexiconEntry>> result = reader.read(file);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("hello"));
        List<LexiconEntry> entries = result.get("hello");
        assertEquals(1, entries.size());
        assertEquals("hello", entries.get(0).word());
        assertEquals("<ol><li>a greeting</li></ol>", entries.get(0).definition());
    }

    @Test
    void read_groupsEntriesByNormalisedKey(@TempDir Path tempDir) throws IOException {
        // "Hello" and "hello" normalise to the same key → grouped together
        Path file = writeLexicon(tempDir,
                "Hello\t<ol><li>first</li></ol>",
                "hello\t<ol><li>second</li></ol>");

        TreeMap<String, List<LexiconEntry>> result = reader.read(file);

        assertEquals(1, result.size());
        List<LexiconEntry> entries = result.get("hello");
        assertNotNull(entries);
        assertEquals(2, entries.size());
    }

    @Test
    void read_multipleDistinctKeys(@TempDir Path tempDir) throws IOException {
        Path file = writeLexicon(tempDir,
                "apple\t<ol><li>fruit</li></ol>",
                "banana\t<ol><li>fruit</li></ol>",
                "cherry\t<ol><li>fruit</li></ol>");

        TreeMap<String, List<LexiconEntry>> result = reader.read(file);

        assertEquals(3, result.size());
        assertTrue(result.containsKey("apple"));
        assertTrue(result.containsKey("banana"));
        assertTrue(result.containsKey("cherry"));
    }

    @Test
    void read_keysAreSortedAlphabetically(@TempDir Path tempDir) throws IOException {
        Path file = writeLexicon(tempDir,
                "zebra\t<ol><li>animal</li></ol>",
                "apple\t<ol><li>fruit</li></ol>",
                "mango\t<ol><li>fruit</li></ol>");

        TreeMap<String, List<LexiconEntry>> result = reader.read(file);

        List<String> keys = List.copyOf(result.keySet());
        assertEquals(List.of("apple", "mango", "zebra"), keys);
    }

    @Test
    void read_skipsBlankAndCommentLines(@TempDir Path tempDir) throws IOException {
        Path file = writeLexicon(tempDir,
                "# this is a comment",
                "word\t<ol><li>def</li></ol>",
                "");

        TreeMap<String, List<LexiconEntry>> result = reader.read(file);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("word"));
    }

    @Test
    void read_skipsLinesWithoutTab(@TempDir Path tempDir) throws IOException {
        Path file = writeLexicon(tempDir,
                "no-tab-line",
                "valid\t<ol><li>def</li></ol>");

        TreeMap<String, List<LexiconEntry>> result = reader.read(file);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("valid"));
    }

    @Test
    void read_normaliseKeyApplied(@TempDir Path tempDir) throws IOException {
        Path file = writeLexicon(tempDir, "UPPER\t<ol><li>def</li></ol>");

        TreeMap<String, List<LexiconEntry>> result = reader.read(file);

        assertFalse(result.containsKey("UPPER"), "key should be lowercased");
        assertTrue(result.containsKey("upper"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Path writeLexicon(Path dir, String... lines) throws IOException {
        Path file = dir.resolve("lexicon.txt");
        Files.write(file, List.of(lines), StandardCharsets.UTF_8);
        return file;
    }
}
