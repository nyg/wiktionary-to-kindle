package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import atlantafx.base.theme.CupertinoLight;
import edu.self.w2k.config.AppTheme;
import edu.self.w2k.config.Preferences;
import edu.self.w2k.dump.DumpFile;
import edu.self.w2k.kaikki.KaikkiCatalog;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        catch (Error _) {
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
    void should_keep_the_word_language_picker_shut_until_an_edition_is_chosen() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());
        ComboBox<?> editionCombo = readField(controller, "editionCombo", ComboBox.class);
        ComboBox<?> wordCombo = readField(controller, "wordLanguageCombo", ComboBox.class);

        assertThat(wordCombo.isDisabled())
                .as("no edition is selected on load")
                .isTrue();
        assertThat(wordCombo.getPromptText()).contains("edition");

        onFxThread(() -> editionCombo.getEditor().setText("el"));

        assertThat(editionCombo.getValue()).isNotNull();
        assertThat(wordCombo.isDisabled())
                .as("an edition is selected, so its languages can be picked")
                .isFalse();

        onFxThread(() -> wordCombo.getEditor().setText("en"));
        assertThat(wordCombo.getValue()).isNotNull();

        onFxThread(() -> editionCombo.getEditor().setText(""));

        assertThat(wordCombo.isDisabled()).isTrue();
        assertThat(wordCombo.getValue())
                .as("a language from a list that no longer applies must not survive the edition")
                .isNull();
    }

    @Test
    void should_open_the_drop_down_as_soon_as_a_picker_takes_focus() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());
        ComboBox<?> editionCombo = readField(controller, "editionCombo", ComboBox.class);
        AtomicReference<Stage> stage = new AtomicReference<>();
        Button elsewhere = new Button("elsewhere");

        try {
            onFxThread(() -> {
                stage.set(new Stage());
                stage.get().setScene(new Scene(new VBox(editionCombo, elsewhere), 480, 320));
                stage.get().show();
            });

            onFxThread(() -> {
                elsewhere.requestFocus();
                editionCombo.getEditor().setText("gr");
                editionCombo.hide();
            });
            assertThat(editionCombo.isShowing()).as("test setup: the list starts closed").isFalse();

            onFxThread(() -> editionCombo.getEditor().requestFocus());

            assertThat(editionCombo.isShowing())
                    .as("the list should be offered before the user types anything")
                    .isTrue();
            assertThat(editionCombo.getItems())
                    .as("focus offers the whole list, not what a previous search narrowed it to")
                    .hasSizeGreaterThan(1);
        }
        finally {
            onFxThread(() -> {
                editionCombo.hide();
                if (stage.get() != null) {
                    stage.get().hide();
                }
            });
        }
    }

    /** The arrow toggles the popup itself, so focus must not open it underneath and cancel the toggle. */
    @Test
    void should_leave_the_drop_down_to_the_arrow_when_the_arrow_is_what_was_pressed() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());
        ComboBox<?> editionCombo = readField(controller, "editionCombo", ComboBox.class);
        AtomicReference<Stage> stage = new AtomicReference<>();
        Button elsewhere = new Button("elsewhere");

        try {
            onFxThread(() -> {
                stage.set(new Stage());
                stage.get().setScene(new Scene(new VBox(editionCombo, elsewhere), 480, 320));
                stage.get().show();
                editionCombo.applyCss();
                editionCombo.layout();
            });

            AtomicReference<Node> arrow = new AtomicReference<>();
            onFxThread(() -> arrow.set(editionCombo.lookup(".arrow-button")));
            assertThat(arrow.get()).as("the picker has no arrow button").isNotNull();

            onFxThread(() -> {
                Event.fireEvent(arrow.get(), mouse(arrow.get(), MouseEvent.MOUSE_PRESSED));
                editionCombo.getEditor().requestFocus();
            });

            assertThat(editionCombo.isShowing())
                    .as("focus must stand aside so the arrow's own toggle decides")
                    .isFalse();

            onFxThread(() -> {
                Event.fireEvent(arrow.get(), mouse(arrow.get(), MouseEvent.MOUSE_RELEASED));
                elsewhere.requestFocus();
                Event.fireEvent(editionCombo.getEditor(),
                                mouse(editionCombo.getEditor(), MouseEvent.MOUSE_PRESSED));
                editionCombo.getEditor().requestFocus();
            });

            assertThat(editionCombo.isShowing())
                    .as("a press on the editor is not the arrow, so focus opens the list")
                    .isTrue();
        }
        finally {
            onFxThread(() -> {
                editionCombo.hide();
                if (stage.get() != null) {
                    stage.get().hide();
                }
            });
        }
    }

    private static MouseEvent mouse(Node target, EventType<MouseEvent> type) {
        return new MouseEvent(null, target, type, 1, 1, 1, 1, MouseButton.PRIMARY, 1,
                              false, false, false, false, true, false, false, true, false, false, null);
    }

    @Test
    void should_scroll_the_drop_down_from_anywhere_over_the_popup() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());
        ComboBox<?> editionCombo = readField(controller, "editionCombo", ComboBox.class);
        AtomicReference<ListView<?>> popup = new AtomicReference<>();
        AtomicReference<VirtualFlow<?>> flow = new AtomicReference<>();
        AtomicReference<Stage> stage = new AtomicReference<>();

        try {
            onFxThread(() -> {
                stage.set(new Stage());
                stage.get().setScene(new Scene(new VBox(editionCombo), 480, 320));
                stage.get().show();
                editionCombo.show();
            });
            onFxThread(() -> {
                popup.set((ListView<?>)
                        ((ComboBoxListViewSkin<?>) editionCombo.getSkin()).getPopupContent());
                popup.get().applyCss();
                popup.get().layout();
                flow.set((VirtualFlow<?>) popup.get().lookup(".virtual-flow"));
            });

            assertThat(flow.get()).as("the drop-down has no virtual flow").isNotNull();
            int before = firstVisibleIndex(flow.get());

            onFxThread(() -> Event.fireEvent(popup.get(), new ScrollEvent(
                    ScrollEvent.SCROLL, 5, 5, 5, 5, false, false, false, false, false, false,
                    0, -120, 0, -120, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null)));

            int after = firstVisibleIndex(flow.get());
            assertThat(after)
                    .as("a scroll over the popup must move the list, not fall through it")
                    .isGreaterThan(before);

            onFxThread(() -> Event.fireEvent(popup.get(), new ScrollEvent(
                    ScrollEvent.SCROLL, 5, 5, 5, 5, false, false, false, false, false, false,
                    0, 0, 0, 0, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null)));

            assertThat(firstVisibleIndex(flow.get()))
                    .as("a scroll carrying no vertical movement must leave the list alone")
                    .isEqualTo(after);
        }
        finally {
            onFxThread(() -> {
                editionCombo.hide();
                if (stage.get() != null) {
                    stage.get().hide();
                }
            });
        }
    }

    private static int firstVisibleIndex(VirtualFlow<?> flow) throws Exception {
        AtomicReference<Integer> index = new AtomicReference<>(-1);
        onFxThread(() -> {
            IndexedCell<?> first = flow.getFirstVisibleCell();
            index.set(first == null ? -1 : first.getIndex());
        });
        return index.get();
    }

    @ParameterizedTest
    @CsvSource({
            "MACOS, macos, 'Wiktionary edition:', 'Show in Finder|Cancel|Build Dictionary', false, false",
            "WINDOWS, windows, 'Wiktionary edition', 'Open file location|Build dictionary|Cancel', true, false",
            "GNOME, gnome, 'Wiktionary edition', 'Show in Files|Cancel|Build Dictionary', false, true",
            "KDE, kde, 'Wiktionary edition:', 'Open Containing Folder|Build Dictionary|Cancel', true, false",
    })
    void should_follow_the_desktop_conventions_when_the_window_is_built_for_it(DesktopPlatform platform,
                                                                               String styleClass,
                                                                               String firstLabel,
                                                                               String footerButtons,
                                                                               boolean menuBarInWindow,
                                                                               boolean primaryMenu)
            throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        AtomicReference<Parent> root = new AtomicReference<>();
        MainController controller = loadOnFxThread(root, platform);
        Button startButton = readField(controller, "startButton", Button.class);
        MenuBar menuBar = readField(controller, "menuBar", MenuBar.class);
        MenuButton primaryMenuButton = readField(controller, "primaryMenuButton", MenuButton.class);

        assertThat(root.get().getStyleClass()).contains(styleClass);
        assertThat(root.get().lookupAll(".form-label")).first()
                .extracting(label -> ((Label) label).getText())
                .isEqualTo(firstLabel);
        assertThat(((HBox) startButton.getParent()).getChildren().stream()
                           .filter(Button.class::isInstance)
                           .map(button -> ((Button) button).getText()))
                .as("the footer's buttons, in order")
                .containsExactly(footerButtons.split("\\|"));
        assertThat(menuBar.isManaged()).as("menu bar laid out in the window").isEqualTo(menuBarInWindow);
        assertThat(primaryMenuButton.isVisible()).as("primary menu button shown").isEqualTo(primaryMenu);
        assertThat(primaryMenuButton.getItems().isEmpty()).isEqualTo(!primaryMenu);
    }

    @ParameterizedTest
    @CsvSource({
            "MACOS, Shortcut+Comma",
            "GNOME, Shortcut+Q",
    })
    void should_answer_the_shortcut_on_the_scene_when_the_desktop_menus_cannot(DesktopPlatform platform,
                                                                               String shortcut)
            throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        AtomicReference<Parent> root = new AtomicReference<>();
        MainController controller = loadOnFxThread(root, platform);
        AtomicReference<Scene> scene = new AtomicReference<>();

        onFxThread(() -> {
            scene.set(new Scene(root.get()));
            controller.attachTo(new Stage());
        });

        assertThat(scene.get().getAccelerators()).containsKey(KeyCombination.keyCombination(shortcut));
    }

    @Test
    void should_title_the_macos_header_with_the_app_name_alone_when_attached_to_a_stage() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        AtomicReference<Parent> root = new AtomicReference<>();
        MainController controller = loadOnFxThread(root, DesktopPlatform.MACOS);
        Label windowTitle = readField(controller, "windowTitle", Label.class);

        onFxThread(() -> {
            new Scene(root.get());
            controller.attachTo(new Stage());
        });

        assertThat(windowTitle.isVisible()).isTrue();
        assertThat(windowTitle.getText()).isEqualTo("Wiktionary to Kindle");
    }

    @ParameterizedTest
    @CsvSource({"MACOS", "GNOME"})
    void should_drag_the_window_from_anywhere_in_the_header_but_its_buttons_when_it_is_the_title_bar(
            DesktopPlatform platform) throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        AtomicReference<Parent> root = new AtomicReference<>();
        MainController controller = loadOnFxThread(root, platform);
        HeaderBar headerBar = readField(controller, "headerBar", HeaderBar.class);
        Node switcher = readField(controller, "pageSwitcher", Node.class);
        Node buildPageButton = readField(controller, "buildPageButton", Node.class);
        Node dumpsPageButton = readField(controller, "dumpsPageButton", Node.class);
        AtomicReference<Scene> scene = new AtomicReference<>();
        AtomicReference<Stage> stage = new AtomicReference<>();
        CountDownLatch laidOut = new CountDownLatch(1);

        onFxThread(() -> {
            scene.set(new Scene(root.get(), 680, 400));
            scene.get().getStylesheets().add(App.class.getResource(App.STYLESHEET).toExternalForm());
            scene.get().addPostLayoutPulseListener(laidOut::countDown);
            stage.set(new Stage());
            controller.attachTo(stage.get());
            stage.get().setScene(scene.get());
            stage.get().setX(-4000);
            stage.get().show();
            Platform.requestNextPulse();
        });
        assertThat(laidOut.await(30, TimeUnit.SECONDS)).as("layout pulse timed out").isTrue();

        Bounds bar = headerBar.localToScene(headerBar.getLayoutBounds());
        Bounds slot = switcher.localToScene(switcher.getLayoutBounds());
        Bounds first = buildPageButton.localToScene(buildPageButton.getLayoutBounds());
        Bounds last = dumpsPageButton.localToScene(dumpsPageButton.getLayoutBounds());
        double middle = bar.getCenterY();
        assertThat(headerArea(scene.get(), (bar.getMinX() + slot.getMinX()) / 2, middle)).as("left of the switcher")
                .isEqualTo("DRAGBAR");
        assertThat(headerArea(scene.get(), (slot.getMinX() + first.getMinX()) / 2, middle))
                .as("between the switcher's edge and its first button").isEqualTo("DRAGBAR");
        assertThat(headerArea(scene.get(), (last.getMaxX() + slot.getMaxX()) / 2, middle))
                .as("between the switcher's last button and its edge").isEqualTo("DRAGBAR");
        assertThat(headerArea(scene.get(), first.getCenterX(), first.getCenterY())).as("a switcher button")
                .isNotEqualTo("DRAGBAR");
        onFxThread(() -> stage.get().hide());
    }

    @Test
    void should_show_one_page_at_a_time_and_never_none_when_a_segment_is_clicked() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());
        ToggleButton dumpsPageButton = readField(controller, "dumpsPageButton", ToggleButton.class);
        Node buildPage = readField(controller, "buildPage", Node.class);
        Node dumpsPage = readField(controller, "dumpsPage", Node.class);

        assertThat(buildPage.isVisible()).as("the window opens on the Build page").isTrue();
        assertThat(dumpsPage.isVisible()).isFalse();

        onFxThread(dumpsPageButton::fire);

        assertThat(buildPage.isVisible()).isFalse();
        assertThat(dumpsPage.isVisible()).isTrue();

        onFxThread(dumpsPageButton::fire);

        assertThat(dumpsPageButton.isSelected())
                .as("clicking the selected segment again must not leave the window blank")
                .isTrue();
        assertThat(dumpsPage.isVisible()).isTrue();
    }

    @Test
    void should_keep_the_log_and_the_status_out_of_the_way_when_the_window_opens() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());
        ToggleButton logToggle = readField(controller, "logToggle", ToggleButton.class);
        ListView<String> logView = readLogView(controller);
        Node progressBox = readField(controller, "progressBox", Node.class);

        assertThat(progressBox.isVisible()).as("an idle window shows no progress and no \"Ready\"").isFalse();
        assertThat(logView.isVisible()).isFalse();
        assertThat(logView.isManaged()).as("a closed log must take no space").isFalse();

        onFxThread(logToggle::fire);

        assertThat(logView.isVisible()).isTrue();
        assertThat(logView.isManaged()).isTrue();
    }

    @Test
    void should_copy_the_selected_log_lines() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());
        ListView<String> logView = readLogView(controller);

        assertThat(logView.getSelectionModel().getSelectionMode())
                .as("a single line is rarely the whole story")
                .isEqualTo(SelectionMode.MULTIPLE);
        assertThat(logView.getContextMenu()).isNotNull();
        assertThat(logView.getContextMenu().getItems()).extracting(MenuItem::getText)
                .containsExactly("Copy", "Copy all");

        AtomicReference<String> copied = new AtomicReference<>();
        onFxThread(() -> {
            logView.getItems().setAll("first line", "second line", "third line");
            logView.getSelectionModel().clearSelection();
            logView.getSelectionModel().selectIndices(0, 2);
            Event.fireEvent(logView, copyShortcut());
            copied.set(Clipboard.getSystemClipboard().getString());
        });

        assertThat(copied.get())
                .as("the shortcut copies the selection, skipping the line between")
                .isEqualTo("first line" + System.lineSeparator() + "third line");

        AtomicReference<String> everything = new AtomicReference<>();
        onFxThread(() -> {
            logView.getContextMenu().getItems().getLast().fire();
            everything.set(Clipboard.getSystemClipboard().getString());
        });

        assertThat(everything.get())
                .as("Copy all takes the pane, not the selection")
                .isEqualTo(String.join(System.lineSeparator(),
                                       "first line", "second line", "third line"));

        AtomicReference<String> afterEmpty = new AtomicReference<>();
        onFxThread(() -> {
            logView.getSelectionModel().clearSelection();
            LogClipboard.copySelectionOf(logView);
            afterEmpty.set(Clipboard.getSystemClipboard().getString());
        });

        assertThat(afterEmpty.get())
                .as("copying nothing must leave what is already on the clipboard")
                .isEqualTo(everything.get());
    }

    private static KeyEvent copyShortcut() {
        boolean mac = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
        return new KeyEvent(KeyEvent.KEY_PRESSED, "c", "c", KeyCode.C, false, !mac, false, mac);
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

    /**
     * Switching away from Cupertino has to take {@code app-atlantafx.css} off the scene as well as
     * restore Modena: its rules are written against AtlantaFX's colour tokens, which resolve to
     * nothing under Modena, and the log view's selection would go unpainted.
     */
    @Test
    void should_follow_the_theme_chosen_in_preferences() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        String previous = Application.getUserAgentStylesheet();
        try {
            Scene scene = new Scene(new BorderPane());
            ObjectProperty<AppTheme> choice = new SimpleObjectProperty<>(AppTheme.CUPERTINO);
            onFxThread(() -> SystemTheme.install(scene, choice));

            assertThat(Application.getUserAgentStylesheet()).contains("cupertino");
            assertThat(scene.getStylesheets()).anyMatch(sheet -> sheet.endsWith("app-atlantafx.css"));

            onFxThread(() -> choice.set(AppTheme.JAVAFX));

            assertThat(Application.getUserAgentStylesheet()).isEqualTo(Application.STYLESHEET_MODENA);
            assertThat(scene.getStylesheets()).isEmpty();

            onFxThread(() -> choice.set(AppTheme.CUPERTINO));

            assertThat(Application.getUserAgentStylesheet()).contains("cupertino");
            assertThat(scene.getStylesheets()).hasSize(1);
        }
        finally {
            Application.setUserAgentStylesheet(previous);
        }
    }

    @Test
    void should_start_the_window_on_the_theme_the_saved_preferences_name() throws Exception {
        if (!toolkitReady) {
            abort("No JavaFX toolkit available");
        }

        MainController controller = loadOnFxThread(new AtomicReference<>());

        assertThat(controller.themeChoice().getValue())
                .as("the window must open on the saved theme, not on a hardcoded one")
                .isEqualTo(Preferences.load().theme());
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

    /** JavaFX exposes no public hit test for the draggable header area, so this asks the one glass uses. */
    private static String headerArea(Scene scene, double x, double y) throws Exception {
        Class<?> listenerType = Class.forName("javafx.scene.Scene$ScenePeerListener");
        var constructor = listenerType.getDeclaredConstructor(Scene.class);
        constructor.setAccessible(true);
        Object listener = constructor.newInstance(scene);
        var pick = listenerType.getMethod("pickHeaderArea", double.class, double.class);
        pick.setAccessible(true);
        AtomicReference<Object> area = new AtomicReference<>();
        onFxThread(() -> {
            try {
                area.set(pick.invoke(listener, x, y));
            }
            catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        return String.valueOf(area.get());
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
        return loadOnFxThread(root, DesktopPlatform.current());
    }

    private static MainController loadOnFxThread(AtomicReference<Parent> root, DesktopPlatform platform)
            throws Exception {
        AtomicReference<MainController> controller = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getResource(App.MAIN_FXML));
                loader.setControllerFactory(_ -> new MainController(new UiLogAppender(), offlineCatalog(), platform));
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

    @SuppressWarnings("unchecked")
    private static ListView<String> readLogView(MainController controller) throws Exception {
        return readField(controller, "logView", ListView.class);
    }
}
