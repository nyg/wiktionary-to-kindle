package edu.self.w2k.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import edu.self.w2k.config.LanguageCatalog;
import edu.self.w2k.config.LanguageCatalog.Language;
import edu.self.w2k.config.Preferences;
import edu.self.w2k.dump.DumpCatalog;
import edu.self.w2k.dump.DumpFile;
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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    private final MainViewModel viewModel = new MainViewModel();

    private PipelineService pipeline;
    private Timeline logDrain;

    public MainController(UiLogAppender logAppender) {
        this.logAppender = logAppender;
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
        pipeline.setOnCancelled(_ -> viewModel.report(new ProgressSnapshot(0, "Cancelled")));

        startButton.disableProperty().bind(viewModel.startableProperty().not());
        cancelButton.disableProperty().bind(viewModel.runningProperty().not());
        revealButton.disableProperty().bind(viewModel.lastOutputProperty().isNull());

        refreshDumps();
    }

    private void setUpLanguagePickers() {
        editionCombo.setItems(FXCollections.observableArrayList(LanguageCatalog.editions()));
        wordLanguageCombo.setItems(FXCollections.observableArrayList(LanguageCatalog.wordLanguages()));

        // Editable so an edition kaikki added since this build can still be entered by hand.
        editionCombo.setEditable(true);
        editionCombo.setConverter(new LanguageConverter());
        wordLanguageCombo.setConverter(new LanguageConverter());

        viewModel.editionProperty().bind(editionCombo.valueProperty());
        viewModel.wordLanguageProperty().bind(wordLanguageCombo.valueProperty());
        titlePreview.textProperty().bind(viewModel.titleProperty());
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
    }

    private void drainLog() {
        List<String> batch = new ArrayList<>();
        if (logAppender.drainTo(batch, MAX_LINES_PER_DRAIN) > 0) {
            viewModel.appendLog(batch);
            logView.scrollTo(viewModel.getLogLines().size() - 1);
        }
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
    }
}
