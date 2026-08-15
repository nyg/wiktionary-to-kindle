package edu.self.w2k.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
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

    private static final String LOG_PATTERN = "%d{HH:mm:ss} %-5level - %msg%n";

    private UiLogAppender uiAppender;

    @Override
    public void start(Stage stage) throws IOException {
        uiAppender = installLogAppenders();

        FXMLLoader loader = new FXMLLoader(App.class.getResource(MAIN_FXML));
        loader.setControllerFactory(_ -> new MainController(uiAppender));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(App.class.getResource(STYLESHEET).toExternalForm());
        SystemTheme.install(scene);

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
            Path logFile = AppPaths.stateDir().resolve("logs").resolve("app.log");
            Files.createDirectories(logFile.getParent());

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            encoder.setPattern(LOG_PATTERN);
            encoder.start();

            FileAppender<ch.qos.logback.classic.spi.ILoggingEvent> fileAppender = new FileAppender<>();
            fileAppender.setContext(context);
            fileAppender.setName("file");
            fileAppender.setFile(logFile.toString());
            fileAppender.setAppend(false);
            fileAppender.setEncoder(encoder);
            fileAppender.start();

            root.addAppender(fileAppender);
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
