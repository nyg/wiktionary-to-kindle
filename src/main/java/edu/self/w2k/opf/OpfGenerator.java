package edu.self.w2k.opf;

import edu.self.w2k.lexicon.LexiconEntry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

public interface OpfGenerator {
    void generate(TreeMap<String, List<LexiconEntry>> defs, String srcLang, String trgLang, String title, Path outputDir) throws IOException;
}
