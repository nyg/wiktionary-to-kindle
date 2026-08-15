package edu.self.w2k.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

/**
 * Per-OS locations for the application's own files.
 * <p>
 * Unix-likes — macOS included — follow the XDG Base Directory specification. Windows has no
 * equivalent, so the same three roles are given sibling directories under {@code %LOCALAPPDATA%}.
 * <p>
 * The GUI needs absolute paths: a bundled {@code .app} is launched with the working directory set to
 * {@code /}, so the CLI's CWD-relative {@code dumps/} and {@code dictionaries/} would resolve to the
 * filesystem root. The CLI keeps its relative defaults; only the GUI consults these.
 */
@Slf4j
public final class AppPaths {

    /** Read from the environment first, then from {@code user-dirs.dirs}, where it is an assignment. */
    private static final String DOCUMENTS_KEY = "XDG_DOCUMENTS_DIR";

    private AppPaths() {}

    /** Where {@code preferences.properties} lives. */
    public static Path configDir() {
        return configDir(System.getenv(), osName());
    }

    static Path configDir(Map<String, String> env, String osName) {
        if (isWindows(osName)) {
            return windowsBase(env).resolve(AppInfo.SLUG).resolve("Config");
        }
        return unixBase(env, "XDG_CONFIG_HOME", ".config").resolve(AppInfo.SLUG);
    }

    /** Where downloaded {@code kindling-cli} binaries are kept — re-fetchable, so cache and not data. */
    public static Path cacheDir() {
        return cacheDir(System.getenv(), osName());
    }

    static Path cacheDir(Map<String, String> env, String osName) {
        if (isWindows(osName)) {
            return windowsBase(env).resolve(AppInfo.SLUG).resolve("Cache");
        }
        return unixBase(env, "XDG_CACHE_HOME", ".cache").resolve(AppInfo.SLUG);
    }

    /**
     * Where the GUI log file lives. State rather than config: it is written by the app, not edited by
     * the user, and losing it costs nothing.
     */
    public static Path stateDir() {
        return stateDir(System.getenv(), osName());
    }

    static Path stateDir(Map<String, String> env, String osName) {
        if (isWindows(osName)) {
            return windowsBase(env).resolve(AppInfo.SLUG).resolve("State");
        }
        return unixBase(env, "XDG_STATE_HOME", ".local", "state").resolve(AppInfo.SLUG);
    }

    /**
     * Default parent of {@code dumps/} and {@code dictionaries/}.
     * <p>
     * Deliberately under the user's documents directory rather than {@code $XDG_DATA_HOME}: dumps are
     * multi-gigabyte and the finished {@code .mobi} has to be found and copied to a Kindle, so both
     * need to be somewhere a file manager shows by default. That directory is still resolved the XDG
     * way — {@code XDG_DOCUMENTS_DIR} from the environment, then from {@code user-dirs.dirs}, then
     * {@code ~/Documents} — so a localised or relocated Documents folder is honoured. Overridable in
     * preferences.
     */
    public static Path defaultDataDir() {
        return defaultDataDir(System.getenv(), osName());
    }

    static Path defaultDataDir(Map<String, String> env, String osName) {
        return documentsDir(env, osName).resolve(AppInfo.SLUG);
    }

    private static Path documentsDir(Map<String, String> env, String osName) {
        Path home = homeDir(env, osName);
        if (isWindows(osName)) {
            return home.resolve("Documents");
        }
        Optional<Path> fromEnv = absolutePath(DOCUMENTS_KEY, expandHome(env.get(DOCUMENTS_KEY), home));
        return fromEnv.or(() -> readUserDirs(env, home)).orElseGet(() -> home.resolve("Documents"));
    }

    /**
     * Reads {@code XDG_DOCUMENTS_DIR} out of {@code $XDG_CONFIG_HOME/user-dirs.dirs}, the file
     * {@code xdg-user-dirs} generates. Any problem — absent, unreadable, no such key — falls through
     * to the caller's default rather than failing the launch.
     */
    private static Optional<Path> readUserDirs(Map<String, String> env, Path home) {
        Path userDirs = unixBase(env, "XDG_CONFIG_HOME", ".config").resolve("user-dirs.dirs");
        if (!Files.isReadable(userDirs)) {
            return Optional.empty();
        }
        String assignment = DOCUMENTS_KEY + "=";
        try (Stream<String> lines = Files.lines(userDirs, StandardCharsets.UTF_8)) {
            return lines.map(String::strip)
                        .filter(line -> line.startsWith(assignment))
                        .map(line -> unquote(line.substring(assignment.length()).strip()))
                        .filter(AppPaths::isSet)
                        .findFirst()
                        .flatMap(value -> absolutePath(userDirs.toString(), expandHome(value, home)));
        }
        catch (IOException | RuntimeException e) {
            log.warn("Could not read {}: {}", userDirs, e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /** {@code user-dirs.dirs} writes paths as {@code "$HOME/Documents"}; nothing else is expanded. */
    private static String expandHome(String value, Path home) {
        if (!isSet(value)) return value;
        if (value.equals("$HOME")) return home.toString();
        if (value.startsWith("$HOME/")) return home + value.substring("$HOME".length());
        return value;
    }

    private static String osName() {
        return System.getProperty("os.name", "");
    }

    private static boolean isWindows(String osName) {
        return osName.toLowerCase(Locale.ROOT).contains("windows");
    }

    private static Path unixBase(Map<String, String> env, String xdgVar, String... homeFallback) {
        Optional<Path> xdg = absolutePath(xdgVar, env.get(xdgVar));
        if (xdg.isPresent()) return xdg.get();
        Optional<Path> home = absolutePath("HOME", env.get("HOME"));
        if (home.isPresent()) return Path.of(home.get().toString(), homeFallback);
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
        if (isWindows(osName)) {
            String profile = env.get("USERPROFILE");
            return isSet(profile) ? Path.of(profile) : tmpFallback("home directory not set");
        }
        return absolutePath("HOME", env.get("HOME")).orElseGet(() -> tmpFallback("home directory not set"));
    }

    /**
     * The XDG spec requires a relative path in one of its variables to be treated as invalid. Honouring
     * that keeps a misconfigured environment on the documented default instead of resolving against
     * whatever the working directory happens to be — which differs between the bundled app and the CLI.
     * <p>
     * Only the Unix branch uses this. {@link Path#isAbsolute()} answers for the filesystem the JVM is
     * running on, so a Windows value such as {@code C:\local} would be judged relative everywhere else,
     * and the Windows variables are not governed by the XDG spec in any case.
     */
    private static Optional<Path> absolutePath(String source, String value) {
        if (!isSet(value)) {
            return Optional.empty();
        }
        try {
            Path path = Path.of(value);
            if (path.isAbsolute()) {
                return Optional.of(path);
            }
            log.warn("Ignoring {}: {} is not an absolute path", source, value);
        }
        catch (InvalidPathException e) {
            log.warn("Ignoring {}: {}", source, e.getLocalizedMessage());
        }
        return Optional.empty();
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
