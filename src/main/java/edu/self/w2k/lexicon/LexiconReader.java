package edu.self.w2k.lexicon;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

public interface LexiconReader {
    TreeMap<String, List<LexiconEntry>> read(Path lexiconFile) throws IOException;
}
