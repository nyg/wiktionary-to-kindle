package edu.self.w2k.kindling;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;

import edu.self.w2k.model.LexiconEntry;
import edu.self.w2k.progress.ProgressListener;
import edu.self.w2k.progress.ProgressListener.Stage;
import edu.self.w2k.write.DictionaryTitles;
import edu.self.w2k.write.DictionaryWriter;
import edu.self.w2k.write.IntermediateFiles;
import edu.self.w2k.write.opf.OpfDictionaryWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KindlingDictionaryConverter implements DictionaryWriter {

    @FunctionalInterface
    public interface ProcessRunner {
        int run(List<String> command) throws IOException;
    }

    /**
     * Runs the binary and relays its output to the log.
     * <p>
     * Deliberately not {@code inheritIO()}: a windowed application has no terminal attached, so
     * inherited output is discarded and kindling-cli failures become invisible. Pumping the merged
     * streams through SLF4J instead means the output reaches the console for the CLI and the log pane
     * for the GUI, with consistent formatting in both.
     */
    public static ProcessRunner defaultRunner() {
        return defaultRunner(_ -> {});
    }

    /**
     * As {@link #defaultRunner()}, but hands the started process to {@code onStart} so a caller can
     * destroy it on cancellation — {@code waitFor()} alone cannot be interrupted out of usefully,
     * since interrupting the waiting thread leaves the subprocess running.
     */
    public static ProcessRunner defaultRunner(Consumer<Process> onStart) {
        return cmd -> {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            onStart.accept(process);
            try (BufferedReader out = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = out.readLine()) != null) {
                    log.info("kindling-cli: {}", line);
                }
            }
            try {
                return process.waitFor();
            }
            catch (InterruptedException e) {
                process.destroy();
                Thread.currentThread().interrupt();
                throw new IOException("kindling-cli interrupted", e);
            }
        };
    }

    private final OpfDictionaryWriter opfWriter;
    private final KindlingCliResolver resolver;
    private final ProcessRunner runner;
    private final ProgressListener progress;
    private final boolean deleteIntermediateFiles;

    public KindlingDictionaryConverter(OpfDictionaryWriter opfWriter, KindlingCliResolver resolver, ProcessRunner runner) {
        this(opfWriter, resolver, runner, ProgressListener.NOOP);
    }

    public KindlingDictionaryConverter(OpfDictionaryWriter opfWriter, KindlingCliResolver resolver,
                                       ProcessRunner runner, ProgressListener progress) {
        this(opfWriter, resolver, runner, progress, false);
    }

    public KindlingDictionaryConverter(OpfDictionaryWriter opfWriter, KindlingCliResolver resolver,
                                       ProcessRunner runner, ProgressListener progress,
                                       boolean deleteIntermediateFiles) {
        this.opfWriter = opfWriter;
        this.resolver = resolver;
        this.runner = runner;
        this.progress = progress;
        this.deleteIntermediateFiles = deleteIntermediateFiles;
    }

    /**
     * Writes the working files into {@code outputDir}'s {@code intermediate} sub-directory and the
     * {@code .mobi} into {@code outputDir} itself, so the dictionaries folder holds only the files a
     * user has any use for. kindling-cli resolves the OPF's hrefs relative to the OPF, and every file
     * it names moves together, so the sub-directory is invisible to it.
     */
    @Override
    public Path write(TreeMap<String, List<LexiconEntry>> defs,
                      String srcLang,
                      String trgLang,
                      String title,
                      Path outputDir) throws IOException {
        Path workDir = IntermediateFiles.dirFor(outputDir, srcLang, trgLang);
        Path opfPath = opfWriter.write(defs, srcLang, trgLang, title, workDir);
        Path mobiPath = outputDir.resolve(DictionaryTitles.baseName(srcLang, trgLang) + ".mobi");

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
        progress.onProgress(Stage.KINDLING, 0, ProgressListener.TOTAL_UNKNOWN);
        int exitCode = runner.run(cmd);
        if (exitCode != 0) {
            throw new IOException("kindling-cli exited with code " + exitCode);
        }
        if (deleteIntermediateFiles) {
            cleanUp(workDir);
        }
        return mobiPath;
    }

    /**
     * A cleanup failure never fails the run: the {@code .mobi} the user asked for already exists, and
     * leftover working files are an annoyance, not a broken result.
     */
    private static void cleanUp(Path workDir) {
        try {
            IntermediateFiles.delete(workDir);
            log.info("Deleted intermediate files in {}", workDir);
        }
        catch (IOException e) {
            log.warn("Could not delete intermediate files in {}: {}", workDir, e.getLocalizedMessage());
        }
    }
}
