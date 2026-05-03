package edu.self.w2k.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import edu.self.w2k.model.LexiconEntry;
import edu.self.w2k.parse.DictionaryParser;
import edu.self.w2k.render.DefinitionRenderer;
import edu.self.w2k.write.DictionaryWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GenerateCommand implements Command {

    private final DictionaryParser parser;
    private final DefinitionRenderer renderer;
    private final DictionaryWriter writer;
    private final Path dumpFile;
    private final Path outputDir;
    private final String srcLang;
    private final String trgLang;
    private final String title;

    @Override
    public void run() throws IOException {
        log.info("Using dump: {}", dumpFile);
        TreeMap<String, List<LexiconEntry>> grouped = new TreeMap<>();
        AtomicLong count = new AtomicLong();

        try (Stream<LexiconEntry> stream = parser.parse(dumpFile, srcLang)
                .flatMap(e -> renderer.render(e.senses())
                        .map(def -> new LexiconEntry(e.word(), def))
                        .stream())) {
            stream.forEach(e -> {
                String key = normaliseKey(e.word());
                if (!key.isEmpty()) {
                    grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
                    count.incrementAndGet();
                }
            });
        }

        log.info("Done. {} entries grouped into {} unique keys for srcLang={}, trgLang={}", count.get(), grouped.size(), srcLang, trgLang);

        writer.write(grouped, srcLang, trgLang, title, outputDir);
    }

    /**
     * Normalises a word into a Kindle lookup key: lowercase, strip, replace {@code "} with {@code '},
     * and escape {@code <}/{@code >} for the Kindle index.
     */
    static String normaliseKey(String word) {
        return word
                .replace('"', '\'')
                .replace("<", "\\<")
                .replace(">", "\\>")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
