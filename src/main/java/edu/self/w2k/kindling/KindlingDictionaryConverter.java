package edu.self.w2k.kindling;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import edu.self.w2k.model.LexiconEntry;
import edu.self.w2k.write.DictionaryWriter;
import edu.self.w2k.write.opf.OpfDictionaryWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KindlingDictionaryConverter implements DictionaryWriter {

    @FunctionalInterface
    public interface ProcessRunner {
        int run(List<String> command) throws IOException;
    }

    public static ProcessRunner defaultRunner() {
        return cmd -> {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.inheritIO();
            try {
                return pb.start().waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("kindling-cli interrupted", e);
            }
        };
    }

    private final OpfDictionaryWriter opfWriter;
    private final KindlingCliResolver resolver;
    private final ProcessRunner runner;

    public KindlingDictionaryConverter(OpfDictionaryWriter opfWriter, KindlingCliResolver resolver,
                                       ProcessRunner runner) {
        this.opfWriter = opfWriter;
        this.resolver = resolver;
        this.runner = runner;
    }

    @Override
    public Path write(TreeMap<String, List<LexiconEntry>> defs, String srcLang, String trgLang,
                      String title, Path outputDir) throws IOException {
        Path opfPath = opfWriter.write(defs, srcLang, trgLang, title, outputDir);
        Path mobiPath = outputDir.resolve(
                "dictionary-%s-%s.mobi".formatted(srcLang, trgLang).toLowerCase(Locale.ROOT));

        Path bin;
        try {
            bin = resolver.resolve();
        } catch (KindlingException e) {
            throw new IOException("Failed to resolve kindling-cli: " + e.getMessage(), e);
        }

        List<String> cmd = List.of(
                bin.toString(), "build", opfPath.toAbsolutePath().toString(),
                "-o", mobiPath.toAbsolutePath().toString());
        log.info("Running: {}", String.join(" ", cmd));
        int exitCode = runner.run(cmd);
        if (exitCode != 0) {
            throw new IOException("kindling-cli exited with code " + exitCode);
        }
        return mobiPath;
    }
}
