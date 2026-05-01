package edu.self.w2k.lexicon;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Slf4j
public class TsvLexiconWriter implements LexiconWriter {

    @Override
    public void write(Path outputFile, Stream<LexiconEntry> entries) throws IOException {
        AtomicLong count = new AtomicLong();
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8);
             Stream<LexiconEntry> stream = entries) {
            for (LexiconEntry entry : (Iterable<LexiconEntry>) stream::iterator) {
                writer.write(entry.word());
                writer.write("\t");
                writer.write(entry.definition());
                writer.write("\n");
                count.incrementAndGet();
            }
        }
        log.info("Done. {} entries written to {}", count.get(), outputFile);
    }
}
