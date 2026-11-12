package edu.self.w2k.gui;

import java.lang.reflect.Field;

import edu.self.w2k.config.LanguageCatalog;
import edu.self.w2k.config.LanguageCatalog.Language;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Opens the real main window with a sample edition and word language chosen, for the screenshots
 * workflow to capture. Lives in test sources so it never ships. Prints {@link #READY} once the
 * word languages fetched from kaikki have replaced the bundled list, then stays open until killed.
 */
public final class ScreenshotLauncher {

    static final String READY = "SCREENSHOT READY";

    /** Long enough for the edition's language list to arrive from kaikki and the pickers to settle. */
    private static final Duration SETTLE = Duration.seconds(10);

    private ScreenshotLauncher() {
    }

    public static void main(String[] args) {
        String edition = args.length > 0 ? args[0] : "en";
        String wordLanguage = args.length > 1 ? args[1] : "el";
        Platform.startup(() -> {
            try {
                MainController controller = App.showMainWindow(new Stage(), new UiLogAppender());
                choose(controller, "editionCombo", edition);
                choose(controller, "wordLanguageCombo", wordLanguage);
                PauseTransition settle = new PauseTransition(SETTLE);
                settle.setOnFinished(_ -> {
                    field(controller, "root", Node.class).requestFocus();
                    System.out.println(READY);
                });
                settle.play();
            }
            catch (Exception e) {
                e.printStackTrace();
                Platform.exit();
            }
        });
    }

    /** Picks from the list, as the app restores a selection, so the editor shows the language's name. */
    @SuppressWarnings("unchecked")
    private static void choose(MainController controller, String comboField, String code) {
        ComboBox<Language> combo = field(controller, comboField, ComboBox.class);
        LanguageCatalog.find(ComboBoxFilter.sourceOf(combo), code).ifPresent(combo::setValue);
        combo.hide();
    }

    private static <T> T field(MainController controller, String name, Class<T> type) {
        try {
            Field field = MainController.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(controller));
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("MainController has no field " + name, e);
        }
    }
}
