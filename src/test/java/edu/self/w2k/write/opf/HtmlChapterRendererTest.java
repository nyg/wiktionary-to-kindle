package edu.self.w2k.write.opf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import edu.self.w2k.model.LexiconEntry;

class HtmlChapterRendererTest {

    @Test
    void render_containsKindleNamespaces() {
        String html = render(entry("word", "def"));
        assertTrue(html.contains("xmlns:mbp=\"" + HtmlChapterRenderer.KINDLE_NS + "\""),
                "mbp namespace must be present");
        assertTrue(html.contains("xmlns:idx=\"" + HtmlChapterRenderer.KINDLE_NS + "\""),
                "idx namespace must be present");
    }

    @Test
    void render_correctEntryStructure() {
        String html = render(entry("hello", "<ol><li>a greeting</li></ol>"));
        assertTrue(html.contains("<idx:entry name=\"word\" scriptable=\"yes\">"),
                "entry element must have correct attributes");
        assertTrue(html.contains("<idx:orth value=\"hello\">"),
                "orth value must equal display term");
        assertTrue(html.contains("<strong>hello</strong>"), "display term in <strong>");
        assertTrue(html.contains("<ol><li>a greeting</li></ol>"), "definition passed through");
    }

    @Test
    void render_sortedEntriesPreserveOrder() {
        // TreeMap guarantees alphabetical order — verify it is preserved in output
        TreeMap<String, List<LexiconEntry>> defs = buildDefs(
                "zebra\t<ol><li>animal</li></ol>",
                "apple\t<ol><li>fruit</li></ol>",
                "mango\t<ol><li>fruit</li></ol>");
        String html = renderDefs(defs);

        int applePos = html.indexOf("value=\"apple\"");
        int mangoPos = html.indexOf("value=\"mango\"");
        int zebraPos = html.indexOf("value=\"zebra\"");
        assertTrue(applePos < mangoPos, "apple before mango");
        assertTrue(mangoPos < zebraPos, "mango before zebra");
    }

    @Test
    void render_valueMatchesDisplayTerm() {
        // value attribute must match the visible display term so kindling can locate entries in the text blob
        List<Map.Entry<String, List<LexiconEntry>>> entries = List.of(
                Map.entry("hello", List.of(new LexiconEntry("Hello", "<ol><li>greet</li></ol>"))));
        String html = new String(HtmlChapterRenderer.render(entries), StandardCharsets.UTF_8);
        assertTrue(html.contains("value=\"Hello\""), "value must match display term (original case)");
        assertTrue(html.contains("<strong>Hello</strong>"), "display term in <strong>");
    }

    @Test
    void render_sameKeyEntriesJoined() {
        List<LexiconEntry> entries = new ArrayList<>();
        entries.add(new LexiconEntry("Apple", "<ol><li>a fruit</li></ol>"));
        entries.add(new LexiconEntry("apple", "<ol><li>a tech company</li></ol>"));
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        defs.put("apple", entries);

        String html = renderDefs(defs);
        long entryCount = html.lines().filter(l -> l.contains("<idx:entry name=\"word\"")).count();
        assertEquals(1, entryCount, "same key → one idx:entry");
        assertTrue(html.contains("a fruit"), "first def present");
        assertTrue(html.contains("a tech company"), "second def present");
        assertTrue(html.indexOf("a fruit") < html.indexOf("a tech company"),
                "definitions joined in insertion order");
    }

    @Test
    void render_singleEntryProducesOneIdxEntry() {
        String html = render(entry("hello", "<ol><li>a greeting</li></ol>"));
        long count = html.lines().filter(l -> l.contains("<idx:entry name=\"word\"")).count();
        assertEquals(1, count, "single entry → one idx:entry");
    }

    @Test
    void render_chunkSplitAt10001() {
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        for (int i = 0; i <= 10_000; i++) {
            String w = "word%05d".formatted(i);
            defs.put(w, List.of(new LexiconEntry(w, "<ol><li>def</li></ol>")));
        }
        List<Map.Entry<String, List<LexiconEntry>>> all = new ArrayList<>(defs.entrySet());
        // First chunk: 10_000 entries
        String first = new String(HtmlChapterRenderer.render(all.subList(0, 10_000)), StandardCharsets.UTF_8);
        // Second chunk: 1 entry
        String second = new String(HtmlChapterRenderer.render(all.subList(10_000, 10_001)), StandardCharsets.UTF_8);

        long firstCount = first.lines().filter(l -> l.contains("<idx:entry name=\"word\"")).count();
        long secondCount = second.lines().filter(l -> l.contains("<idx:entry name=\"word\"")).count();
        assertEquals(10_000, firstCount, "first chunk has 10,000 entries");
        assertEquals(1, secondCount, "second chunk has 1 entry");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String render(Map.Entry<String, List<LexiconEntry>> entry) {
        return new String(HtmlChapterRenderer.render(List.of(entry)), StandardCharsets.UTF_8);
    }

    private static String renderDefs(TreeMap<String, List<LexiconEntry>> defs) {
        return new String(HtmlChapterRenderer.render(new ArrayList<>(defs.entrySet())),
                StandardCharsets.UTF_8);
    }

    private static Map.Entry<String, List<LexiconEntry>> entry(String key, String def) {
        return Map.entry(key, List.of(new LexiconEntry(key, def)));
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
