package edu.self.w2k.write;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

import edu.self.w2k.model.LexiconEntry;

public interface DictionaryWriter {
    Path write(TreeMap<String, List<LexiconEntry>> defs, String srcLang, String trgLang, String title, Path outputDir) throws IOException;
}
