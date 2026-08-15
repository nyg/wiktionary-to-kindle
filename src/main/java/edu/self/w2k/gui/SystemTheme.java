package edu.self.w2k.gui;

import java.util.Optional;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Theme;
import edu.self.w2k.config.AppTheme;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SystemTheme {

    static final String STYLESHEET = "/edu/self/w2k/gui/app-atlantafx.css";

    private SystemTheme() {
    }

    /**
     * Applies {@code choice} and keeps following it, so picking a theme in Preferences restyles the
     * open window. Under Cupertino the window also follows the system light/dark setting live.
     */
    public static void install(Scene scene, ObservableValue<AppTheme> choice) {
        apply(scene, choice.getValue());
        choice.addListener((_, _, updated) -> apply(scene, updated));
        systemPreferences().ifPresent(preferences -> preferences.colorSchemeProperty()
                                                                .addListener((_, _, _) -> apply(scene, choice.getValue())));
    }

    public static void apply(Scene scene, AppTheme choice) {
        String tweaks = SystemTheme.class.getResource(STYLESHEET).toExternalForm();

        if (choice != AppTheme.CUPERTINO) {
            Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
            scene.getStylesheets().remove(tweaks);
            return;
        }

        Theme theme = themeFor(systemColorScheme());
        Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
        if (!scene.getStylesheets().contains(tweaks)) {
            scene.getStylesheets().add(tweaks);
        }
        log.debug("Applied the {} theme", theme.getName());
    }

    static Theme themeFor(ColorScheme scheme) {
        return scheme == ColorScheme.DARK ? new CupertinoDark() : new CupertinoLight();
    }

    private static ColorScheme systemColorScheme() {
        return systemPreferences().map(Platform.Preferences::getColorScheme).orElse(ColorScheme.LIGHT);
    }

    /**
     * Empty when the platform reports nothing, which leaves the window on the light variant rather
     * than on a dialog: a theme is never worth failing a launch over.
     */
    private static Optional<Platform.Preferences> systemPreferences() {
        try {
            return Optional.of(Platform.getPreferences());
        }
        catch (RuntimeException e) {
            log.debug("Could not read the system colour scheme: {}", e.toString());
            return Optional.empty();
        }
    }
}
