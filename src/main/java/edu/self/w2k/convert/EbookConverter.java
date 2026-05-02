package edu.self.w2k.convert;

import java.io.IOException;
import java.nio.file.Path;

public interface EbookConverter {
    Path convert(Path input, Path outputDir, String outputFormat) throws IOException;
}
