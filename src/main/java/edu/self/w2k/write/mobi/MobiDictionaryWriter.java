package edu.self.w2k.write.mobi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

import edu.self.w2k.convert.EbookConverter;
import edu.self.w2k.model.LexiconEntry;
import edu.self.w2k.write.DictionaryWriter;
import edu.self.w2k.write.epub.EpubDictionaryWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MobiDictionaryWriter implements DictionaryWriter {

    private final EpubDictionaryWriter epubWriter;
    private final EbookConverter converter;

    @Override
    public Path write(TreeMap<String, List<LexiconEntry>> defs, String srcLang, String trgLang,
            String title, Path outputDir) throws IOException {
        Path epubPath = epubWriter.write(defs, srcLang, trgLang, title, outputDir);
        log.info("Converting {} to MOBI", epubPath.getFileName());
        return converter.convert(epubPath, outputDir, "mobi");
    }
}
