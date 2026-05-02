package edu.self.w2k.write.epub;

import java.io.IOException;
import java.io.OutputStream;
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
import io.documentnode.epub4j.domain.Book;
import io.documentnode.epub4j.domain.Identifier;
import io.documentnode.epub4j.domain.MediaTypes;
import io.documentnode.epub4j.domain.Metadata;
import io.documentnode.epub4j.domain.Resource;
import io.documentnode.epub4j.epub.EpubWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EpubDictionaryWriter implements DictionaryWriter {

    static final int ENTRIES_PER_CHAPTER = 10_000;

    @Override
    public Path write(TreeMap<String, List<LexiconEntry>> defs, String srcLang, String trgLang,
            String title, Path outputDir) throws IOException {

        Files.createDirectories(outputDir);
        log.info("Generating EPUB for lang={}->{}, title=\"{}\"", srcLang, trgLang, title);
        log.info("{} unique keys loaded from lexicon", defs.size());

        Book book = new Book();
        Metadata metadata = book.getMetadata();
        metadata.addTitle(title);
        metadata.addIdentifier(new Identifier(Identifier.Scheme.UUID, UUID.randomUUID().toString()));
        metadata.setLanguage(srcLang);

        List<Map.Entry<String, List<LexiconEntry>>> entries = new ArrayList<>(defs.entrySet());
        int chapterCount = 0;
        for (int start = 0; start < entries.size(); start += ENTRIES_PER_CHAPTER, chapterCount++) {
            int end = Math.min(start + ENTRIES_PER_CHAPTER, entries.size());
            byte[] xhtml = XhtmlChapterRenderer.render(entries.subList(start, end));
            String chapterName = "chapter%d.xhtml".formatted(chapterCount);
            Resource resource = new Resource(xhtml, chapterName);
            resource.setMediaType(MediaTypes.XHTML);
            book.addSection("Chapter " + chapterCount, resource);
            log.debug("Added chapter {} ({} entries)", chapterName, end - start);
        }

        Path epubPath = outputDir.resolve(
                "dictionary-%s-%s.epub".formatted(srcLang, trgLang).toLowerCase(Locale.ROOT));
        try (OutputStream os = Files.newOutputStream(epubPath)) {
            new EpubWriter().write(book, os);
        }
        log.debug("EPUB written to {}", epubPath);

        new OpfPostProcessor(srcLang, trgLang).process(epubPath);
        log.info("EPUB generation complete: {} chapter(s)", chapterCount);

        return epubPath;
    }
}
