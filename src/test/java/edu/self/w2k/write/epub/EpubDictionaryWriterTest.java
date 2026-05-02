package edu.self.w2k.write.epub;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.self.w2k.model.LexiconEntry;

class EpubDictionaryWriterTest {

    private final EpubDictionaryWriter writer = new EpubDictionaryWriter();

    @Test
    void write_mimetypeIsFirstAndUncompressed(@TempDir Path tempDir) throws IOException {
        Path epub = writer.write(buildDefs("hello\ta greeting"), "el", "en", "Test", tempDir);

        try (ZipFile zip = new ZipFile(epub.toFile())) {
            ZipEntry mimetype = zip.getEntry("mimetype");
            assertTrue(mimetype != null, "mimetype entry must exist");
            assertEquals(ZipEntry.STORED, mimetype.getMethod(), "mimetype must be stored uncompressed");
            String content = new String(zip.getInputStream(mimetype).readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("application/epub+zip", content.trim());
        }
    }

    @Test
    void write_containerXmlExists(@TempDir Path tempDir) throws IOException {
        Path epub = writer.write(buildDefs("hello\ta greeting"), "el", "en", "Test", tempDir);

        try (ZipFile zip = new ZipFile(epub.toFile())) {
            assertTrue(zip.getEntry("META-INF/container.xml") != null,
                    "META-INF/container.xml must exist");
        }
    }

    @Test
    void write_opfHasRequiredMetadata(@TempDir Path tempDir) throws IOException {
        Path epub = writer.write(buildDefs("hello\ta greeting"), "el", "en",
                "Greek–English Dictionary", tempDir);

        String opf = readOpf(epub);
        assertTrue(opf.contains("<dc:title>"), "OPF must have dc:title");
        assertTrue(opf.contains("Greek–English Dictionary"), "title value must match");
        assertTrue(opf.contains("<dc:language>el</dc:language>")
                || opf.contains(">el<"), "OPF must have dc:language = el");

        // UUID identifier — skip past the opening tag's attributes to the text content
        String idBlock = extractBetween(opf, "<dc:identifier", "</dc:identifier>");
        String idValue = idBlock.contains(">") ? idBlock.substring(idBlock.indexOf('>') + 1).trim() : idBlock.trim();
        assertDoesNotThrow(() -> UUID.fromString(idValue),
                "dc:identifier must be a valid UUID, got: " + idValue);
    }

    @Test
    void write_opfHasXMetadata(@TempDir Path tempDir) throws IOException {
        Path epub = writer.write(buildDefs("hello\ta greeting"), "el", "en", "Test", tempDir);

        String opf = readOpf(epub);
        assertTrue(opf.contains("<DictionaryInLanguage>el</DictionaryInLanguage>"),
                "OPF must contain DictionaryInLanguage");
        assertTrue(opf.contains("<DictionaryOutLanguage>en</DictionaryOutLanguage>"),
                "OPF must contain DictionaryOutLanguage");
    }

    @Test
    void write_opfManifestAndSpineReferenceChapters(@TempDir Path tempDir) throws IOException {
        Path epub = writer.write(buildDefs("hello\ta greeting"), "el", "en", "Test", tempDir);

        String opf = readOpf(epub);
        assertTrue(opf.contains("chapter0.xhtml"), "manifest must reference chapter0.xhtml");
        assertTrue(opf.contains("idref="), "spine must have at least one itemref");
    }

    @Test
    void write_chapterContainsKindleMarkup(@TempDir Path tempDir) throws IOException {
        Path epub = writer.write(buildDefs("hello\ta greeting"), "el", "en", "Test", tempDir);

        String chapterContent = readChapter(epub, "chapter0.xhtml");
        assertTrue(chapterContent.contains("xmlns:idx"), "chapter must declare xmlns:idx");
        assertTrue(chapterContent.contains("xmlns:mbp"), "chapter must declare xmlns:mbp");
        assertTrue(chapterContent.contains("<idx:entry name=\"word\" scriptable=\"yes\">"),
                "chapter must contain idx:entry");
    }

    @Test
    void write_10001EntriesProduceTwoChapters(@TempDir Path tempDir) throws IOException {
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        for (int i = 0; i <= 10_000; i++) {
            String w = "word%05d".formatted(i);
            defs.put(w, List.of(new LexiconEntry(w, "<ol><li>def</li></ol>")));
        }

        Path epub = writer.write(defs, "en", "en", "Test", tempDir);

        List<String> chapters = listChapterFiles(epub);
        assertTrue(chapters.size() >= 2, "10,001 entries must produce at least 2 chapter files");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String readOpf(Path epub) throws IOException {
        try (ZipFile zip = new ZipFile(epub.toFile())) {
            String containerXml = read(zip, "META-INF/container.xml");
            String opfPath = extractBetween(containerXml, "full-path=\"", "\"");
            return read(zip, opfPath);
        }
    }

    private static String readChapter(Path epub, String name) throws IOException {
        try (ZipFile zip = new ZipFile(epub.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.getName().endsWith(name)) {
                    return read(zip, e.getName());
                }
            }
        }
        throw new IOException("Chapter not found: " + name);
    }

    private static List<String> listChapterFiles(Path epub) throws IOException {
        List<String> result = new ArrayList<>();
        try (ZipFile zip = new ZipFile(epub.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.getName().endsWith(".xhtml")) {
                    result.add(e.getName());
                }
            }
        }
        return result;
    }

    private static String read(ZipFile zip, String path) throws IOException {
        ZipEntry entry = zip.getEntry(path);
        if (entry == null) throw new IOException("Entry not found in EPUB: " + path);
        try (InputStream is = zip.getInputStream(entry)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extractBetween(String src, String open, String close) {
        int start = src.indexOf(open);
        if (start < 0) return "";
        start += open.length();
        int end = src.indexOf(close, start);
        return end < 0 ? "" : src.substring(start, end);
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
