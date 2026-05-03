package edu.self.w2k.write.opf;

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

import edu.self.w2k.model.LexiconEntry;
import edu.self.w2k.write.DictionaryWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpfDictionaryWriter implements DictionaryWriter {

    @Override
    public Path write(TreeMap<String, List<LexiconEntry>> defs, String srcLang, String trgLang,
                      String title, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        log.info("Generating OPF for lang={}->{}, title=\"{}\"", srcLang, trgLang, title);
        log.info("{} unique keys loaded from lexicon", defs.size());

        List<Map.Entry<String, List<LexiconEntry>>> entries = new ArrayList<>(defs.entrySet());
        List<String> htmlFileNames = new ArrayList<>();

        for (int start = 0, idx = 0; start < entries.size(); start += HtmlChapterRenderer.ENTRIES_PER_CHAPTER, idx++) {
            int end = Math.min(start + HtmlChapterRenderer.ENTRIES_PER_CHAPTER, entries.size());
            String fileName = htmlFileName(srcLang, trgLang, idx);
            Files.write(outputDir.resolve(fileName), HtmlChapterRenderer.render(entries.subList(start, end)));
            htmlFileNames.add(fileName);
            log.debug("Wrote {} ({} entries)", fileName, end - start);
        }

        Path opfPath = writeOpfFile(outputDir, srcLang, trgLang, title, htmlFileNames);
        log.info("OPF generation complete: {} HTML file(s) + 1 OPF", htmlFileNames.size());
        return opfPath;
    }

    private static String htmlFileName(String srcLang, String trgLang, int index) {
        return "dictionary-%s-%s-%d.html".formatted(srcLang, trgLang, index).toLowerCase(Locale.ROOT);
    }

    private static Path writeOpfFile(Path outputDir, String srcLang, String trgLang,
                                     String title, List<String> htmlFileNames) throws IOException {
        Path opfPath = outputDir.resolve(
                "dictionary-%s-%s.opf".formatted(srcLang, trgLang).toLowerCase(Locale.ROOT));

        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(opfPath), StandardCharsets.UTF_8))) {

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
        return opfPath;
    }
}
