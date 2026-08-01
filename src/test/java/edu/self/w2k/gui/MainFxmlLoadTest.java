package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import edu.self.w2k.dump.DumpFile;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Loads {@code main.fxml} for real, because nothing else catches a mistake in it: a misspelled
 * {@code fx:id} or a handler name that no longer exists compiles fine and fails only at runtime, when
 * the window refuses to open.
 * <p>
 * Needs a graphics toolkit, so it self-skips when none is available and runs under {@code xvfb-run}
 * in CI. OpenJFX does not publish Monocle artifacts for this release line, so a truly headless
 * variant is not currently an option.
 */
class MainFxmlLoadTest {

    private static boolean toolkitReady;

    @BeforeAll
    static void startToolkit() {
        try {
            CountDownLatch started = new CountDownLatch(1);
            Platform.startup(started::countDown);
            toolkitReady = started.await(30, TimeUnit.SECONDS);
        }
        catch (IllegalStateException _) {
            // Already started by an earlier test class in the same forked JVM.
            toolkitReady = true;
        }
        catch (UnsupportedOperationException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            toolkitReady = false;
        }
        catch (Error e) {
            // No display, or the native libraries cannot initialise.
            toolkitReady = false;
        }
    }

    @Test
    void should_load_main_fxml_and_inject_every_field() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available (expected outside CI's xvfb and on headless machines)");
        }

        AtomicReference<Parent> root = new AtomicReference<>();
        MainController controller = loadOnFxThread(root);

        assertThat(root.get()).isNotNull();

        // A null field here means an fx:id in the FXML no longer matches the controller.
        assertThat(controller).isNotNull();
        assertFieldsInjected(controller);
    }

    private static void assertFieldsInjected(MainController controller) throws Exception {
        for (var field : MainController.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(javafx.fxml.FXML.class)) {
                field.setAccessible(true);
                assertThat(field.get(controller))
                        .as("@FXML field '%s' was not injected — check its fx:id in main.fxml",
                            field.getName())
                        .isNotNull();
            }
        }
    }

    @Test
    void should_populate_language_pickers_and_dumps_table_on_load() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());

        var editionCombo = readField(controller, "editionCombo", ComboBox.class);
        var wordCombo = readField(controller, "wordLanguageCombo", ComboBox.class);
        var dumpsTable = readField(controller, "dumpsTable", TableView.class);

        assertThat(editionCombo.getItems()).isNotEmpty();
        assertThat(editionCombo.isEditable()).as("edition box must accept a hand-typed code").isTrue();
        assertThat(wordCombo.getItems()).hasSizeGreaterThan(150);
        assertThat(dumpsTable.getColumns()).hasSize(3);
    }

    /**
     * Every dumps column must actually produce a value. {@code PropertyValueFactory} used to be wired
     * to the Edition and Generated columns, and it cannot read a record: it looks for the JavaBean
     * names {@code langProperty()}/{@code getLang()}, {@code DumpFile} exposes {@code lang()}, and the
     * mismatch yields a null cell value rather than any error — the columns simply rendered blank.
     */
    @Test
    void should_render_a_value_in_every_dumps_column() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());
        DumpFile dump = new DumpFile(Path.of("raw-wiktextract-data-el-2026-07-25.jsonl.gz"),
                                     "el", LocalDate.of(2026, 7, 25), 2_800_000_000L);

        assertThat(cellValue(controller, "dumpLangColumn", dump))
                .as("Edition column")
                .isNotNull()
                .asString()
                .contains("el");
        assertThat(cellValue(controller, "dumpDateColumn", dump))
                .as("Generated column")
                .isEqualTo(LocalDate.of(2026, 7, 25));
        assertThat(cellValue(controller, "dumpSizeColumn", dump))
                .as("Size column")
                .isEqualTo("2.6 GB");
    }

    /** Runs a column's cell value factory the way {@code TableView} does when it renders a row. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object cellValue(MainController controller, String columnField, DumpFile dump)
            throws Exception {
        TableView table = readField(controller, "dumpsTable", TableView.class);
        TableColumn column = readField(controller, columnField, TableColumn.class);
        var factory = column.getCellValueFactory();
        assertThat(factory).as("%s has no cell value factory", columnField).isNotNull();

        ObservableValue<?> observable = (ObservableValue<?>)
                factory.call(new TableColumn.CellDataFeatures<>(table, column, dump));
        return observable == null ? null : observable.getValue();
    }

    private static MainController loadOnFxThread(AtomicReference<Parent> root) throws Exception {
        AtomicReference<MainController> controller = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getResource(App.MAIN_FXML));
                loader.setControllerFactory(_ -> new MainController(new UiLogAppender()));
                root.set(loader.load());
                controller.set(loader.getController());
            }
            catch (Throwable t) {
                failure.set(t);
            }
            finally {
                done.countDown();
            }
        });

        assertThat(done.await(30, TimeUnit.SECONDS)).as("FXML load timed out").isTrue();
        assertThat(failure.get()).as("FXML failed to load").isNull();
        return controller.get();
    }

    private static <T> T readField(MainController controller, String name, Class<T> type)
            throws Exception {
        var field = MainController.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(controller));
    }
}
