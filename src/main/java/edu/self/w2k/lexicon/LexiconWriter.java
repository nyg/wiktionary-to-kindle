package edu.self.w2k.lexicon;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface LexiconWriter {

    void write(Path outputFile, Stream<LexiconEntry> entries) throws IOException;
}
