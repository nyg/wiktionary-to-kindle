package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
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
        assertThat(root.get()).isNotNull();

        // A null field here means an fx:id in the FXML no longer matches the controller.
        assertThat(controller.get()).isNotNull();
        assertFieldsInjected(controller.get());
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

        AtomicReference<MainController> controller = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getResource(App.MAIN_FXML));
                loader.setControllerFactory(_ -> new MainController(new UiLogAppender()));
                loader.load();
                controller.set(loader.getController());
            }
            catch (Throwable t) {
                failure.set(t);
            }
            finally {
                done.countDown();
            }
        });

        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(failure.get()).isNull();

        var editionCombo = readField(controller.get(), "editionCombo", ComboBox.class);
        var wordCombo = readField(controller.get(), "wordLanguageCombo", ComboBox.class);
        var dumpsTable = readField(controller.get(), "dumpsTable", TableView.class);

        assertThat(editionCombo.getItems()).isNotEmpty();
        assertThat(editionCombo.isEditable()).as("edition box must accept a hand-typed code").isTrue();
        assertThat(wordCombo.getItems()).hasSizeGreaterThan(150);
        assertThat(dumpsTable.getColumns()).hasSize(3);
    }

    private static <T> T readField(MainController controller, String name, Class<T> type)
            throws Exception {
        var field = MainController.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(controller));
    }
}
