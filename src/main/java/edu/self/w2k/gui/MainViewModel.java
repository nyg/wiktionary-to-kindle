package edu.self.w2k.gui;

import java.nio.file.Path;
import java.util.List;

import edu.self.w2k.config.LanguageCatalog.Language;
import edu.self.w2k.config.Preferences;
import edu.self.w2k.dump.DumpFile;
import edu.self.w2k.write.DictionaryTitles;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Observable state behind the main window.
 * <p>
 * All the logic lives here rather than in the FXML controller, which keeps it testable: JavaFX
 * properties and bindings work without an initialised toolkit, so this class can be exercised in a
 * plain unit test while a controller could not.
 */
public class MainViewModel {

    /**
     * Console lines retained. A full run logs far more than this; the pane is for watching progress
     * and diagnosing the tail of a failure, and an unbounded list would grow without limit across a
     * long session. The complete record goes to the log file.
     */
    static final int MAX_LOG_LINES = 5_000;

    private final ObjectProperty<Language> edition = new SimpleObjectProperty<>();
    private final ObjectProperty<Language> wordLanguage = new SimpleObjectProperty<>();
    private final StringProperty title = new SimpleStringProperty("");
    private final ObjectProperty<ProgressSnapshot> progress =
            new SimpleObjectProperty<>(ProgressSnapshot.idle());
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final ObjectProperty<Path> lastOutput = new SimpleObjectProperty<>();
    private final ObjectProperty<Preferences> preferences =
            new SimpleObjectProperty<>(Preferences.defaults());

    private final ObservableList<String> logLines = FXCollections.observableArrayList();
    private final ObservableList<DumpFile> dumps = FXCollections.observableArrayList();

    private final BooleanProperty startable = new SimpleBooleanProperty(false);
    private final BooleanProperty wordLanguageSelectable = new SimpleBooleanProperty(false);

    public MainViewModel() {
        title.bind(Bindings.createStringBinding(this::computeTitle, edition, wordLanguage));
        startable.bind(edition.isNotNull().and(wordLanguage.isNotNull()).and(running.not()));
        wordLanguageSelectable.bind(edition.isNotNull());
    }

    private String computeTitle() {
        Language src = wordLanguage.get();
        Language trg = edition.get();
        if (src == null || trg == null) {
            return "";
        }
        // Argument order matches the CLI: the word language is the source, the edition the target.
        return DictionaryTitles.autoTitle(src.code(), trg.code());
    }

    /** Applies a listener callback, replacing the current progress display. */
    public void report(ProgressSnapshot snapshot) {
        progress.set(snapshot);
    }

    /** Appends console lines, trimming the oldest beyond {@link #MAX_LOG_LINES}. */
    public void appendLog(List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }
        logLines.addAll(lines);
        int excess = logLines.size() - MAX_LOG_LINES;
        if (excess > 0) {
            logLines.remove(0, excess);
        }
    }

    public void clearLog() {
        logLines.clear();
    }

    public ObjectProperty<Language> editionProperty() {
        return edition;
    }

    public ObjectProperty<Language> wordLanguageProperty() {
        return wordLanguage;
    }

    public ReadOnlyStringProperty titleProperty() {
        return title;
    }

    public ObjectProperty<ProgressSnapshot> progressProperty() {
        return progress;
    }

    public BooleanProperty runningProperty() {
        return running;
    }

    public ReadOnlyBooleanProperty startableProperty() {
        return startable;
    }

    public ReadOnlyBooleanProperty wordLanguageSelectableProperty() {
        return wordLanguageSelectable;
    }

    public ObjectProperty<Path> lastOutputProperty() {
        return lastOutput;
    }

    public ObjectProperty<Preferences> preferencesProperty() {
        return preferences;
    }

    public ObservableList<String> getLogLines() {
        return logLines;
    }

    public ObservableList<DumpFile> getDumps() {
        return dumps;
    }
}
