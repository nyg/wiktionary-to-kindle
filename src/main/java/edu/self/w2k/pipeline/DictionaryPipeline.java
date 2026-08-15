package edu.self.w2k.pipeline;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import edu.self.w2k.command.GenerateCommand;
import edu.self.w2k.config.Preferences;
import edu.self.w2k.download.DownloadResult;
import edu.self.w2k.download.DumpDownloader;
import edu.self.w2k.download.KaikkiDumpDownloader;
import edu.self.w2k.kindling.KindlingCliResolver;
import edu.self.w2k.kindling.KindlingDictionaryConverter;
import edu.self.w2k.kindling.KindlingDownloader;
import edu.self.w2k.kindling.KindlingRelease;
import edu.self.w2k.parse.JsonlDictionaryParser;
import edu.self.w2k.progress.ProgressListener;
import edu.self.w2k.render.HtmlDefinitionRenderer;
import edu.self.w2k.write.DictionaryTitles;
import edu.self.w2k.write.opf.OpfDictionaryWriter;
import lombok.extern.slf4j.Slf4j;

/**
 * The combined download-then-generate flow behind the GUI's single button.
 * <p>
 * Deliberately free of JavaFX: the orchestration is the part worth testing, and tangling it with
 * {@code javafx.concurrent.Task} would make it reachable only through a UI test. The GUI's
 * {@code PipelineTask} is a thin wrapper that adds cancellation and thread management.
 * <p>
 * The two collaborators are injectable so a test can substitute them; the defaults hold the real
 * wiring.
 */
@Slf4j
public class DictionaryPipeline {

    /** Obtains the dump for an edition, downloading it if it is not already on disk. */
    @FunctionalInterface
    public interface DownloaderFactory {
        DumpDownloader create(String editionLang, Path dumpsDir, ProgressListener progress);
    }

    /**
     * Turns a downloaded dump into a dictionary.
     *
     * @param onKindlingStart receives the kindling-cli process so a caller can destroy it on cancel
     */
    @FunctionalInterface
    public interface Generator {
        Path generate(Path dumpFile,
                      Preferences prefs,
                      String wordLang,
                      String editionLang,
                      ProgressListener progress,
                      Consumer<Process> onKindlingStart) throws IOException;
    }

    private final DownloaderFactory downloaderFactory;
    private final Generator generator;

    public DictionaryPipeline() {
        this(KaikkiDumpDownloader::new, DictionaryPipeline::generateWithKindling);
    }

    public DictionaryPipeline(DownloaderFactory downloaderFactory, Generator generator) {
        this.downloaderFactory = downloaderFactory;
        this.generator = generator;
    }

    /**
     * Downloads the dump if needed, then generates the dictionary.
     *
     * @return the generated {@code .mobi}
     */
    public Path run(Preferences prefs,
                    String editionLang,
                    String wordLang,
                    ProgressListener progress,
                    Consumer<Process> onKindlingStart) throws IOException {

        DownloadResult download =
                downloaderFactory.create(editionLang, prefs.dumpsDir(), progress).download();
        if (download.alreadyPresent()) {
            log.info("Using already-downloaded dump: {}", download.dumpPath());
        }

        return generator.generate(download.dumpPath(), prefs, wordLang, editionLang,
                                  progress, onKindlingStart);
    }

    private static Path generateWithKindling(Path dumpFile,
                                             Preferences prefs,
                                             String wordLang,
                                             String editionLang,
                                             ProgressListener progress,
                                             Consumer<Process> onKindlingStart) throws IOException {
        String version = prefs.kindlingVersion().orElseGet(() -> KindlingRelease.load().version());
        KindlingCliResolver resolver =
                new KindlingCliResolver(version, prefs.kindlingCliPath(), new KindlingDownloader());

        KindlingDictionaryConverter writer = new KindlingDictionaryConverter(
                new OpfDictionaryWriter(progress),
                resolver,
                KindlingDictionaryConverter.defaultRunner(onKindlingStart),
                progress,
                prefs.deleteIntermediateFiles());

        return new GenerateCommand(
                new JsonlDictionaryParser(progress),
                new HtmlDefinitionRenderer(),
                writer,
                dumpFile,
                prefs.dictionariesDir(),
                wordLang,
                editionLang,
                DictionaryTitles.autoTitle(wordLang, editionLang),
                progress
        ).execute();
    }
}
