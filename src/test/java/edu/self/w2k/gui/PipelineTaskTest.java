package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import edu.self.w2k.config.AppTheme;
import edu.self.w2k.config.Preferences;
import edu.self.w2k.download.DownloadResult;
import edu.self.w2k.gui.PipelineService.PipelineTask;
import edu.self.w2k.pipeline.DictionaryPipeline;
import edu.self.w2k.progress.ProgressListener;
import org.junit.jupiter.api.Test;

/**
 * Covers the Task wrapper directly. {@code Task} itself needs no toolkit — only its
 * {@code Platform.runLater} state callbacks do — so {@code call()} can be invoked in-line.
 */
class PipelineTaskTest {

    private static final Preferences PREFS = new Preferences(Path.of("/data/dumps"),
                                                             Path.of("/data/dictionaries"),
                                                             Optional.empty(),
                                                             Optional.empty(),
                                                             AppTheme.JAVAFX, false);

    private static final Path DUMP = Path.of("/data/dumps/raw-wiktextract-data-el-2026-07-24.jsonl.gz");
    private static final Path MOBI = Path.of("/data/dictionaries/w2k-dictionary-en-el.mobi");

    @Test
    void should_return_the_generated_path_when_called() throws Exception {
        // Given
        DictionaryPipeline pipeline = new DictionaryPipeline(
                (_, _, _) -> () -> new DownloadResult(DUMP, false),
                (_, _, _, _, _, _) -> MOBI);
        PipelineTask task = new PipelineTask(pipeline, PREFS, "el", "en", ProgressListener.NOOP);

        // When / Then
        assertThat(task.call()).isEqualTo(MOBI);
    }

    @Test
    void should_destroy_kindling_process_when_cancelled_mid_build() throws Exception {
        // Given a real long-running subprocess standing in for kindling-cli, registered through the
        // same callback the converter uses
        Process sleeper = new ProcessBuilder("sleep", "30").start();
        DictionaryPipeline pipeline = new DictionaryPipeline(
                (_, _, _) -> () -> new DownloadResult(DUMP, false),
                (_, _, _, _, _, onKindlingStart) -> {
                    onKindlingStart.accept(sleeper);
                    return MOBI;
                });
        PipelineTask task = new PipelineTask(pipeline, PREFS, "el", "en", ProgressListener.NOOP);
        task.call();
        assertThat(sleeper.isAlive()).isTrue();

        // When
        task.terminateKindling();

        // Then — interrupting the waiting thread alone would leave this process running
        assertThat(sleeper.waitFor(20, TimeUnit.SECONDS)).isTrue();
        assertThat(sleeper.isAlive()).isFalse();
    }

    @Test
    void should_do_nothing_when_cancelled_before_kindling_starts() {
        // Given
        DictionaryPipeline pipeline = new DictionaryPipeline(
                (_, _, _) -> () -> new DownloadResult(DUMP, false),
                (_, _, _, _, _, _) -> MOBI);
        PipelineTask task = new PipelineTask(pipeline, PREFS, "el", "en", ProgressListener.NOOP);

        // When / Then — cancelling during download must not blow up on a null process
        assertThatCode(task::terminateKindling).doesNotThrowAnyException();
    }

    @Test
    void should_tolerate_an_already_finished_process_when_cancelled() throws Exception {
        // Given
        Process finished = new ProcessBuilder("true").start();
        finished.waitFor();
        DictionaryPipeline pipeline = new DictionaryPipeline(
                (_, _, _) -> () -> new DownloadResult(DUMP, false),
                (_, _, _, _, _, onKindlingStart) -> {
                    onKindlingStart.accept(finished);
                    return MOBI;
                });
        PipelineTask task = new PipelineTask(pipeline, PREFS, "el", "en", ProgressListener.NOOP);
        task.call();

        // When / Then
        task.terminateKindling();
        assertThat(finished.isAlive()).isFalse();
    }

    @Test
    void should_propagate_pipeline_failure_when_called() {
        // Given
        DictionaryPipeline pipeline = new DictionaryPipeline(
                (_, _, _) -> () -> {
                    throw new java.io.IOException("HTTP 503");
                },
                (_, _, _, _, _, _) -> MOBI);
        PipelineTask task = new PipelineTask(pipeline, PREFS, "el", "en", ProgressListener.NOOP);

        // When / Then
        org.assertj.core.api.Assertions.assertThatIOException()
                .isThrownBy(task::call)
                .withMessage("HTTP 503");
    }

    @Test
    void should_use_the_view_models_selection_when_creating_a_task() throws Exception {
        // Given
        MainViewModel viewModel = new MainViewModel();
        viewModel.preferencesProperty().set(PREFS);
        viewModel.editionProperty().set(edu.self.w2k.config.LanguageCatalog.Language.of("el"));
        viewModel.wordLanguageProperty().set(edu.self.w2k.config.LanguageCatalog.Language.of("en"));

        List<String> seen = new java.util.ArrayList<>();
        DictionaryPipeline pipeline = new DictionaryPipeline(
                (edition, _, _) -> {
                    seen.add(edition);
                    return () -> new DownloadResult(DUMP, false);
                },
                (_, _, wordLang, _, _, _) -> {
                    seen.add(wordLang);
                    return MOBI;
                });
        PipelineService service = new PipelineService(viewModel, ProgressListener.NOOP, pipeline);

        // When
        javafx.concurrent.Task<Path> task = service.createTask();

        // Then
        assertThat(task).isInstanceOf(PipelineTask.class);
        assertThat(((PipelineTask) task).call()).isEqualTo(MOBI);
        assertThat(seen).containsExactly("el", "en");
    }
}
