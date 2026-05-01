package edu.self.w2k.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class OpfUtilTest {

    // ── normaliseKey unit tests ───────────────────────────────────────────────

    @Test
    void normaliseKey_lowercases() {
        assertEquals("hello", OpfUtil.normaliseKey("Hello"));
        assertEquals("hello", OpfUtil.normaliseKey("HELLO"));
    }

    @Test
    void normaliseKey_stripsWhitespace() {
        assertEquals("hello", OpfUtil.normaliseKey("  hello  "));
    }

    @Test
    void normaliseKey_replacesDoubleQuotes() {
        assertEquals("it's", OpfUtil.normaliseKey("it\"s"));
    }

    @Test
    void normaliseKey_escapesAngleBrackets() {
        assertEquals("a \\< b \\> c", OpfUtil.normaliseKey("A < B > C"));
    }

    // ── autoTitle unit tests ──────────────────────────────────────────────────

    @Test
    void autoTitle_knownLanguageCodes() {
        String title = OpfUtil.autoTitle("fr", "en");
        assertTrue(title.contains("French"), "should contain 'French', got: " + title);
        assertTrue(title.contains("English"), "should contain 'English', got: " + title);
        assertTrue(title.endsWith("Dictionary"), "should end with 'Dictionary'");
    }

    @Test
    void autoTitle_unknownCodeFallsBackToUpperCase() {
        String title = OpfUtil.autoTitle("xx", "en");
        assertTrue(title.contains("XX"), "unknown code should fall back to upper-case, got: " + title);
    }

    // ── generateOpf integration tests ────────────────────────────────────────

    @Test
    void generateOpf_createsExpectedFiles(@TempDir Path tempDir) throws IOException {
        writeLexicon(tempDir, "hello\t<ol><li>a greeting</li></ol>");

        OpfUtil.generateOpf(tempDir.resolve("lexicon.txt"), "en", "fr", "Test Dictionary", tempDir);

        assertTrue(Files.exists(tempDir.resolve("dictionary-en-fr-0.html")), "HTML file should exist");
        assertTrue(Files.exists(tempDir.resolve("dictionary-en-fr.opf")), "OPF file should exist");
    }

    @Test
    void generateOpf_htmlHasCorrectKindleNamespaces(@TempDir Path tempDir) throws IOException {
        writeLexicon(tempDir, "word\t<ol><li>definition</li></ol>");

        OpfUtil.generateOpf(tempDir.resolve("lexicon.txt"), "el", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-el-en-0.html"));
        String kindleNs = "https://kindlegen.s3.amazonaws.com/AmazonKindlePublishingGuidelines.pdf";
        assertTrue(html.contains("xmlns:mbp=\"" + kindleNs + "\""), "mbp namespace URI must be exact");
        assertTrue(html.contains("xmlns:idx=\"" + kindleNs + "\""), "idx namespace URI must be exact");
    }

    @Test
    void generateOpf_htmlHasCorrectEntryStructure(@TempDir Path tempDir) throws IOException {
        writeLexicon(tempDir, "hello\t<ol><li>a greeting</li></ol>");

        OpfUtil.generateOpf(tempDir.resolve("lexicon.txt"), "el", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-el-en-0.html"));
        assertTrue(html.contains("<idx:entry name=\"word\" scriptable=\"yes\">"),
                "entry element must have correct attributes");
        assertTrue(html.contains("<idx:orth value=\"hello\">"),
                "orth value must be the normalised key");
        assertTrue(html.contains("<strong>hello</strong>"), "display term in <strong>");
        assertTrue(html.contains("<ol><li>a greeting</li></ol>"), "definition passed through unchanged");
    }

    @Test
    void generateOpf_opfHasCorrectStructure(@TempDir Path tempDir) throws Exception {
        writeLexicon(tempDir, "hello\t<ol><li>a greeting</li></ol>");

        OpfUtil.generateOpf(tempDir.resolve("lexicon.txt"), "el", "en", "Greek\u2013English Dictionary", tempDir);

        Document doc = parseXml(tempDir.resolve("dictionary-el-en.opf"));

        // package version
        assertEquals("2.0", doc.getDocumentElement().getAttribute("version"),
                "OPF package must be version 2.0");

        // dc:title
        NodeList titles = doc.getElementsByTagName("dc:title");
        assertEquals(1, titles.getLength());
        assertEquals("Greek\u2013English Dictionary", titles.item(0).getTextContent());

        // dc:language
        NodeList langs = doc.getElementsByTagName("dc:language");
        assertEquals(1, langs.getLength());
        assertEquals("el", langs.item(0).getTextContent());

        // dc:identifier is a non-blank UUID
        NodeList ids = doc.getElementsByTagName("dc:identifier");
        assertEquals(1, ids.getLength());
        String uid = ids.item(0).getTextContent();
        assertFalse(uid.isBlank(), "dc:identifier must not be blank");
        assertDoesNotThrow(() -> java.util.UUID.fromString(uid), "dc:identifier must be a valid UUID");

        // DictionaryInLanguage / DictionaryOutLanguage
        String opf = readFile(tempDir.resolve("dictionary-el-en.opf"));
        assertTrue(opf.contains("<DictionaryInLanguage>el</DictionaryInLanguage>"));
        assertTrue(opf.contains("<DictionaryOutLanguage>en</DictionaryOutLanguage>"));

        // manifest and spine reference the HTML file
        assertTrue(opf.contains("href=\"dictionary-el-en-0.html\""), "manifest must reference HTML file");
        assertTrue(opf.contains("idref=\"dictionary0\""), "spine must reference dictionary0");
    }

    @Test
    void generateOpf_entriesAreSorted(@TempDir Path tempDir) throws IOException {
        // Entries written in reverse alphabetical order.
        writeLexicon(tempDir,
                "zebra\t<ol><li>animal</li></ol>",
                "apple\t<ol><li>fruit</li></ol>",
                "mango\t<ol><li>fruit</li></ol>");

        OpfUtil.generateOpf(tempDir.resolve("lexicon.txt"), "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        int applePos = html.indexOf("value=\"apple\"");
        int mangoPos = html.indexOf("value=\"mango\"");
        int zebraPos = html.indexOf("value=\"zebra\"");

        assertTrue(applePos < mangoPos, "apple should appear before mango");
        assertTrue(mangoPos < zebraPos, "mango should appear before zebra");
    }

    @Test
    void generateOpf_keyNormalisationApplied(@TempDir Path tempDir) throws IOException {
        // Term "Hello" should produce key "hello".
        writeLexicon(tempDir, "Hello\t<ol><li>a greeting</li></ol>");

        OpfUtil.generateOpf(tempDir.resolve("lexicon.txt"), "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        assertTrue(html.contains("value=\"hello\""), "key must be lowercased");
        assertTrue(html.contains("<strong>Hello</strong>"), "display term preserves original capitalisation");
    }

    @Test
    void generateOpf_sameKeyEntriesAreGrouped(@TempDir Path tempDir) throws IOException {
        // "Apple" and "apple" normalise to the same key → one idx:entry, joined definitions.
        writeLexicon(tempDir,
                "Apple\t<ol><li>a fruit</li></ol>",
                "apple\t<ol><li>a tech company</li></ol>");

        OpfUtil.generateOpf(tempDir.resolve("lexicon.txt"), "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        long entryCount = html.lines()
                .filter(l -> l.contains("<idx:entry name=\"word\""))
                .count();
        assertEquals(1, entryCount,
                "both terms share the same key → should produce exactly one idx:entry");
        assertTrue(html.contains("a fruit"), "first definition must be present");
        assertTrue(html.contains("a tech company"), "second definition must be present");
        assertTrue(html.indexOf("a fruit") < html.indexOf("a tech company"),
                "definitions should be joined in insertion order");
    }

    @Test
    void generateOpf_splitIntoChunksOfTenThousand(@TempDir Path tempDir) throws IOException {
        // 10,001 unique entries must produce 2 HTML files.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 10_000; i++) {
            sb.append("word%05d\t<ol><li>def %d</li></ol>\n".formatted(i, i));
        }
        Files.writeString(tempDir.resolve("lexicon.txt"), sb.toString(), StandardCharsets.UTF_8);

        OpfUtil.generateOpf(tempDir.resolve("lexicon.txt"), "en", "en", "Test", tempDir);

        assertTrue(Files.exists(tempDir.resolve("dictionary-en-en-0.html")), "first HTML file must exist");
        assertTrue(Files.exists(tempDir.resolve("dictionary-en-en-1.html")), "second HTML file must exist");
        assertFalse(Files.exists(tempDir.resolve("dictionary-en-en-2.html")),
                "no third file for 10,001 entries");

        // OPF must reference both files.
        String opf = readFile(tempDir.resolve("dictionary-en-en.opf"));
        assertTrue(opf.contains("href=\"dictionary-en-en-0.html\""));
        assertTrue(opf.contains("href=\"dictionary-en-en-1.html\""));
    }

    @Test
    void generateOpf_skipBlankAndCommentLines(@TempDir Path tempDir) throws IOException {
        writeLexicon(tempDir,
                "",
                "# this is a comment",
                "hello\t<ol><li>a greeting</li></ol>",
                "   ",
                "# another comment");

        OpfUtil.generateOpf(tempDir.resolve("lexicon.txt"), "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        long entryCount = html.lines()
                .filter(l -> l.contains("<idx:entry name=\"word\""))
                .count();
        assertEquals(1, entryCount, "only one real entry — blank/comment lines must be skipped");
    }

    @Test
    void generateOpf_autoTitleIncludesLanguageNames(@TempDir Path tempDir) throws IOException {
        writeLexicon(tempDir, "bonjour\t<ol><li>hello</li></ol>");

        String title = OpfUtil.autoTitle("fr", "en");
        OpfUtil.generateOpf(tempDir.resolve("lexicon.txt"), "fr", "en", title, tempDir);

        String opf = readFile(tempDir.resolve("dictionary-fr-en.opf"));
        assertTrue(opf.contains("<dc:title>" + title + "</dc:title>"),
                "OPF title must match the auto-generated title");
        assertTrue(title.contains("French"), "auto title should mention French, got: " + title);
    }

    // ── readLexicon unit tests ────────────────────────────────────────────────

    @Test
    void readLexicon_groupsSameNormalisedKey(@TempDir Path tempDir) throws IOException {
        writeLexicon(tempDir,
                "Run\t<ol><li>to move fast</li></ol>",
                "run\t<ol><li>a running session</li></ol>");

        TreeMap<String, List<String[]>> defs = OpfUtil.readLexicon(tempDir.resolve("lexicon.txt"));

        assertEquals(1, defs.size(), "both terms normalise to 'run' → one key");
        List<String[]> entries = defs.get("run");
        assertNotNull(entries);
        assertEquals(2, entries.size(), "two term-definition pairs under the same key");
    }

    @Test
    void readLexicon_sortsKeysAlphabetically(@TempDir Path tempDir) throws IOException {
        writeLexicon(tempDir,
                "zoo\t<ol><li>animal park</li></ol>",
                "ant\t<ol><li>insect</li></ol>",
                "cat\t<ol><li>feline</li></ol>");

        TreeMap<String, List<String[]>> defs = OpfUtil.readLexicon(tempDir.resolve("lexicon.txt"));

        List<String> keys = List.copyOf(defs.keySet());
        assertEquals(List.of("ant", "cat", "zoo"), keys, "keys must be in alphabetical order");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void writeLexicon(Path dir, String... lines) throws IOException {
        Files.writeString(dir.resolve("lexicon.txt"),
                String.join("\n", lines) + "\n",
                StandardCharsets.UTF_8);
    }

    private static String readFile(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static Document parseXml(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        // Suppress DTD resolution errors (OPF may reference external DTDs).
        builder.setEntityResolver((publicId, systemId) ->
                new org.xml.sax.InputSource(new java.io.StringReader("")));
        return builder.parse(file.toFile());
    }
}
