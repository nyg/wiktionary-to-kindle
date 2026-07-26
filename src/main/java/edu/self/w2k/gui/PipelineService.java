package edu.self.w2k.gui;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import edu.self.w2k.config.Preferences;
import edu.self.w2k.pipeline.DictionaryPipeline;
import edu.self.w2k.progress.ProgressListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs {@link DictionaryPipeline} on a background thread so the one-click flow is a single
 * cancellable unit of work. The orchestration itself lives in the pipeline class, which keeps it
 * testable without a UI; this adds only threading and cancellation.
 * <p>
 * Cancellation needs two mechanisms, not one. {@code cancel(true)} interrupts the worker, which the
 * download and parse loops check; but the kindling-cli stage blocks in {@code Process.waitFor()},
 * where interrupting the waiting thread would leave the subprocess running and the MOBI
 * half-written. The process reference is therefore tracked and destroyed in {@link Task#cancelled()}.
 */
@Slf4j
public class PipelineService extends Service<Path> {

    private final MainViewModel viewModel;
    private final ProgressListener progress;
    private final DictionaryPipeline pipeline;

    public PipelineService(MainViewModel viewModel, ProgressListener progress) {
        this(viewModel, progress, new DictionaryPipeline());
    }

    PipelineService(MainViewModel viewModel, ProgressListener progress, DictionaryPipeline pipeline) {
        this.viewModel = viewModel;
        this.progress = progress;
        this.pipeline = pipeline;
    }

    @Override
    protected Task<Path> createTask() {
        return new PipelineTask(pipeline,
                                viewModel.preferencesProperty().get(),
                                viewModel.editionProperty().get().code(),
                                viewModel.wordLanguageProperty().get().code(),
                                progress);
    }

    static class PipelineTask extends Task<Path> {

        private final DictionaryPipeline pipeline;
        private final Preferences prefs;
        private final String editionLang;
        private final String wordLang;
        private final ProgressListener progress;
        private final AtomicReference<Process> kindlingProcess = new AtomicReference<>();

        PipelineTask(DictionaryPipeline pipeline, Preferences prefs, String editionLang,
                     String wordLang, ProgressListener progress) {
            this.pipeline = pipeline;
            this.prefs = prefs;
            this.editionLang = editionLang;
            this.wordLang = wordLang;
            this.progress = progress;
        }

        @Override
        protected Path call() throws Exception {
            return pipeline.run(prefs, editionLang, wordLang, progress, kindlingProcess::set);
        }

        @Override
        protected void cancelled() {
            super.cancelled();
            terminateKindling();
        }

        /** Package-private so cancellation can be verified without driving a real Service. */
        void terminateKindling() {
            Optional.ofNullable(kindlingProcess.get())
                    .filter(Process::isAlive)
                    .ifPresent(process -> {
                        log.warn("Cancelled — terminating kindling-cli");
                        process.destroy();
                    });
        }
    }
}
