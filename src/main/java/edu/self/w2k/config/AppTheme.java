package edu.self.w2k.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Look of the desktop window. Lives in {@code config} rather than {@code gui} because
 * {@link Preferences} carries it and must stay free of JavaFX for the CLI's sake.
 */
public enum AppTheme {

    JAVAFX("javafx", "JavaFX"),
    CUPERTINO("cupertino", "AtlantaFX — Cupertino"),
    PRIMER("primer", "AtlantaFX — Primer");

    private final String key;
    private final String label;

    AppTheme(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    /**
     * Cupertino is a macOS look, so it is only the default there. Elsewhere Primer: flat and lightly
     * rounded, it is much nearer Windows 11 and GNOME than Modena's gradients.
     */
    public static AppTheme defaultFor(String osName) {
        return osName != null && osName.toLowerCase(Locale.ROOT).startsWith("mac") ? CUPERTINO : PRIMER;
    }

    public static AppTheme defaultForThisPlatform() {
        return defaultFor(System.getProperty("os.name"));
    }

    public static Optional<AppTheme> fromKey(String key) {
        return Arrays.stream(values())
                     .filter(theme -> theme.key.equalsIgnoreCase(key))
                     .findFirst();
    }

    @Override
    public String toString() {
        return label;
    }
}
