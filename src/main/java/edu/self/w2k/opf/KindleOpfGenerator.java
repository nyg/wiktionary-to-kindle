package edu.self.w2k.opf;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import edu.self.w2k.lexicon.LexiconEntry;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates Kindle-compatible OPF and HTML dictionary files from a pre-grouped lexicon.
 * <p>Output:
 * <ul>
 *   <li>{@code dictionary-{src}-{trg}-{n}.html} — Kindle MobiPocket HTML entry files (≤10,000 entries each)</li>
 *   <li>{@code dictionary-{src}-{trg}.opf}       — OPF 2.0 manifest referencing all HTML files</li>
 * </ul>
 */
@Slf4j
public class KindleOpfGenerator implements OpfGenerator {

    private static final int ENTRIES_PER_FILE = 10_000;

    private static final String KINDLE_NS =
            "https://kindlegen.s3.amazonaws.com/AmazonKindlePublishingGuidelines.pdf";

    @Override
    public void generate(TreeMap<String, List<LexiconEntry>> defs, String srcLang, String trgLang,
            String title, Path outputDir) throws IOException {

        log.info("Generating OPF for lang={}->{}, title=\"{}\"", srcLang, trgLang, title);
        log.info("{} unique keys loaded from lexicon", defs.size());

        List<String> htmlFileNames = writeHtmlFiles(defs, outputDir, srcLang, trgLang);
        writeOpfFile(outputDir, srcLang, trgLang, title, htmlFileNames);

        log.info("OPF generation complete: {} HTML file(s) + 1 OPF", htmlFileNames.size());
    }

    /**
     * Builds an auto-generated title from the source and target language codes.
     * Example: {@code "el"} + {@code "en"} → {@code "Modern Greek–English Dictionary"}.
     */
    public static String autoTitle(String srcLang, String trgLang) {
        String src = Locale.forLanguageTag(srcLang).getDisplayLanguage(Locale.ENGLISH);
        String trg = Locale.forLanguageTag(trgLang).getDisplayLanguage(Locale.ENGLISH);
        // Locale returns the bare tag for unknown codes — uppercase it as a readable fallback.
        if (src.isBlank() || src.equalsIgnoreCase(srcLang)) {
            src = srcLang.toUpperCase(Locale.ROOT);
        }
        if (trg.isBlank() || trg.equalsIgnoreCase(trgLang)) {
            trg = trgLang.toUpperCase(Locale.ROOT);
        }
        return src + "–" + trg + " Dictionary";
    }

    // ── step 1: write HTML files ──────────────────────────────────────────────

    /**
     * Writes the HTML dictionary files in chunks of {@value #ENTRIES_PER_FILE} keys.
     *
     * @return list of HTML file names (base names only, relative to outputDir)
     */
    private List<String> writeHtmlFiles(TreeMap<String, List<LexiconEntry>> defs,
            Path outputDir, String srcLang, String trgLang)
            throws IOException {

        List<String> fileNames = new ArrayList<>();
        List<Map.Entry<String, List<LexiconEntry>>> entries = new ArrayList<>(defs.entrySet());

        for (int start = 0, fileIndex = 0; start < entries.size(); start += ENTRIES_PER_FILE, fileIndex++) {
            int end = Math.min(start + ENTRIES_PER_FILE, entries.size());
            String fileName = htmlFileName(srcLang, trgLang, fileIndex);
            writeHtmlFile(outputDir.resolve(fileName), entries.subList(start, end));
            fileNames.add(fileName);
            log.debug("Wrote {} ({} entries)", fileName, end - start);
        }

        return fileNames;
    }

    private static String htmlFileName(String srcLang, String trgLang, int index) {
        return "dictionary-%s-%s-%d.html".formatted(srcLang, trgLang, index).toLowerCase(Locale.ROOT);
    }

    private static void writeHtmlFile(Path file, List<Map.Entry<String, List<LexiconEntry>>> entries)
            throws IOException {

        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8))) {

            w.write("""
                    <html xmlns:mbp="%s"
                          xmlns:idx="%s">
                    <head>
                        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
                    </head>
                    <body>
                        <mbp:pagebreak/>
                        <mbp:frameset>
                            <mbp:slave-frame display="bottom" device="all" breadth="auto" leftmargin="0" rightmargin="0" bottommargin="0" topmargin="0">
                                <div align="center" bgcolor="yellow">
                                    <a onclick="index_search()">Dictionary Search</a>
                                </div>
                            </mbp:slave-frame>
                            <mbp:pagebreak/>
                    """.formatted(KINDLE_NS, KINDLE_NS));

            for (var entry : entries) {
                writeEntry(w, entry.getKey(), entry.getValue());
            }

            w.write("""
                        </mbp:frameset>
                    </body>
                    </html>
                    """);
        }
    }

    /**
     * Writes a single {@code <idx:entry>} block for one key.
     * When multiple entries share the same key, their definitions are joined with {@code "; "}.
     */
    private static void writeEntry(BufferedWriter w, String key, List<LexiconEntry> lexiconEntries)
            throws IOException {

        String displayTerm = lexiconEntries.getFirst().word();
        StringBuilder combinedDef = new StringBuilder();
        for (int i = 0; i < lexiconEntries.size(); i++) {
            if (i > 0) {
                combinedDef.append("; ");
            }
            combinedDef.append(lexiconEntries.get(i).definition());
        }

        w.write("""
                        <idx:entry name="word" scriptable="yes">
                            <idx:orth value="%s"><strong>%s</strong></idx:orth>
                            %s
                        </idx:entry>
                """.formatted(key, displayTerm, combinedDef));
    }

    // ── step 2: write OPF file ────────────────────────────────────────────────

    private static void writeOpfFile(Path outputDir, String srcLang, String trgLang,
            String title, List<String> htmlFileNames) throws IOException {

        Path opfFile = outputDir.resolve("dictionary-%s-%s.opf".formatted(srcLang, trgLang).toLowerCase(Locale.ROOT));

        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(opfFile), StandardCharsets.UTF_8))) {

            w.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <package version="2.0" xmlns="http://www.idpf.org/2007/opf" unique-identifier="uid">
                    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
                        <dc:identifier id="uid">%s</dc:identifier>
                        <dc:title>%s</dc:title>
                        <dc:language>%s</dc:language>
                        <x-metadata>
                            <DictionaryInLanguage>%s</DictionaryInLanguage>
                            <DictionaryOutLanguage>%s</DictionaryOutLanguage>
                        </x-metadata>
                    </metadata>
                    <manifest>
                    """.formatted(UUID.randomUUID(), title, srcLang, srcLang, trgLang));

            for (int i = 0; i < htmlFileNames.size(); i++) {
                w.write("    <item id=\"dictionary%d\" href=\"%s\" media-type=\"application/xhtml+xml\"/>\n"
                        .formatted(i, htmlFileNames.get(i)));
            }

            w.write("</manifest>\n<spine>\n");
            for (int i = 0; i < htmlFileNames.size(); i++) {
                w.write("    <itemref idref=\"dictionary%d\"/>\n".formatted(i));
            }

            w.write("""
                    </spine>
                    <guide>
                        <reference type="search" title="Dictionary Search" onclick="index_search()"/>
                    </guide>
                    </package>
                    """);
        }
    }
}
