package edu.self.w2k.config;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Per-OS locations for the application's own files, following the same conventions as
 * {@link edu.self.w2k.kindling.XdgCachePaths} (which covers the cache only).
 * <p>
 * The GUI needs absolute paths: a bundled {@code .app} is launched with the working directory set to
 * {@code /}, so the CLI's CWD-relative {@code dumps/} and {@code dictionaries/} would resolve to the
 * filesystem root. The CLI keeps its relative defaults; only the GUI consults these.
 */
@Slf4j
public final class AppPaths {

    private static final String APP_DIR_NAME = "wiktionary-to-kindle";

    private AppPaths() {}

    /** Where {@code preferences.properties} and the GUI log file live. */
    public static Path configDir() {
        return configDir(System.getenv(), System.getProperty("os.name", ""));
    }

    static Path configDir(Map<String, String> env, String osName) {
        if (isWindows(osName)) {
            return windowsBase(env).resolve(APP_DIR_NAME).resolve("Config");
        }
        return unixConfigBase(env).resolve(APP_DIR_NAME);
    }

    /**
     * Default parent of {@code dumps/} and {@code dictionaries/}.
     * <p>
     * Deliberately under {@code Documents} rather than a hidden XDG data directory: dumps are
     * multi-gigabyte and the finished {@code .mobi} has to be found and copied to a Kindle, so both
     * need to be somewhere a file manager shows by default. Overridable in preferences.
     */
    public static Path defaultDataDir() {
        return defaultDataDir(System.getenv(), System.getProperty("os.name", ""));
    }

    static Path defaultDataDir(Map<String, String> env, String osName) {
        return homeDir(env, osName).resolve("Documents").resolve(APP_DIR_NAME);
    }

    private static boolean isWindows(String osName) {
        return osName.toLowerCase(Locale.ROOT).contains("windows");
    }

    private static Path unixConfigBase(Map<String, String> env) {
        String xdg = env.get("XDG_CONFIG_HOME");
        if (isSet(xdg)) return Path.of(xdg);
        String home = env.get("HOME");
        if (isSet(home)) return Path.of(home, ".config");
        return tmpFallback("HOME not set");
    }

    private static Path windowsBase(Map<String, String> env) {
        String local = env.get("LOCALAPPDATA");
        if (isSet(local)) return Path.of(local);
        String profile = env.get("USERPROFILE");
        if (isSet(profile)) return Path.of(profile, "AppData", "Local");
        return tmpFallback("LOCALAPPDATA and USERPROFILE not set");
    }

    private static Path homeDir(Map<String, String> env, String osName) {
        String home = env.get(isWindows(osName) ? "USERPROFILE" : "HOME");
        if (isSet(home)) return Path.of(home);
        return tmpFallback("home directory not set");
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private static Path tmpFallback(String reason) {
        String tmp = System.getProperty("java.io.tmpdir");
        log.warn("{}; using java.io.tmpdir ({}) instead", reason, tmp);
        return Path.of(tmp);
    }
}
