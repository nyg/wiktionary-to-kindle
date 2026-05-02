package edu.self.w2k.command;

import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;

import edu.self.w2k.lexicon.LexiconEntry;
import edu.self.w2k.lexicon.LexiconReader;
import edu.self.w2k.lexicon.LexiconWriter;
import edu.self.w2k.opf.OpfGenerator;
import edu.self.w2k.parse.DictionaryParser;
import edu.self.w2k.render.DefinitionRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GenerateCommand implements Command {

    private final DictionaryParser parser;
    private final DefinitionRenderer renderer;
    private final LexiconWriter writer;
    private final LexiconReader reader;
    private final OpfGenerator generator;
    private final Path dumpFile;
    private final Path lexiconFile;
    private final Path outputDir;
    private final String lang;
    private final String title;

    @Override
    public void run() throws Exception {
        // Parse dump file - one word (JSON object) per line
        Stream<LexiconEntry> lexiconEntries = parser.parse(dumpFile, lang)
                // Render deserialized word into HTML
                .map(e -> new LexiconEntry(e.getWord(), renderer.render(e.getSenses())))
                .filter(e -> e.definition() != null);

        // Write rendered HTML into a TSV file
        writer.write(lexiconFile, lexiconEntries);

        // Read TSV file
        TreeMap<String, List<LexiconEntry>> tsv = reader.read(lexiconFile);

        // Generate OPF and HTML file - this can then be converted to an MOBI or KF8 Kindle ebooks
        // or into a EPUB 2 or 3 eBook
        generator.generate(tsv, lang, lang, title, outputDir);
    }
}
