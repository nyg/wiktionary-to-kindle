package edu.self.w2k.opf;

import edu.self.w2k.lexicon.LexiconEntry;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class KindleOpfGeneratorTest {

    private final KindleOpfGenerator generator = new KindleOpfGenerator();

    // ── autoTitle unit tests ──────────────────────────────────────────────────

    @Test
    void autoTitle_knownLanguageCodes() {
        String title = KindleOpfGenerator.autoTitle("fr", "en");
        assertTrue(title.contains("French"), "should contain 'French', got: " + title);
        assertTrue(title.contains("English"), "should contain 'English', got: " + title);
        assertTrue(title.endsWith("Dictionary"), "should end with 'Dictionary'");
    }

    @Test
    void autoTitle_unknownCodeFallsBackToUpperCase() {
        String title = KindleOpfGenerator.autoTitle("xx", "en");
        assertTrue(title.contains("XX"), "unknown code should fall back to upper-case, got: " + title);
    }

    // ── generate integration tests ────────────────────────────────────────────

    @Test
    void generateOpf_createsExpectedFiles(@TempDir Path tempDir) throws IOException {
        TreeMap<String, List<LexiconEntry>> defs = buildDefs(
                "hello\t<ol><li>a greeting</li></ol>");

        generator.generate(defs, "en", "fr", "Test Dictionary", tempDir);

        assertTrue(Files.exists(tempDir.resolve("dictionary-en-fr-0.html")), "HTML file should exist");
        assertTrue(Files.exists(tempDir.resolve("dictionary-en-fr.opf")), "OPF file should exist");
    }

    @Test
    void generateOpf_htmlHasCorrectKindleNamespaces(@TempDir Path tempDir) throws IOException {
        TreeMap<String, List<LexiconEntry>> defs = buildDefs(
                "word\t<ol><li>definition</li></ol>");

        generator.generate(defs, "el", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-el-en-0.html"));
        String kindleNs = "https://kindlegen.s3.amazonaws.com/AmazonKindlePublishingGuidelines.pdf";
        assertTrue(html.contains("xmlns:mbp=\"" + kindleNs + "\""), "mbp namespace URI must be exact");
        assertTrue(html.contains("xmlns:idx=\"" + kindleNs + "\""), "idx namespace URI must be exact");
    }

    @Test
    void generateOpf_htmlHasCorrectEntryStructure(@TempDir Path tempDir) throws IOException {
        TreeMap<String, List<LexiconEntry>> defs = buildDefs(
                "hello\t<ol><li>a greeting</li></ol>");

        generator.generate(defs, "el", "en", "Test", tempDir);

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
        TreeMap<String, List<LexiconEntry>> defs = buildDefs(
                "hello\t<ol><li>a greeting</li></ol>");

        generator.generate(defs, "el", "en", "Greek\u2013English Dictionary", tempDir);

        Document doc = parseXml(tempDir.resolve("dictionary-el-en.opf"));

        assertEquals("2.0", doc.getDocumentElement().getAttribute("version"),
                "OPF package must be version 2.0");

        NodeList titles = doc.getElementsByTagName("dc:title");
        assertEquals(1, titles.getLength());
        assertEquals("Greek\u2013English Dictionary", titles.item(0).getTextContent());

        NodeList langs = doc.getElementsByTagName("dc:language");
        assertEquals(1, langs.getLength());
        assertEquals("el", langs.item(0).getTextContent());

        NodeList ids = doc.getElementsByTagName("dc:identifier");
        assertEquals(1, ids.getLength());
        String uid = ids.item(0).getTextContent();
        assertFalse(uid.isBlank(), "dc:identifier must not be blank");
        assertDoesNotThrow(() -> java.util.UUID.fromString(uid), "dc:identifier must be a valid UUID");

        String opf = readFile(tempDir.resolve("dictionary-el-en.opf"));
        assertTrue(opf.contains("<DictionaryInLanguage>el</DictionaryInLanguage>"));
        assertTrue(opf.contains("<DictionaryOutLanguage>en</DictionaryOutLanguage>"));
        assertTrue(opf.contains("href=\"dictionary-el-en-0.html\""), "manifest must reference HTML file");
        assertTrue(opf.contains("idref=\"dictionary0\""), "spine must reference dictionary0");
    }

    @Test
    void generateOpf_entriesAreSorted(@TempDir Path tempDir) throws IOException {
        TreeMap<String, List<LexiconEntry>> defs = buildDefs(
                "zebra\t<ol><li>animal</li></ol>",
                "apple\t<ol><li>fruit</li></ol>",
                "mango\t<ol><li>fruit</li></ol>");

        generator.generate(defs, "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        int applePos = html.indexOf("value=\"apple\"");
        int mangoPos = html.indexOf("value=\"mango\"");
        int zebraPos = html.indexOf("value=\"zebra\"");

        assertTrue(applePos < mangoPos, "apple should appear before mango");
        assertTrue(mangoPos < zebraPos, "mango should appear before zebra");
    }

    @Test
    void generateOpf_keyNormalisationApplied(@TempDir Path tempDir) throws IOException {
        // Key "hello" maps to LexiconEntry with word "Hello" (original capitalisation preserved).
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        defs.put("hello", List.of(new LexiconEntry("Hello", "<ol><li>a greeting</li></ol>")));

        generator.generate(defs, "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        assertTrue(html.contains("value=\"hello\""), "key must be lowercased");
        assertTrue(html.contains("<strong>Hello</strong>"), "display term preserves original capitalisation");
    }

    @Test
    void generateOpf_sameKeyEntriesAreGrouped(@TempDir Path tempDir) throws IOException {
        // "Apple" and "apple" normalise to the same key → one idx:entry, joined definitions.
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        List<LexiconEntry> entries = new ArrayList<>();
        entries.add(new LexiconEntry("Apple", "<ol><li>a fruit</li></ol>"));
        entries.add(new LexiconEntry("apple", "<ol><li>a tech company</li></ol>"));
        defs.put("apple", entries);

        generator.generate(defs, "en", "en", "Test", tempDir);

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
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        for (int i = 0; i <= 10_000; i++) {
            String word = "word%05d".formatted(i);
            defs.put(word, List.of(new LexiconEntry(word, "<ol><li>def %d</li></ol>".formatted(i))));
        }

        generator.generate(defs, "en", "en", "Test", tempDir);

        assertTrue(Files.exists(tempDir.resolve("dictionary-en-en-0.html")), "first HTML file must exist");
        assertTrue(Files.exists(tempDir.resolve("dictionary-en-en-1.html")), "second HTML file must exist");
        assertFalse(Files.exists(tempDir.resolve("dictionary-en-en-2.html")),
                "no third file for 10,001 entries");

        String opf = readFile(tempDir.resolve("dictionary-en-en.opf"));
        assertTrue(opf.contains("href=\"dictionary-en-en-0.html\""));
        assertTrue(opf.contains("href=\"dictionary-en-en-1.html\""));
    }

    @Test
    void generateOpf_singleEntryProducesOneIdxEntry(@TempDir Path tempDir) throws IOException {
        TreeMap<String, List<LexiconEntry>> defs = buildDefs(
                "hello\t<ol><li>a greeting</li></ol>");

        generator.generate(defs, "en", "en", "Test", tempDir);

        String html = readFile(tempDir.resolve("dictionary-en-en-0.html"));
        long entryCount = html.lines()
                .filter(l -> l.contains("<idx:entry name=\"word\""))
                .count();
        assertEquals(1, entryCount, "a single entry in defs must produce exactly one idx:entry");
    }

    @Test
    void generateOpf_autoTitleIncludesLanguageNames(@TempDir Path tempDir) throws IOException {
        TreeMap<String, List<LexiconEntry>> defs = buildDefs(
                "bonjour\t<ol><li>hello</li></ol>");

        String title = KindleOpfGenerator.autoTitle("fr", "en");
        generator.generate(defs, "fr", "en", title, tempDir);

        String opf = readFile(tempDir.resolve("dictionary-fr-en.opf"));
        assertTrue(opf.contains("<dc:title>" + title + "</dc:title>"),
                "OPF title must match the auto-generated title");
        assertTrue(title.contains("French"), "auto title should mention French, got: " + title);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a {@link TreeMap} of defs from tab-delimited {@code word\tdefinition} strings.
     * Entries sharing the same normalised key are grouped under that key.
     */
    private static TreeMap<String, List<LexiconEntry>> buildDefs(String... lines) {
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        for (String line : lines) {
            int tab = line.indexOf('\t');
            if (tab < 0) continue;
            String term = line.substring(0, tab).strip();
            String defn = line.substring(tab + 1);
            String key = term.replace('"', '\'')
                    .replace("<", "\\<")
                    .replace(">", "\\>")
                    .toLowerCase(Locale.ROOT)
                    .strip();
            if (key.isEmpty()) continue;
            defs.computeIfAbsent(key, k -> new ArrayList<>()).add(new LexiconEntry(term, defn));
        }
        return defs;
    }

    private static String readFile(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static Document parseXml(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) ->
                new org.xml.sax.InputSource(new java.io.StringReader("")));
        return builder.parse(file.toFile());
    }
}
