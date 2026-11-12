package edu.self.w2k.gui;

import java.util.List;
import java.util.Optional;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
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

    static final String CUPERTINO_STYLESHEET = "/edu/self/w2k/gui/app-cupertino.css";

    static final String CUPERTINO_DARK_STYLESHEET = "/edu/self/w2k/gui/app-cupertino-dark.css";

    private static final List<String> THEME_STYLESHEETS =
            List.of(STYLESHEET, CUPERTINO_STYLESHEET, CUPERTINO_DARK_STYLESHEET);

    private SystemTheme() {
    }

    /**
     * Applies {@code choice} and keeps following it, so picking a theme in Preferences restyles the
     * open window. Under an AtlantaFX theme the window also follows the system light/dark setting live.
     */
    public static void install(Scene scene, ObservableValue<AppTheme> choice) {
        apply(scene, choice.getValue());
        choice.addListener((_, _, updated) -> apply(scene, updated));
        systemPreferences().ifPresent(preferences -> preferences.colorSchemeProperty()
                                                                .addListener((_, _, _) -> apply(scene, choice.getValue())));
    }

    public static void apply(Scene scene, AppTheme choice) {
        ColorScheme scheme = systemColorScheme();
        if (choice == AppTheme.JAVAFX) {
            Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        }
        else {
            Theme theme = themeFor(choice, scheme);
            Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
            log.debug("Applied the {} theme", theme.getName());
        }
        useStylesheets(scene, stylesheetsFor(choice, scheme));
    }

    /** The app's own sheets that go with a theme, in the order they must follow app.css. */
    static List<String> stylesheetsFor(AppTheme choice, ColorScheme scheme) {
        return switch (choice) {
            case JAVAFX -> List.of();
            case PRIMER -> List.of(STYLESHEET);
            case CUPERTINO -> scheme == ColorScheme.DARK
                    ? List.of(STYLESHEET, CUPERTINO_STYLESHEET, CUPERTINO_DARK_STYLESHEET)
                    : List.of(STYLESHEET, CUPERTINO_STYLESHEET);
        };
    }

    private static void useStylesheets(Scene scene, List<String> wanted) {
        List<String> urls = wanted.stream().map(SystemTheme::url).toList();
        List<String> known = THEME_STYLESHEETS.stream().map(SystemTheme::url).toList();
        scene.getStylesheets().removeIf(sheet -> known.contains(sheet) && !urls.contains(sheet));
        urls.stream().filter(url -> !scene.getStylesheets().contains(url)).forEach(scene.getStylesheets()::add);
    }

    static String url(String stylesheet) {
        return SystemTheme.class.getResource(stylesheet).toExternalForm();
    }

    static Theme themeFor(AppTheme choice, ColorScheme scheme) {
        boolean dark = scheme == ColorScheme.DARK;
        if (choice == AppTheme.PRIMER) {
            return dark ? new PrimerDark() : new PrimerLight();
        }
        return dark ? new CupertinoDark() : new CupertinoLight();
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
