package edu.self.w2k.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

/**
 * The build's version, read from a resource Maven filters at package time.
 * <p>
 * Exists so the version is stated once, in the pom. It was previously hardcoded in {@code CLI}'s
 * {@code @Command} annotation, which meant {@code --version} would keep reporting 1.0.0 after any
 * release bump.
 */
@Slf4j
public final class AppVersion {

    static final String RESOURCE = "/application.properties";
    static final String UNKNOWN = "unknown";

    private static final String VERSION = load();

    private AppVersion() {}

    /** The project version, or {@code "unknown"} if the resource is missing or unfiltered. */
    public static String get() {
        return VERSION;
    }

    private static String load() {
        Properties props = new Properties();
        try (InputStream in = AppVersion.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                log.warn("{} not found on the classpath", RESOURCE);
                return UNKNOWN;
            }
            props.load(in);
        }
        catch (IOException e) {
            log.warn("Could not read {}: {}", RESOURCE, e.getLocalizedMessage());
            return UNKNOWN;
        }
        return sanitise(props.getProperty("version"));
    }

    /**
     * Guards against an unfiltered resource: running straight from {@code src/main/resources} (in an
     * IDE, say) leaves the literal placeholder in place, which would otherwise be shown as the
     * version.
     */
    static String sanitise(String value) {
        if (value == null || value.isBlank() || value.startsWith("${")) {
            return UNKNOWN;
        }
        return value.strip();
    }
}
