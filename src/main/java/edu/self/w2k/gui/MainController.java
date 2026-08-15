package edu.self.w2k.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import edu.self.w2k.config.LanguageCatalog;
import edu.self.w2k.config.LanguageCatalog.Language;
import edu.self.w2k.config.Preferences;
import edu.self.w2k.dump.DumpCatalog;
import edu.self.w2k.dump.DumpFile;
import edu.self.w2k.kaikki.KaikkiCatalog;
import edu.self.w2k.kaikki.LanguageCodeResolver;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.VirtualFlow;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

/**
 * Wires the main window to {@link MainViewModel}. Kept deliberately thin — anything worth testing
 * belongs in the view model, which needs no toolkit.
 */
@Slf4j
public class MainController {

    /**
     * How often the console pane pulls buffered log lines. 10 Hz is fast enough to look live and slow
     * enough that a burst of thousands of lines becomes a handful of batched list updates.
     */
    private static final Duration LOG_DRAIN_INTERVAL = Duration.millis(100);

    private static final int MAX_LINES_PER_DRAIN = 500;

    private static final int IDLE_TICKS_BEFORE_PARKING = 20;

    private static final int VISIBLE_ROWS = 12;

    private static final String FILTER_PROMPT = "Type to filter";

    @FXML private ComboBox<Language> editionCombo;
    @FXML private ComboBox<Language> wordLanguageCombo;
    @FXML private Label titlePreview;
    @FXML private Button startButton;
    @FXML private Button cancelButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private ListView<String> logView;
    @FXML private Button revealButton;

    @FXML private TableView<DumpFile> dumpsTable;
    @FXML private TableColumn<DumpFile, String> dumpLangColumn;
    @FXML private TableColumn<DumpFile, LocalDate> dumpDateColumn;
    @FXML private TableColumn<DumpFile, String> dumpSizeColumn;
    @FXML private Button deleteDumpButton;
    @FXML private Label dumpsLocationLabel;

    private final UiLogAppender logAppender;
    private final KaikkiCatalog catalog;
    private final MainViewModel viewModel = new MainViewModel();
    private final AtomicLong wordLanguageRequests = new AtomicLong();
    private final AtomicBoolean wakePending = new AtomicBoolean();

    private PipelineService pipeline;
    private Timeline logDrain;
    private VirtualFlow<?> logFlow;
    private String loadedWordLanguageEdition;
    private int idleTicks;

    public MainController(UiLogAppender logAppender) {
        this(logAppender, new KaikkiCatalog());
    }

    public MainController(UiLogAppender logAppender, KaikkiCatalog catalog) {
        this.logAppender = logAppender;
        this.catalog = catalog;
    }

    @FXML
    void initialize() {
        viewModel.preferencesProperty().set(Preferences.load());

        setUpLanguagePickers();
        setUpProgressBindings();
        setUpLogConsole();
        setUpDumpsTable();

        pipeline = new PipelineService(viewModel, this::onProgress);
        viewModel.runningProperty().bind(pipeline.runningProperty());
        pipeline.setOnSucceeded(_ -> onFinished(pipeline.getValue()));
        pipeline.setOnFailed(_ -> onFailed(pipeline.getException()));
        pipeline.setOnCancelled(_ -> {
            viewModel.report(new ProgressSnapshot(0, "Cancelled"));
            refreshDumps();
        });

        startButton.disableProperty().bind(viewModel.startableProperty().not());
        cancelButton.disableProperty().bind(viewModel.runningProperty().not());
        revealButton.disableProperty().bind(viewModel.lastOutputProperty().isNull());

        refreshDumps();
    }

    private void setUpLanguagePickers() {
        editionCombo.setItems(FXCollections.observableArrayList(LanguageCatalog.editions()));
        wordLanguageCombo.setItems(FXCollections.observableArrayList(LanguageCatalog.wordLanguages()));

        ComboBoxFilter.install(editionCombo);
        ComboBoxFilter.install(wordLanguageCombo);
        editionCombo.setConverter(new LanguageConverter(() -> ComboBoxFilter.sourceOf(editionCombo)));
        wordLanguageCombo.setConverter(
                new WordLanguageConverter(() -> ComboBoxFilter.sourceOf(wordLanguageCombo)));
        editionCombo.setVisibleRowCount(VISIBLE_ROWS);
        wordLanguageCombo.setVisibleRowCount(VISIBLE_ROWS);
        editionCombo.setPromptText(FILTER_PROMPT);
        wordLanguageCombo.setPromptText(FILTER_PROMPT);

        viewModel.editionProperty().bind(editionCombo.valueProperty());
        viewModel.wordLanguageProperty().bind(wordLanguageCombo.valueProperty());
        titlePreview.textProperty().bind(viewModel.titleProperty());

        editionCombo.valueProperty().addListener((_, _, edition) -> loadWordLanguages(edition));
        refreshEditions();
    }

    private void refreshEditions() {
        Thread.ofVirtual().start(() -> {
            List<Language> editions = catalog.editions().stream().map(Language::of).sorted().toList();
            if (editions.isEmpty()) {
                return;
            }
            Platform.runLater(() -> replaceItems(editionCombo, editions));
        });
    }

    private void loadWordLanguages(Language edition) {
        if (edition == null || edition.code().equals(loadedWordLanguageEdition)) {
            return;
        }
        loadedWordLanguageEdition = edition.code();
        String code = edition.code();
        long request = wordLanguageRequests.incrementAndGet();

        Thread.ofVirtual().start(() -> {
            List<Language> scoped = LanguageCodeResolver.toLanguages(code, catalog.languagesFor(code));
            List<Language> items = scoped.isEmpty() ? LanguageCatalog.wordLanguages() : scoped;
            Platform.runLater(() -> {
                if (request == wordLanguageRequests.get()) {
                    replaceItems(wordLanguageCombo, items);
                }
            });
        });
    }

    private static void replaceItems(ComboBox<Language> combo, List<Language> items) {
        Language previous = combo.getValue();
        ComboBoxFilter.sourceOf(combo).setAll(items);
        if (previous != null) {
            LanguageCatalog.find(items, previous.code()).ifPresent(combo::setValue);
        }
    }

    private void setUpProgressBindings() {
        progressBar.progressProperty().bind(Bindings.createDoubleBinding(
                () -> viewModel.progressProperty().get().fraction(), viewModel.progressProperty()));
        statusLabel.textProperty().bind(Bindings.createStringBinding(
                () -> viewModel.progressProperty().get().message(), viewModel.progressProperty()));
    }

    private void setUpLogConsole() {
        logView.setItems(viewModel.getLogLines());
        // A single timer draining in batches, rather than one runLater per log event.
        logDrain = new Timeline(new KeyFrame(LOG_DRAIN_INTERVAL, _ -> drainLog()));
        logDrain.setCycleCount(Animation.INDEFINITE);
        logDrain.play();
        logAppender.setWakeListener(this::wakeLogDrain);
    }

    private void wakeLogDrain() {
        if (wakePending.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                wakePending.set(false);
                idleTicks = 0;
                if (logDrain.getStatus() != Animation.Status.RUNNING) {
                    logDrain.play();
                }
            });
        }
    }

    private void drainLog() {
        List<String> batch = new ArrayList<>();
        if (logAppender.drainTo(batch, MAX_LINES_PER_DRAIN) == 0) {
            if (++idleTicks >= IDLE_TICKS_BEFORE_PARKING && !viewModel.runningProperty().get()) {
                logDrain.stop();
            }
            return;
        }
        idleTicks = 0;
        boolean wasAtTail = isLogAtTail();
        viewModel.appendLog(batch);
        if (wasAtTail) {
            logView.scrollTo(viewModel.getLogLines().size() - 1);
        }
    }

    private boolean isLogAtTail() {
        if (logFlow == null) {
            logFlow = (VirtualFlow<?>) logView.lookup(".virtual-flow");
        }
        if (logFlow == null) {
            return true;
        }
        IndexedCell<?> last = logFlow.getLastVisibleCell();
        return last == null || last.getIndex() >= viewModel.getLogLines().size() - 2;
    }

    /**
     * Note the explicit accessor lambdas: {@code DumpFile} is a record, so {@code PropertyValueFactory}
     * cannot read it. That factory introspects JavaBean names ({@code langProperty()}, then
     * {@code getLang()}), finds neither on a record's {@code lang()} accessor, and yields a null cell
     * value — a blank column, with no failure anywhere the compiler or a load test could see it.
     */
    private void setUpDumpsTable() {
        dumpLangColumn.setCellValueFactory(cell -> Bindings.createStringBinding(
                () -> Language.of(cell.getValue().lang()).toString()));
        dumpDateColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().generated()));
        dumpSizeColumn.setCellValueFactory(cell ->
                Bindings.createStringBinding(() -> ByteSizes.format(cell.getValue().sizeBytes())));
        dumpsTable.setItems(viewModel.getDumps());
        deleteDumpButton.disableProperty()
                .bind(dumpsTable.getSelectionModel().selectedItemProperty().isNull());
        dumpsLocationLabel.setText(viewModel.preferencesProperty().get().dumpsDir().toString());
    }

    /** Called from the worker thread; hops to the FX thread before touching the view model. */
    private void onProgress(edu.self.w2k.progress.ProgressListener.Stage stage, long done, long total) {
        ProgressSnapshot snapshot = ProgressSnapshot.of(stage, done, total);
        Platform.runLater(() -> viewModel.report(snapshot));
    }

    @FXML
    void onStart() {
        viewModel.lastOutputProperty().set(null);
        viewModel.clearLog();
        idleTicks = 0;
        logDrain.play();
        pipeline.reset();
        pipeline.start();
    }

    @FXML
    void onCancel() {
        pipeline.cancel();
    }

    @FXML
    void onReveal() {
        Path output = viewModel.lastOutputProperty().get();
        if (output == null) {
            return;
        }
        // Runs off the FX thread: Desktop calls can block on the platform file manager.
        Thread.ofVirtual().start(() -> {
            try {
                Desktop.getDesktop().open(output.getParent().toFile());
            }
            catch (IOException | UnsupportedOperationException e) {
                log.warn("Could not open {}: {}", output.getParent(), e.getLocalizedMessage());
            }
        });
    }

    @FXML
    void onRefreshDumps() {
        refreshDumps();
    }

    @FXML
    void onDeleteDump() {
        DumpFile selected = dumpsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                                  "Delete %s (%s)?".formatted(selected.path().getFileName(),
                                                              ByteSizes.format(selected.sizeBytes())));
        confirm.setHeaderText("Delete dump");
        confirm.showAndWait()
                .filter(button -> button == javafx.scene.control.ButtonType.OK)
                .ifPresent(_ -> deleteDump(selected));
    }

    private void deleteDump(DumpFile dump) {
        try {
            catalog().delete(dump);
            refreshDumps();
        }
        catch (IOException e) {
            log.error("Could not delete {}: {}", dump.path(), e.getLocalizedMessage());
            new Alert(Alert.AlertType.ERROR, "Could not delete the dump: " + e.getLocalizedMessage())
                    .showAndWait();
        }
    }

    private void refreshDumps() {
        viewModel.getDumps().setAll(catalog().list());
        dumpsTable.setPlaceholder(new Label(dumpsPlaceholder(viewModel.preferencesProperty().get().dumpsDir())));
    }

    static String dumpsPlaceholder(Path dumpsDir) {
        if (Files.notExists(dumpsDir)) {
            return "No dumps yet. The folder is created on the first download:%n%s".formatted(dumpsDir);
        }
        if (!Files.isReadable(dumpsDir)) {
            return ("%s cannot be read.%nOn macOS, Documents is protected: grant access under System "
                    + "Settings > Privacy & Security > Files and Folders, or choose another folder in "
                    + "Preferences.").formatted(dumpsDir);
        }
        return "No dumps downloaded yet.";
    }

    private DumpCatalog catalog() {
        return new DumpCatalog(viewModel.preferencesProperty().get().dumpsDir());
    }

    @FXML
    void onPreferences() {
        new PreferencesDialog(viewModel.preferencesProperty().get())
                .showAndWait()
                .ifPresent(updated -> {
                    viewModel.preferencesProperty().set(updated);
                    dumpsLocationLabel.setText(updated.dumpsDir().toString());
                    refreshDumps();
                    try {
                        updated.store();
                    }
                    catch (IOException e) {
                        log.error("Could not save preferences: {}", e.getLocalizedMessage());
                    }
                });
    }

    private void onFinished(Path mobi) {
        viewModel.lastOutputProperty().set(mobi);
        viewModel.report(new ProgressSnapshot(1, "Done — " + mobi.getFileName()));
        refreshDumps();
        if (logAppender.droppedCount() > 0) {
            log.warn("{} log lines were dropped; see the log file for the full record",
                     logAppender.droppedCount());
        }
    }

    private void onFailed(Throwable error) {
        log.error("Pipeline failed", error);
        viewModel.report(new ProgressSnapshot(0, "Failed — " + error.getLocalizedMessage()));
        refreshDumps();
    }
}
