package edu.self.w2k.write.opf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import edu.self.w2k.model.LexiconEntry;
import edu.self.w2k.write.DictionaryTitles;
import edu.self.w2k.write.DictionaryWriter;
import edu.self.w2k.progress.ProgressListener;
import edu.self.w2k.progress.ProgressListener.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpfDictionaryWriter implements DictionaryWriter {

    private final ProgressListener progress;

    public OpfDictionaryWriter() {
        this(ProgressListener.NOOP);
    }

    public OpfDictionaryWriter(ProgressListener progress) {
        this.progress = progress;
    }

    @Override
    public Path write(TreeMap<String, List<LexiconEntry>> defs, String srcLang, String trgLang,
                      String title, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        log.info("Generating OPF for lang={}->{}, title=\"{}\"", srcLang, trgLang, title);
        log.info("{} unique keys loaded from lexicon", defs.size());

        List<Map.Entry<String, List<LexiconEntry>>> entries = new ArrayList<>(defs.entrySet());
        int totalChapters = chapterCount(entries.size());
        OutputNames names = OutputNames.of(srcLang, trgLang, totalChapters);

        for (int start = 0, idx = 0; start < entries.size(); start += HtmlChapterRenderer.ENTRIES_PER_CHAPTER, idx++) {
            int end = Math.min(start + HtmlChapterRenderer.ENTRIES_PER_CHAPTER, entries.size());
            String fileName = names.chapters().get(idx);
            Files.write(outputDir.resolve(fileName), HtmlChapterRenderer.render(entries.subList(start, end)));
            log.debug("Wrote {} ({} entries)", fileName, end - start);
            progress.onProgress(Stage.WRITE_HTML, idx + 1L, totalChapters);
        }

        writeCoverImage(outputDir, names.cover());
        String uid = UUID.randomUUID().toString();
        writeTocNcx(outputDir, names, uid, title);
        Path opfPath = writeOpfFile(outputDir, srcLang, trgLang, title, uid, names);
        log.info("OPF generation complete: {} HTML file(s) + 1 OPF + NCX", names.chapters().size());
        return opfPath;
    }

    /**
     * Every file one generation produces, all named from a single stem.
     * <p>
     * Several dictionaries share one output directory, so a fixed {@code toc.ncx} or {@code cover.jpg}
     * would have each run overwrite the previous one's side-artefacts.
     */
    private record OutputNames(String opf, String cover, String ncx, List<String> chapters) {

        static OutputNames of(String srcLang, String trgLang, int chapterCount) {
            String base = DictionaryTitles.baseName(srcLang, trgLang);
            return new OutputNames(base + ".opf", base + "-cover.jpg", base + "-toc.ncx",
                                   IntStream.range(0, chapterCount)
                                            .mapToObj(i -> "%s-%d.html".formatted(base, i))
                                            .toList());
        }
    }

    /** Number of chapter files {@code entryCount} entries will be chunked into. */
    static int chapterCount(int entryCount) {
        return Math.ceilDiv(entryCount, HtmlChapterRenderer.ENTRIES_PER_CHAPTER);
    }

    private static void writeCoverImage(Path outputDir, String fileName) throws IOException {
        BufferedImage img = new BufferedImage(600, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 600, 800);
        g.dispose();
        ImageIO.write(img, "JPEG", outputDir.resolve(fileName).toFile());
    }

    private static void writeTocNcx(Path outputDir, OutputNames names, String uid, String title)
            throws IOException {
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(outputDir.resolve(names.ncx())),
                        StandardCharsets.UTF_8))) {
            w.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <!DOCTYPE ncx PUBLIC "-//NISO//DTD ncx 2005-1//EN" "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd">
                    <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
                    <head>
                        <meta name="dtb:uid" content="%s"/>
                        <meta name="dtb:depth" content="1"/>
                        <meta name="dtb:totalPageCount" content="0"/>
                        <meta name="dtb:maxPageNumber" content="0"/>
                    </head>
                    <docTitle><text>%s</text></docTitle>
                    <navMap>
                        <navPoint id="entry" playOrder="1">
                            <navLabel><text>%s</text></navLabel>
                            <content src="%s"/>
                        </navPoint>
                    </navMap>
                    </ncx>
                    """.formatted(uid, title, title, names.chapters().getFirst()));
        }
    }

    private static Path writeOpfFile(Path outputDir, String srcLang, String trgLang,
                                     String title, String uid, OutputNames names) throws IOException {
        List<String> htmlFileNames = names.chapters();
        Path opfPath = outputDir.resolve(names.opf());

        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(opfPath), StandardCharsets.UTF_8))) {

            w.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <package version="2.0" xmlns="http://www.idpf.org/2007/opf" unique-identifier="uid">
                    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
                        <dc:identifier id="uid">%s</dc:identifier>
                        <dc:title>%s</dc:title>
                        <dc:language>%s</dc:language>
                        <meta name="cover" content="cover-image"/>
                        <x-metadata>
                            <DictionaryInLanguage>%s</DictionaryInLanguage>
                            <DictionaryOutLanguage>%s</DictionaryOutLanguage>
                            <DefaultLookupIndex>word</DefaultLookupIndex>
                        </x-metadata>
                    </metadata>
                    <manifest>
                        <item id="cover-image" href="%s" media-type="image/jpeg"/>
                        <item id="toc" href="%s" media-type="application/x-dtbncx+xml"/>
                    """.formatted(uid, title, srcLang, srcLang, trgLang, names.cover(), names.ncx()));

            for (int i = 0; i < htmlFileNames.size(); i++) {
                w.write("    <item id=\"dictionary%d\" href=\"%s\" media-type=\"application/xhtml+xml\"/>%n"
                        .formatted(i, htmlFileNames.get(i)));
            }

            w.write("</manifest>%n<spine toc=\"toc\">%n".formatted());
            for (int i = 0; i < htmlFileNames.size(); i++) {
                w.write("    <itemref idref=\"dictionary%d\"/>%n".formatted(i));
            }

            w.write("""
                    </spine>
                    <guide>
                        <reference type="search" title="Dictionary Search" onclick="index_search()"/>
                        <reference type="index" title="Dictionary Index" href="%s"/>
                    </guide>
                    </package>
                    """.formatted(htmlFileNames.getFirst()));
        }
        return opfPath;
    }
}
