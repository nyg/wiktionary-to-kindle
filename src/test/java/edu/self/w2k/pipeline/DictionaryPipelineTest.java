package edu.self.w2k.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import edu.self.w2k.config.AppTheme;
import edu.self.w2k.config.Preferences;
import edu.self.w2k.download.DownloadResult;
import edu.self.w2k.progress.ProgressListener;
import org.junit.jupiter.api.Test;

class DictionaryPipelineTest {

    private static final Preferences PREFS = new Preferences(Path.of("/data/dumps"),
                                                             Path.of("/data/dictionaries"),
                                                             Optional.empty(),
                                                             Optional.empty(),
                                                             AppTheme.JAVAFX);

    private static final Path DUMP = Path.of("/data/dumps/raw-wiktextract-data-el-2026-07-24.jsonl.gz");
    private static final Path MOBI = Path.of("/data/dictionaries/w2k-dictionary-en-el.mobi");

    @Test
    void should_pass_downloaded_dump_to_the_generator() throws Exception {
        // Given
        AtomicReference<Path> generatedFrom = new AtomicReference<>();
        DictionaryPipeline unit = new DictionaryPipeline(
                (_, _, _) -> () -> new DownloadResult(DUMP, false),
                (dumpFile, _, _, _, _, _) -> {
                    generatedFrom.set(dumpFile);
                    return MOBI;
                });

        // When
        Path result = unit.run(PREFS, "el", "en", ProgressListener.NOOP, _ -> {});

        // Then
        assertThat(generatedFrom.get()).isEqualTo(DUMP);
        assertThat(result).isEqualTo(MOBI);
    }

    @Test
    void should_generate_from_the_existing_dump_when_already_present() throws Exception {
        // Given a download that was skipped because the file was already on disk
        AtomicReference<Path> generatedFrom = new AtomicReference<>();
        DictionaryPipeline unit = new DictionaryPipeline(
                (_, _, _) -> () -> new DownloadResult(DUMP, true),
                (dumpFile, _, _, _, _, _) -> {
                    generatedFrom.set(dumpFile);
                    return MOBI;
                });

        // When
        Path result = unit.run(PREFS, "el", "en", ProgressListener.NOOP, _ -> {});

        // Then — a skipped download is a success, not a reason to stop
        assertThat(generatedFrom.get()).isEqualTo(DUMP);
        assertThat(result).isEqualTo(MOBI);
    }

    @Test
    void should_pass_configured_dumps_dir_and_edition_to_the_downloader() throws Exception {
        // Given
        List<String> seen = new ArrayList<>();
        DictionaryPipeline unit = new DictionaryPipeline(
                (edition, dumpsDir, _) -> {
                    seen.add(edition);
                    seen.add(dumpsDir.toString());
                    return () -> new DownloadResult(DUMP, false);
                },
                (_, _, _, _, _, _) -> MOBI);

        // When
        unit.run(PREFS, "el", "en", ProgressListener.NOOP, _ -> {});

        // Then
        assertThat(seen).containsExactly("el", PREFS.dumpsDir().toString());
    }

    @Test
    void should_pass_word_language_and_edition_in_the_right_order_to_the_generator() throws Exception {
        // Given the CLI's convention: word language is the source, edition the target
        List<String> seen = new ArrayList<>();
        DictionaryPipeline unit = new DictionaryPipeline(
                (_, _, _) -> () -> new DownloadResult(DUMP, false),
                (_, _, wordLang, editionLang, _, _) -> {
                    seen.add(wordLang);
                    seen.add(editionLang);
                    return MOBI;
                });

        // When
        unit.run(PREFS, "el", "en", ProgressListener.NOOP, _ -> {});

        // Then
        assertThat(seen).containsExactly("en", "el");
    }

    @Test
    void should_not_generate_when_the_download_fails() throws Exception {
        // Given
        AtomicReference<Boolean> generated = new AtomicReference<>(false);
        DictionaryPipeline unit = new DictionaryPipeline(
                (_, _, _) -> () -> {
                    throw new IOException("HTTP 404");
                },
                (_, _, _, _, _, _) -> {
                    generated.set(true);
                    return MOBI;
                });

        // When / Then
        assertThatIOException()
                .isThrownBy(() -> unit.run(PREFS, "zzz", "en", ProgressListener.NOOP, _ -> {}))
                .withMessage("HTTP 404");
        assertThat(generated.get()).isFalse();
    }

    @Test
    void should_propagate_generation_failure() {
        // Given
        DictionaryPipeline unit = new DictionaryPipeline(
                (_, _, _) -> () -> new DownloadResult(DUMP, false),
                (_, _, _, _, _, _) -> {
                    throw new IOException("kindling-cli exited with code 1");
                });

        // When / Then
        assertThatIOException()
                .isThrownBy(() -> unit.run(PREFS, "el", "en", ProgressListener.NOOP, _ -> {}))
                .withMessageContaining("kindling-cli");
    }

    @Test
    void should_forward_the_progress_listener_to_both_stages() throws Exception {
        // Given
        ProgressListener listener = (_, _, _) -> {};
        List<ProgressListener> seen = new ArrayList<>();
        DictionaryPipeline unit = new DictionaryPipeline(
                (_, _, progress) -> {
                    seen.add(progress);
                    return () -> new DownloadResult(DUMP, false);
                },
                (_, _, _, _, progress, _) -> {
                    seen.add(progress);
                    return MOBI;
                });

        // When
        unit.run(PREFS, "el", "en", listener, _ -> {});

        // Then — a dropped listener would leave the progress bar frozen for a whole stage
        assertThat(seen).containsExactly(listener, listener);
    }

    @Test
    void should_forward_the_kindling_process_callback_so_cancel_can_terminate_it() throws Exception {
        // Given
        java.util.function.Consumer<Process> callback = _ -> {};
        AtomicReference<Object> seen = new AtomicReference<>();
        DictionaryPipeline unit = new DictionaryPipeline(
                (_, _, _) -> () -> new DownloadResult(DUMP, false),
                (_, _, _, _, _, onKindlingStart) -> {
                    seen.set(onKindlingStart);
                    return MOBI;
                });

        // When
        unit.run(PREFS, "el", "en", ProgressListener.NOOP, callback);

        // Then
        assertThat(seen.get()).isSameAs(callback);
    }

    @Test
    void should_wire_real_collaborators_when_constructed_with_defaults() {
        // The production constructor must at least be usable; its collaborators are exercised
        // end-to-end by the CLI tests rather than mocked here.
        assertThat(new DictionaryPipeline()).isNotNull();
    }
}
