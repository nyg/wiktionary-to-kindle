package edu.self.w2k.command;

import edu.self.w2k.lexicon.LexiconEntry;
import edu.self.w2k.lexicon.LexiconReader;
import edu.self.w2k.lexicon.LexiconWriter;
import edu.self.w2k.opf.OpfGenerator;
import edu.self.w2k.parse.DictionaryParser;
import edu.self.w2k.render.DefinitionRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.stream.Stream;

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
        Stream<LexiconEntry> lexiconEntries = parser.parse(dumpFile, lang)
                .map(e -> new LexiconEntry(e.getWord(), renderer.render(e.getSenses())))
                .filter(e -> e.definition() != null);

        writer.write(lexiconFile, lexiconEntries);

        generator.generate(reader.read(lexiconFile), lang, lang, title, outputDir);
    }
}
