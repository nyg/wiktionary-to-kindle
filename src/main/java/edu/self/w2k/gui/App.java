package edu.self.w2k.gui;

import java.io.IOException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import edu.self.w2k.config.AppInfo;
import edu.self.w2k.config.AppPaths;
import edu.self.w2k.config.AppVersion;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

@Slf4j
public class App extends Application {

    static final String MAIN_FXML = "/edu/self/w2k/gui/main.fxml";
    static final String STYLESHEET = "/edu/self/w2k/gui/app.css";
    static final String ICON = "/edu/self/w2k/gui/icon.png";

    private UiLogAppender uiAppender;

    @Override
    public void start(Stage stage) throws IOException {
        uiAppender = installLogAppenders();

        FXMLLoader loader = new FXMLLoader(App.class.getResource(MAIN_FXML));
        loader.setControllerFactory(_ -> new MainController(uiAppender));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(App.class.getResource(STYLESHEET).toExternalForm());
        SystemTheme.install(scene, ((MainController) loader.getController()).themeChoice());

        stage.setTitle(AppInfo.DISPLAY_NAME + " " + AppVersion.get());
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(560);
        loadIcon().ifPresent(stage.getIcons()::add);
        stage.show();

        // Logged after the appenders are installed, so both the console pane and the log file always
        // open with the environment details a bug report needs.
        log.info("{} {} started — Java {}, {} {}, max heap {} MB",
                 AppInfo.SLUG,
                 AppVersion.get(),
                 Runtime.version(),
                 System.getProperty("os.name"),
                 System.getProperty("os.arch"),
                 Runtime.getRuntime().maxMemory() / (1024 * 1024));
    }

    @Override
    public void stop() {
        if (uiAppender != null) {
            uiAppender.stop();
        }
    }

    /**
     * Attaches the GUI appenders programmatically rather than declaring them in {@code logback.xml},
     * so the CLI's configuration is untouched and its console output stays exactly as it was.
     */
    private static UiLogAppender installLogAppenders() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        UiLogAppender appender = new UiLogAppender();
        appender.setContext(context);
        appender.start();
        root.addAppender(appender);

        addFileAppender(context, root);
        return appender;
    }

    /** A log file gives users something concrete to attach to a bug report. */
    private static void addFileAppender(LoggerContext context, ch.qos.logback.classic.Logger root) {
        try {
            LogFile.install(context, root, AppPaths.stateDir().resolve("logs"));
            root.setLevel(Level.INFO);
        }
        catch (IOException e) {
            // Not fatal: the console pane still works, so the app must not refuse to start.
            root.warn("Could not open log file: {}", e.getLocalizedMessage());
        }
    }

    private java.util.Optional<Image> loadIcon() {
        var stream = App.class.getResourceAsStream(ICON);
        return stream == null ? java.util.Optional.empty() : java.util.Optional.of(new Image(stream));
    }
}
