package edu.self.w2k.gui;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import edu.self.w2k.command.GenerateCommand;
import edu.self.w2k.config.Preferences;
import edu.self.w2k.download.DownloadResult;
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
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs the whole download-then-generate pipeline on a background thread, so the one-click flow is a
 * single cancellable unit of work.
 * <p>
 * Cancellation needs two mechanisms, not one. {@code cancel(true)} interrupts the worker, which the
 * download and parse loops check; but the kindling-cli stage is blocked in {@code Process.waitFor()},
 * where interrupting the waiting thread would leave the subprocess running and the MOBI half-written.
 * The process reference is therefore tracked and destroyed in {@link Task#cancelled()}.
 */
@Slf4j
public class PipelineService extends Service<Path> {

    private final MainViewModel viewModel;
    private final ProgressListener progress;

    public PipelineService(MainViewModel viewModel, ProgressListener progress) {
        this.viewModel = viewModel;
        this.progress = progress;
    }

    @Override
    protected Task<Path> createTask() {
        Preferences prefs = viewModel.preferencesProperty().get();
        String editionLang = viewModel.editionProperty().get().code();
        String wordLang = viewModel.wordLanguageProperty().get().code();

        return new PipelineTask(prefs, editionLang, wordLang, progress);
    }

    /** Package-private so the pipeline can be exercised without an FX toolkit. */
    static class PipelineTask extends Task<Path> {

        private final Preferences prefs;
        private final String editionLang;
        private final String wordLang;
        private final ProgressListener progress;
        private final AtomicReference<Process> kindlingProcess = new AtomicReference<>();

        PipelineTask(Preferences prefs, String editionLang, String wordLang, ProgressListener progress) {
            this.prefs = prefs;
            this.editionLang = editionLang;
            this.wordLang = wordLang;
            this.progress = progress;
        }

        @Override
        protected Path call() throws Exception {
            DownloadResult download =
                    new KaikkiDumpDownloader(editionLang, prefs.dumpsDir(), progress).download();
            if (download.alreadyPresent()) {
                log.info("Using already-downloaded dump: {}", download.dumpPath());
            }

            String version = prefs.kindlingVersion().orElseGet(() -> KindlingRelease.load().version());
            KindlingCliResolver resolver =
                    new KindlingCliResolver(version, prefs.kindlingCliPath(), new KindlingDownloader());

            KindlingDictionaryConverter writer = new KindlingDictionaryConverter(
                    new OpfDictionaryWriter(progress),
                    resolver,
                    KindlingDictionaryConverter.defaultRunner(kindlingProcess::set),
                    progress);

            return new GenerateCommand(
                    new JsonlDictionaryParser(progress),
                    new HtmlDefinitionRenderer(),
                    writer,
                    download.dumpPath(),
                    prefs.dictionariesDir(),
                    wordLang,
                    editionLang,
                    DictionaryTitles.autoTitle(wordLang, editionLang),
                    progress
            ).execute();
        }

        @Override
        protected void cancelled() {
            super.cancelled();
            Optional.ofNullable(kindlingProcess.get())
                    .filter(Process::isAlive)
                    .ifPresent(process -> {
                        log.warn("Cancelled — terminating kindling-cli");
                        process.destroy();
                    });
        }
    }
}
