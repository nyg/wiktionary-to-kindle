package edu.self.w2k.gui;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import edu.self.w2k.config.Preferences;
import edu.self.w2k.kindling.KindlingRelease;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

/**
 * Modal preferences editor, returning the edited {@link Preferences} on OK.
 * <p>
 * Built in code rather than FXML: it is a four-row form, and a second FXML file plus controller would
 * add wiring to get wrong without making the form any clearer.
 * <p>
 * Max heap is shown read-only. It cannot be a setting — the heap is fixed when the JVM starts, so a
 * value changed here could not take effect, and rewriting jpackage's {@code .cfg} to persist one
 * would invalidate the macOS ad-hoc signature. The bundle ships {@code -XX:MaxRAMPercentage=75}
 * instead, which scales with the machine.
 */
public class PreferencesDialog extends Dialog<Preferences> {

    /** Below this, the largest editions are at real risk of exhausting the heap. */
    static final long LOW_HEAP_THRESHOLD_MB = 4096;

    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final TextField dumpsDir = new TextField();
    private final TextField dictionariesDir = new TextField();
    private final TextField kindlingCliPath = new TextField();
    private final TextField kindlingVersion = new TextField();

    public PreferencesDialog(Preferences current) {
        setTitle("Preferences");
        setHeaderText("Where files are kept, and which kindling-cli to use");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dumpsDir.setText(current.dumpsDir().toString());
        dictionariesDir.setText(current.dictionariesDir().toString());
        kindlingCliPath.setText(current.kindlingCliPath().map(Path::toString).orElse(""));
        kindlingCliPath.setPromptText("Leave empty to use PATH, cache, or download");
        kindlingVersion.setText(current.kindlingVersion().orElse(""));
        kindlingVersion.setPromptText(KindlingRelease.load().version() + " (pinned default)");

        getDialogPane().setContent(buildForm());
        setResultConverter(button -> button == ButtonType.OK ? toPreferences() : null);
    }

    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        grid.addRow(0, new Label("Dumps folder"), withDirectoryChooser(dumpsDir, "Choose dumps folder"));
        grid.addRow(1, new Label("Dictionaries folder"),
                    withDirectoryChooser(dictionariesDir, "Choose dictionaries folder"));
        grid.addRow(2, new Label("kindling-cli binary"), withFileChooser(kindlingCliPath));
        grid.addRow(3, new Label("kindling version"), kindlingVersion);
        grid.add(memoryLabel(), 1, 4);

        GridPane.setHgrow(dumpsDir, Priority.ALWAYS);
        return grid;
    }

    private HBox withDirectoryChooser(TextField field, String chooserTitle) {
        Button browse = new Button("Browse…");
        browse.setOnAction(_ -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(chooserTitle);
            initialDirectory(field.getText()).ifPresent(chooser::setInitialDirectory);
            File chosen = chooser.showDialog(getOwner());
            if (chosen != null) {
                field.setText(chosen.getAbsolutePath());
            }
        });
        return row(field, browse);
    }

    private HBox withFileChooser(TextField field) {
        Button browse = new Button("Browse…");
        browse.setOnAction(_ -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose kindling-cli binary");
            File chosen = chooser.showOpenDialog(getOwner());
            if (chosen != null) {
                field.setText(chosen.getAbsolutePath());
            }
        });
        return row(field, browse);
    }

    private static HBox row(TextField field, Button browse) {
        field.setPrefColumnCount(28);
        HBox box = new HBox(6, field, browse);
        HBox.setHgrow(field, Priority.ALWAYS);
        return box;
    }

    private static Optional<File> initialDirectory(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        File dir = new File(text);
        return dir.isDirectory() ? Optional.of(dir) : Optional.empty();
    }

    private static Label memoryLabel() {
        long maxHeapMb = Runtime.getRuntime().maxMemory() / BYTES_PER_MB;
        Label label = new Label(memoryText(maxHeapMb));
        label.setWrapText(true);
        if (maxHeapMb < LOW_HEAP_THRESHOLD_MB) {
            label.getStyleClass().add("warning-label");
        }
        return label;
    }

    static String memoryText(long maxHeapMb) {
        String base = "Max heap: %d MB (set at startup, not adjustable here)".formatted(maxHeapMb);
        if (maxHeapMb < LOW_HEAP_THRESHOLD_MB) {
            return base + " — the largest editions, such as English, may run out of memory.";
        }
        return base;
    }

    private Preferences toPreferences() {
        return new Preferences(Path.of(dumpsDir.getText().strip()),
                               Path.of(dictionariesDir.getText().strip()),
                               optionalPath(kindlingCliPath.getText()),
                               optionalText(kindlingVersion.getText()));
    }

    private static Optional<String> optionalText(String text) {
        return Optional.ofNullable(text).map(String::strip).filter(s -> !s.isEmpty());
    }

    private static Optional<Path> optionalPath(String text) {
        return optionalText(text).map(Path::of);
    }
}
