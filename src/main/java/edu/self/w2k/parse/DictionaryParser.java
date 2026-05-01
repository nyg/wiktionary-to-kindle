package edu.self.w2k.parse;

import edu.self.w2k.model.WiktionaryEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface DictionaryParser {

    Stream<WiktionaryEntry> parse(Path dumpFile, String lang) throws IOException;
}
