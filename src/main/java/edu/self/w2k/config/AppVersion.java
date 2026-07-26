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

    private static final String VERSION = load(RESOURCE);

    private AppVersion() {}

    /** The project version, or {@code "unknown"} if the resource is missing or unfiltered. */
    public static String get() {
        return VERSION;
    }

    static String load(String resource) {
        Properties props = new Properties();
        try (InputStream in = AppVersion.class.getResourceAsStream(resource)) {
            if (in == null) {
                log.warn("{} not found on the classpath", resource);
                return UNKNOWN;
            }
            props.load(in);
        }
        // IllegalArgumentException is caught alongside IOException because Properties.load throws it
        // for malformed unicode escapes — the likely shape of a corrupt or wrongly filtered resource.
        // Letting it escape would fail this class's static initialiser, turning a cosmetic version
        // lookup into an ExceptionInInitializerError at startup.
        catch (IOException | IllegalArgumentException e) {
            log.warn("Could not read {}: {}", resource, e.getLocalizedMessage());
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
