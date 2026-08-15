package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import atlantafx.base.theme.CupertinoLight;
import edu.self.w2k.dump.DumpFile;
import edu.self.w2k.kaikki.KaikkiCatalog;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
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

        // A test that shows a window would otherwise take the whole toolkit down with it when it
        // closes that window, and every later Platform.runLater would silently never run.
        if (toolkitReady) {
            Platform.setImplicitExit(false);
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
        assertThat(editionCombo.isEditable())
                .as("edition box must accept typing so its list can be filtered")
                .isTrue();
        assertThat(wordCombo.isEditable())
                .as("word language box must accept typing so its list can be filtered")
                .isTrue();
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

    @Test
    void should_narrow_the_list_as_the_user_types_into_a_picker() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());
        ComboBox<?> editionCombo = readField(controller, "editionCombo", ComboBox.class);
        int all = editionCombo.getItems().size();

        onFxThread(() -> editionCombo.getEditor().setText("gr"));

        assertThat(editionCombo.getItems())
                .as("\"gr\" should leave only Greek")
                .hasSizeLessThan(all)
                .allSatisfy(item -> assertThat(item).hasToString("Greek (el)"));
        assertThat(editionCombo.getValue())
                .as("a partial name identifies nothing yet")
                .isNull();

        onFxThread(() -> editionCombo.getEditor().setText("el"));

        assertThat(editionCombo.getValue())
                .as("an exact code should select the language it names")
                .hasToString("Greek (el)");
    }

    @Test
    void should_empty_the_list_and_select_nothing_when_the_text_matches_no_language() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());
        ComboBox<?> editionCombo = readField(controller, "editionCombo", ComboBox.class);

        onFxThread(() -> editionCombo.getEditor().setText("el"));
        assertThat(editionCombo.getValue()).isNotNull();

        onFxThread(() -> editionCombo.getEditor().setText("nds"));

        assertThat(editionCombo.getItems())
                .as("an edition kaikki does not serve must offer nothing to pick")
                .isEmpty();
        assertThat(editionCombo.getValue())
                .as("and must not survive as a value, or it would reach the downloader")
                .isNull();
    }

    @Test
    void should_restore_the_selected_language_when_escape_is_pressed() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());
        ComboBox<?> editionCombo = readField(controller, "editionCombo", ComboBox.class);
        int all = editionCombo.getItems().size();

        onFxThread(() -> editionCombo.getEditor().setText("el"));
        onFxThread(() -> Event.fireEvent(editionCombo, new KeyEvent(KeyEvent.KEY_PRESSED, "", "",
                                                                    KeyCode.ESCAPE, false, false, false, false)));

        assertThat(editionCombo.getEditor().getText()).isEqualTo("Greek (el)");
        assertThat(editionCombo.getItems()).hasSize(all);
    }

    @Test
    void should_apply_a_cupertino_theme_on_macos_and_nothing_elsewhere() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        Scene scene = new Scene(new BorderPane());
        onFxThread(() -> SystemTheme.install(scene));

        if (SystemTheme.appliesTo(System.getProperty("os.name"))) {
            assertThat(Application.getUserAgentStylesheet()).contains("cupertino");
            assertThat(scene.getStylesheets()).anyMatch(sheet -> sheet.endsWith("app-atlantafx.css"));
        }
        else {
            assertThat(scene.getStylesheets()).isEmpty();
        }
    }

    /**
     * The row heights in {@code app.css} were measured against Modena, and Cupertino pads its cells
     * more and sizes list rows at {@code 3em}: the drop-down clipped every language name in half, and
     * log lines came out at more than double their height. Both are silent — no error, just a broken
     * window — so the metrics are asserted rather than eyeballed.
     */
    @Test
    void should_keep_list_rows_compact_and_unclipped_under_the_cupertino_theme() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        String previous = Application.getUserAgentStylesheet();
        AtomicReference<double[]> popupSizes = new AtomicReference<>();
        List<double[]> logRows = new ArrayList<>();
        AtomicReference<Stage> stage = new AtomicReference<>();
        AtomicReference<ComboBox<String>> combo = new AtomicReference<>();
        AtomicReference<ListView<String>> logView = new AtomicReference<>();

        try {
            onFxThread(() -> {
                combo.set(new ComboBox<>(FXCollections.observableArrayList("Chinese (zh)", "Czech (cs)")));
                logView.set(new ListView<>(FXCollections.observableArrayList("12:00:00 INFO  - started")));
                logView.get().getStyleClass().add("log-view");

                Scene scene = new Scene(new VBox(combo.get(), logView.get()), 480, 320);
                scene.getStylesheets().add(App.class.getResource(App.STYLESHEET).toExternalForm());
                Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
                scene.getStylesheets().add(App.class.getResource(SystemTheme.STYLESHEET).toExternalForm());

                stage.set(new Stage());
                stage.get().setScene(scene);
                stage.get().show();
                combo.get().show();
            });

            onFxThread(() -> {
                ListView<?> popup =
                        (ListView<?>) ((ComboBoxListViewSkin<?>) combo.get().getSkin()).getPopupContent();
                popup.applyCss();
                popup.layout();
                popupSizes.set(new double[] {popup.getFixedCellSize(), rowHeightNeededBy(popup)});
                collectRows(logView.get(), logRows);
            });

            assertThat(popupSizes.get()[0])
                    .as("a drop-down row shorter than its own text clips the language name")
                    .isGreaterThanOrEqualTo(popupSizes.get()[1]);
            assertThat(logRows)
                    .isNotEmpty()
                    .allSatisfy(row -> assertThat(row[0])
                            .as("log lines are read as a dense stream, not as list items")
                            .isGreaterThanOrEqualTo(row[1])
                            .isLessThanOrEqualTo(20));
        }
        finally {
            onFxThread(() -> {
                if (stage.get() != null) {
                    stage.get().hide();
                }
                Application.setUserAgentStylesheet(previous);
            });
        }
    }

    /**
     * The height one row would need: its own text plus the padding the theme gives it. A cell under a
     * fixed cell size reports that size as its preferred height however badly the text fits, so the
     * text has to be measured directly for the comparison to mean anything.
     */
    private static double rowHeightNeededBy(Parent parent) {
        ListCell<?> cell = parent.lookupAll(".list-cell").stream()
                                 .filter(node -> node instanceof ListCell<?> c && !c.isEmpty())
                                 .map(ListCell.class::cast)
                                 .findFirst()
                                 .orElseThrow(() -> new AssertionError("the drop-down has no rows"));

        Text text = new Text(cell.getText());
        text.setFont(cell.getFont());
        return text.getLayoutBounds().getHeight()
               + cell.getInsets().getTop()
               + cell.getInsets().getBottom();
    }

    /** Collects {@code {height, preferred height}} for every filled cell below {@code parent}. */
    private static void collectRows(Parent parent, List<double[]> rows) {
        parent.lookupAll(".list-cell").stream()
              .filter(node -> node instanceof ListCell<?> cell && !cell.isEmpty())
              .map(ListCell.class::cast)
              .forEach(cell -> rows.add(new double[] {cell.getHeight(), cell.prefHeight(-1)}));
    }

    @Test
    void should_explain_an_empty_dumps_table_when_the_folder_is_absent_or_unreadable() {
        // When / Then
        assertThat(MainController.dumpsPlaceholder(Path.of("/nowhere/w2k/dumps")))
                .contains("created on the first download");
        assertThat(MainController.dumpsPlaceholder(Path.of(System.getProperty("java.io.tmpdir"))))
                .isEqualTo("No dumps downloaded yet.");
    }

    private static void onFxThread(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            }
            finally {
                done.countDown();
            }
        });
        assertThat(done.await(30, TimeUnit.SECONDS)).as("FX action timed out").isTrue();
    }

    /**
     * A catalog that can neither reach kaikki nor read a cache, so loading the window exercises the
     * bundled fallback and the suite stays free of network calls.
     */
    private static KaikkiCatalog offlineCatalog() {
        return new KaikkiCatalog(
                uri -> {
                    throw new IOException("offline");
                },
                Path.of(System.getProperty("java.io.tmpdir"), "w2k-test-cache-absent"),
                Duration.ofDays(7));
    }

    private static MainController loadOnFxThread(AtomicReference<Parent> root) throws Exception {
        AtomicReference<MainController> controller = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getResource(App.MAIN_FXML));
                loader.setControllerFactory(_ -> new MainController(new UiLogAppender(), offlineCatalog()));
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
