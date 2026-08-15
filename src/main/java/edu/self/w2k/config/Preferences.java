package edu.self.w2k.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

/**
 * User-adjustable settings for the desktop app, persisted as a plain properties file.
 * <p>
 * A properties file is used rather than {@link java.util.prefs.Preferences}, which writes to the
 * macOS defaults system and the Windows registry: those are neither inspectable by the user nor
 * straightforward to point at a temp directory in a test.
 * <p>
 * Notably absent is a max-heap setting. The heap is fixed when the JVM starts, so changing it from
 * inside the running app could not take effect, and rewriting jpackage's {@code .cfg} would
 * invalidate the macOS ad-hoc signature. The bundle instead ships
 * {@code -XX:MaxRAMPercentage=75}, which scales with the machine.
 *
 * Every path is made absolute on construction, wherever it came from — the properties file, the
 * preferences dialog, or a caller. A relative path would otherwise mean two different directories to
 * the two front-ends: a bundled {@code .app} launches with the working directory set to {@code /},
 * while the CLI inherits the shell's. Normalising here rather than at each use keeps the invariant
 * the rest of the code already assumes, and shows the user the resolved path when the dialog
 * reopens.
 *
 * @param dumpsDir        where downloaded kaikki.org dumps are kept
 * @param dictionariesDir where generated dictionaries are written
 * @param kindlingCliPath explicit kindling-cli binary, bypassing PATH/cache/download resolution
 * @param kindlingVersion kindling release tag to download; empty means the pinned default
 * @param theme           look of the desktop window; read by the GUI only, ignored by the CLI
 */
@Slf4j
public record Preferences(Path dumpsDir,
                          Path dictionariesDir,
                          Optional<Path> kindlingCliPath,
                          Optional<String> kindlingVersion,
                          AppTheme theme) {

    public Preferences {
        dumpsDir = absolute(dumpsDir);
        dictionariesDir = absolute(dictionariesDir);
        kindlingCliPath = kindlingCliPath.map(Preferences::absolute);
    }

    static final String FILE_NAME = "preferences.properties";

    private static final String KEY_DUMPS_DIR = "dumpsDir";
    private static final String KEY_DICTIONARIES_DIR = "dictionariesDir";
    private static final String KEY_KINDLING_CLI_PATH = "kindlingCliPath";
    private static final String KEY_KINDLING_VERSION = "kindlingVersion";
    private static final String KEY_THEME = "theme";

    private static final String FILE_COMMENT = """
            %s preferences. Blank or missing values fall back to the defaults.
            kindlingVersion accepts a release tag such as v0.28.0; blank uses the pinned default.
            theme accepts javafx or cupertino, and only the desktop app reads it."""
            .formatted(AppInfo.SLUG);

    public static Preferences defaults() {
        Path dataDir = AppPaths.defaultDataDir();
        return new Preferences(dataDir.resolve("dumps"),
                               dataDir.resolve("dictionaries"),
                               Optional.empty(),
                               Optional.empty(),
                               AppTheme.defaultForThisPlatform());
    }

    /** Loads from the standard location, falling back to {@link #defaults()} if unreadable. */
    public static Preferences load() {
        return load(AppPaths.configDir().resolve(FILE_NAME));
    }

    /**
     * Loads from {@code file}. A missing file, or any individual missing or blank value, falls back to
     * the corresponding default — a partially hand-edited file stays usable.
     */
    public static Preferences load(Path file) {
        Preferences defaults = defaults();
        if (!Files.exists(file)) {
            return defaults;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        }
        catch (IOException e) {
            log.warn("Could not read preferences from {} ({}); using defaults", file, e.getLocalizedMessage());
            return defaults;
        }

        return new Preferences(
                path(props, KEY_DUMPS_DIR).orElse(defaults.dumpsDir()),
                path(props, KEY_DICTIONARIES_DIR).orElse(defaults.dictionariesDir()),
                path(props, KEY_KINDLING_CLI_PATH),
                value(props, KEY_KINDLING_VERSION),
                value(props, KEY_THEME).flatMap(AppTheme::fromKey).orElse(defaults.theme()));
    }

    /** Writes to the standard location, creating the config directory if needed. */
    public Path store() throws IOException {
        return store(AppPaths.configDir().resolve(FILE_NAME));
    }

    public Path store(Path file) throws IOException {
        Properties props = new Properties();
        props.setProperty(KEY_DUMPS_DIR, dumpsDir.toString());
        props.setProperty(KEY_DICTIONARIES_DIR, dictionariesDir.toString());
        props.setProperty(KEY_KINDLING_CLI_PATH, kindlingCliPath.map(Path::toString).orElse(""));
        props.setProperty(KEY_KINDLING_VERSION, kindlingVersion.orElse(""));
        props.setProperty(KEY_THEME, theme.key());

        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, FILE_COMMENT);
        }
        return file;
    }

    private static Optional<String> value(Properties props, String key) {
        return Optional.ofNullable(props.getProperty(key))
                .map(String::strip)
                .filter(s -> !s.isEmpty());
    }

    private static Optional<Path> path(Properties props, String key) {
        return value(props, key).map(Path::of);
    }

    private static Path absolute(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
