package edu.self.w2k.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

/**
 * Generates Kindle-compatible OPF and HTML dictionary files from a tab-delimited lexicon file.
 *
 * <p>Input format (one entry per line):
 * <pre>word{TAB}definition</pre>
 *
 * <p>Output:
 * <ul>
 *   <li>{@code dictionary-{src}-{trg}-{n}.html} — Kindle MobiPocket HTML entry files (≤10,000 entries each)</li>
 *   <li>{@code dictionary-{src}-{trg}.opf}       — OPF 2.0 manifest referencing all HTML files</li>
 * </ul>
 */
@Slf4j
@UtilityClass
public class OpfUtil {

    private static final int ENTRIES_PER_FILE = 10_000;

    private static final String KINDLE_NS =
            "https://kindlegen.s3.amazonaws.com/AmazonKindlePublishingGuidelines.pdf";

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Generates OPF and HTML files from {@code lexiconFile} into {@code outputDir}.
     *
     * @param lexiconFile tab-delimited lexicon ({@code word\tdefinition} per line)
     * @param srcLang     ISO 639-1 source language code (e.g. {@code "el"})
     * @param trgLang     ISO 639-1 target language code (e.g. {@code "en"})
     * @param title       human-readable dictionary title for the OPF metadata
     * @param outputDir   directory where output files are written
     */
    public static void generateOpf(Path lexiconFile, String srcLang, String trgLang,
                                   String title, Path outputDir) throws IOException {

        log.info("Generating OPF for lang={}->{}, title=\"{}\"", srcLang, trgLang, title);

        TreeMap<String, List<String[]>> defs = readLexicon(lexiconFile);
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
        if (src.isBlank() || src.equalsIgnoreCase(srcLang)) src = srcLang.toUpperCase(Locale.ROOT);
        if (trg.isBlank() || trg.equalsIgnoreCase(trgLang)) trg = trgLang.toUpperCase(Locale.ROOT);
        return src + "\u2013" + trg + " Dictionary";
    }

    // ── step 1: read and group ────────────────────────────────────────────────

    /**
     * Reads the lexicon file and groups entries by normalised key.
     * The returned {@link TreeMap} is sorted alphabetically by key.
     * Each value is a list of {@code [term, definition]} pairs sharing that key.
     */
    static TreeMap<String, List<String[]>> readLexicon(Path lexiconFile) throws IOException {

        TreeMap<String, List<String[]>> defs = new TreeMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(lexiconFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                int tab = line.indexOf('\t');
                if (tab < 0) {
                    log.warn("Skipping malformed line (no tab): {}", line);
                    continue;
                }
                String term = line.substring(0, tab).strip();
                String defn = line.substring(tab + 1);
                String key = normaliseKey(term);
                if (key.isEmpty()) {
                    log.warn("Skipping entry with empty key: {}", term);
                    continue;
                }
                defs.computeIfAbsent(key, k -> new ArrayList<>()).add(new String[]{term, defn});
            }
        }

        return defs;
    }

    /**
     * Normalises a term into a lookup key: lowercase, strip, replace {@code "} with {@code '},
     * and escape {@code <}/{@code >} for the Kindle index.
     */
    static String normaliseKey(String term) {
        return term
                .replace('"', '\'')
                .replace("<", "\\<")
                .replace(">", "\\>")
                .toLowerCase(Locale.ROOT)
                .strip();
    }

    // ── step 2: write HTML files ──────────────────────────────────────────────

    /**
     * Writes the HTML dictionary files in chunks of {@value #ENTRIES_PER_FILE} keys.
     *
     * @return list of HTML file names (base names only, relative to outputDir)
     */
    private static List<String> writeHtmlFiles(TreeMap<String, List<String[]>> defs,
                                               Path outputDir, String srcLang, String trgLang)
            throws IOException {

        List<String> fileNames = new ArrayList<>();
        List<Map.Entry<String, List<String[]>>> entries = new ArrayList<>(defs.entrySet());

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

    private static void writeHtmlFile(Path file, List<Map.Entry<String, List<String[]>>> entries)
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
     * When multiple terms share the same key, their definitions are joined with {@code "; "}.
     */
    private static void writeEntry(BufferedWriter w, String key, List<String[]> termDefs)
            throws IOException {

        // Display the first term as the heading; join all definitions.
        String displayTerm = termDefs.getFirst()[0];
        StringBuilder combinedDef = new StringBuilder();
        for (int i = 0; i < termDefs.size(); i++) {
            if (i > 0) combinedDef.append("; ");
            combinedDef.append(termDefs.get(i)[1]);
        }

        w.write("""
                        <idx:entry name="word" scriptable="yes">
                            <idx:orth value="%s"><strong>%s</strong></idx:orth>
                            %s
                        </idx:entry>
                """.formatted(key, displayTerm, combinedDef));
    }

    // ── step 3: write OPF file ────────────────────────────────────────────────

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
