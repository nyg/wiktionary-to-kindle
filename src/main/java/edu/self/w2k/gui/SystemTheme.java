package edu.self.w2k.gui;

import java.util.Locale;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SystemTheme {

    static final String STYLESHEET = "/edu/self/w2k/gui/app-atlantafx.css";

    private SystemTheme() {
    }

    public static void install(Scene scene) {
        if (!appliesTo(System.getProperty("os.name"))) {
            return;
        }

        try {
            Platform.Preferences preferences = Platform.getPreferences();
            apply(scene, preferences.getColorScheme());
            preferences.colorSchemeProperty()
                       .addListener((_, _, scheme) -> apply(scene, scheme));
        }
        catch (RuntimeException e) {
            log.debug("Could not follow the system colour scheme: {}", e.toString());
        }
    }

    static boolean appliesTo(String osName) {
        return osName != null && osName.toLowerCase(Locale.ROOT).startsWith("mac");
    }

    static Theme themeFor(ColorScheme scheme) {
        return scheme == ColorScheme.DARK ? new CupertinoDark() : new CupertinoLight();
    }

    private static void apply(Scene scene, ColorScheme scheme) {
        Theme theme = themeFor(scheme);
        Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());

        String tweaks = SystemTheme.class.getResource(STYLESHEET).toExternalForm();
        if (!scene.getStylesheets().contains(tweaks)) {
            scene.getStylesheets().add(tweaks);
        }

        log.debug("Applied the {} theme", theme.getName());
    }
}
