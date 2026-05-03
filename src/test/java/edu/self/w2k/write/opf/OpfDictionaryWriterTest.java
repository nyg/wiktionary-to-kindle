package edu.self.w2k.write.opf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import edu.self.w2k.model.LexiconEntry;

class OpfDictionaryWriterTest {

    private final OpfDictionaryWriter writer = new OpfDictionaryWriter();

    @Test
    void write_returnsOpfPath(@TempDir Path tempDir) throws Exception {
        Path opf = writer.write(buildDefs("alpha\tdef1", "beta\tdef2", "gamma\tdef3"),
                "el", "en", "Test", tempDir);
        assertEquals(tempDir.resolve("dictionary-el-en.opf"), opf);
    }

    @Test
    void write_opfHasDictionaryLanguageTags(@TempDir Path tempDir) throws Exception {
        Path opf = writer.write(buildDefs("alpha\tdef1"), "el", "en", "Test", tempDir);
        Document doc = parseXml(opf);

        NodeList inLang = doc.getElementsByTagName("DictionaryInLanguage");
        NodeList outLang = doc.getElementsByTagName("DictionaryOutLanguage");
        assertEquals(1, inLang.getLength(), "DictionaryInLanguage must appear once");
        assertEquals("el", inLang.item(0).getTextContent(), "DictionaryInLanguage must be el");
        assertEquals(1, outLang.getLength(), "DictionaryOutLanguage must appear once");
        assertEquals("en", outLang.item(0).getTextContent(), "DictionaryOutLanguage must be en");
    }

    @Test
    void write_manifestItemsHrefToExistingHtmlFiles(@TempDir Path tempDir) throws Exception {
        Path opf = writer.write(buildDefs("alpha\tdef1", "beta\tdef2"), "el", "en", "Test", tempDir);
        Document doc = parseXml(opf);

        NodeList items = doc.getElementsByTagName("item");
        assertTrue(items.getLength() > 0, "manifest must contain at least one item");
        for (int i = 0; i < items.getLength(); i++) {
            String href = items.item(i).getAttributes().getNamedItem("href").getNodeValue();
            assertTrue(Files.exists(tempDir.resolve(href)),
                    "Referenced HTML file must exist: " + href);
        }
    }

    @Test
    void write_htmlFilesContainKindleMarkup(@TempDir Path tempDir) throws Exception {
        writer.write(buildDefs("alpha\tdef1"), "el", "en", "Test", tempDir);

        List<Path> htmlFiles = new ArrayList<>();
        try (var stream = Files.newDirectoryStream(tempDir, "*.html")) {
            for (Path p : stream) htmlFiles.add(p);
        }
        assertTrue(htmlFiles.size() >= 1, "at least one HTML file must be created");
        for (Path html : htmlFiles) {
            String content = Files.readString(html);
            assertTrue(content.contains("xmlns:idx"), "HTML must declare xmlns:idx");
            assertTrue(content.contains("<idx:entry"), "HTML must contain idx:entry");
        }
    }

    @Test
    void write_moreThan10000KeysProduceMultipleHtmlFiles(@TempDir Path tempDir) throws Exception {
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        for (int i = 0; i <= 10_000; i++) {
            String w = "word%05d".formatted(i);
            defs.put(w, List.of(new LexiconEntry(w, "<ol><li>def</li></ol>")));
        }

        writer.write(defs, "el", "en", "Test", tempDir);

        List<Path> htmlFiles = new ArrayList<>();
        try (var stream = Files.newDirectoryStream(tempDir, "*.html")) {
            for (Path p : stream) htmlFiles.add(p);
        }
        assertTrue(htmlFiles.size() >= 2, "10,001 entries must produce at least 2 HTML files");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Document parseXml(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        // Disable external entity processing (not needed here, also safer)
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(path.toFile());
    }

    private static TreeMap<String, List<LexiconEntry>> buildDefs(String... lines) {
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        for (String line : lines) {
            int tab = line.indexOf('\t');
            if (tab < 0) continue;
            String term = line.substring(0, tab).strip();
            String defn = line.substring(tab + 1);
            defs.computeIfAbsent(term, k -> new ArrayList<>()).add(new LexiconEntry(term, defn));
        }
        return defs;
    }
}
