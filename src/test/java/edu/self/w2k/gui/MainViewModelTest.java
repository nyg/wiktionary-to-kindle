package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import edu.self.w2k.config.LanguageCatalog.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the view model without an FX toolkit — properties, bindings and observable lists all come
 * from javafx.base, which needs no {@code Platform.startup} and loads no native libraries. This is
 * the reason the logic lives here rather than in the FXML controller.
 */
class MainViewModelTest {

    private MainViewModel unit;

    @BeforeEach
    void setUp() {
        unit = new MainViewModel();
    }

    @Test
    void should_not_be_startable_until_both_languages_are_chosen() {
        // Then
        assertThat(unit.startableProperty().get()).isFalse();

        // When
        unit.editionProperty().set(Language.of("el"));

        // Then
        assertThat(unit.startableProperty().get()).isFalse();

        // When
        unit.wordLanguageProperty().set(Language.of("en"));

        // Then
        assertThat(unit.startableProperty().get()).isTrue();
    }

    @Test
    void should_not_offer_a_word_language_until_an_edition_is_chosen() {
        // Then — the languages on offer are the edition's own, so there is nothing to pick from yet
        assertThat(unit.wordLanguageSelectableProperty().get()).isFalse();

        // When
        unit.editionProperty().set(Language.of("el"));

        // Then
        assertThat(unit.wordLanguageSelectableProperty().get()).isTrue();

        // When
        unit.editionProperty().set(null);

        // Then
        assertThat(unit.wordLanguageSelectableProperty().get()).isFalse();
    }

    @Test
    void should_not_be_startable_while_running() {
        // Given
        unit.editionProperty().set(Language.of("el"));
        unit.wordLanguageProperty().set(Language.of("en"));

        // When
        unit.runningProperty().set(true);

        // Then
        assertThat(unit.startableProperty().get()).isFalse();

        // When
        unit.runningProperty().set(false);

        // Then
        assertThat(unit.startableProperty().get()).isTrue();
    }

    @Test
    void should_derive_title_from_word_language_and_edition() {
        // When
        unit.editionProperty().set(Language.of("el"));
        unit.wordLanguageProperty().set(Language.of("fr"));

        // Then — argument order matches the CLI: word language first, edition second
        assertThat(unit.titleProperty().get()).isEqualTo("W2K French–Greek Dictionary");
    }

    @Test
    void should_leave_title_blank_until_both_languages_are_chosen() {
        // When
        unit.editionProperty().set(Language.of("el"));

        // Then
        assertThat(unit.titleProperty().get()).isEmpty();
    }

    @Test
    void should_update_title_when_a_language_changes() {
        // Given
        unit.editionProperty().set(Language.of("el"));
        unit.wordLanguageProperty().set(Language.of("fr"));

        // When
        unit.wordLanguageProperty().set(Language.of("de"));

        // Then
        assertThat(unit.titleProperty().get()).isEqualTo("W2K German–Greek Dictionary");
    }

    @Test
    void should_append_log_lines_in_order() {
        // When
        unit.appendLog(List.of("one", "two"));
        unit.appendLog(List.of("three"));

        // Then
        assertThat(unit.getLogLines()).containsExactly("one", "two", "three");
    }

    @Test
    void should_ignore_an_empty_batch_when_appending() {
        // Given
        unit.appendLog(List.of("one"));

        // When
        unit.appendLog(List.of());

        // Then
        assertThat(unit.getLogLines()).containsExactly("one");
    }

    @Test
    void should_drop_oldest_lines_when_exceeding_the_cap() {
        // Given a batch larger than the cap in one go
        List<String> flood = IntStream.range(0, MainViewModel.MAX_LOG_LINES + 250)
                .mapToObj(i -> "line " + i)
                .toList();

        // When
        unit.appendLog(flood);

        // Then
        assertThat(unit.getLogLines()).hasSize(MainViewModel.MAX_LOG_LINES);
        assertThat(unit.getLogLines().getFirst()).isEqualTo("line 250");
        assertThat(unit.getLogLines().getLast()).isEqualTo("line " + (flood.size() - 1));
    }

    @Test
    void should_keep_the_cap_across_several_batches() {
        // When
        for (int i = 0; i < 12; i++) {
            int batchStart = i * 500;
            unit.appendLog(IntStream.range(batchStart, batchStart + 500)
                                   .mapToObj(n -> "line " + n)
                                   .toList());
        }

        // Then
        assertThat(unit.getLogLines()).hasSize(MainViewModel.MAX_LOG_LINES);
        assertThat(unit.getLogLines().getLast()).isEqualTo("line 5999");
    }

    @Test
    void should_empty_the_console_when_cleared() {
        // Given
        unit.appendLog(List.of("one", "two"));

        // When
        unit.clearLog();

        // Then
        assertThat(unit.getLogLines()).isEmpty();
    }

    @Test
    void should_start_idle_and_accept_progress_reports() {
        // Then
        assertThat(unit.progressProperty().get()).isEqualTo(ProgressSnapshot.idle());

        // When
        ProgressSnapshot snapshot = new ProgressSnapshot(0.5, "halfway");
        unit.report(snapshot);

        // Then
        assertThat(unit.progressProperty().get()).isEqualTo(snapshot);
    }

    @Test
    void should_default_preferences_so_the_dumps_pane_has_a_location_before_loading() {
        // Then
        assertThat(unit.preferencesProperty().get()).isNotNull();
        assertThat(unit.getDumps()).isEmpty();
        assertThat(unit.lastOutputProperty().get()).isNull();
    }
}
