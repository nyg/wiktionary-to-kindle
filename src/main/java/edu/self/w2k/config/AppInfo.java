package edu.self.w2k.config;

/**
 * The application's own names, in the two forms it needs.
 * <p>
 * Kept here rather than repeated as literals so a rename cannot half-apply — the config directory
 * and the cache directory used to spell the name independently, and nothing would have caught them
 * drifting apart.
 */
public final class AppInfo {

    /** Shown to people: window title, macOS bundle, Windows launcher, Scoop shortcut. */
    public static final String DISPLAY_NAME = "Wiktionary to Kindle";

    /**
     * Machine-readable form: Maven artifactId, CLI command name, directory names, release assets.
     * A compile-time constant, so it can be used in the picocli {@code @Command} annotation.
     */
    public static final String SLUG = "wiktionary-to-kindle";

    /**
     * Prefixes generated dictionary titles and filenames, so they group together and stay
     * distinguishable from other dictionaries in the Kindle settings list.
     */
    public static final String DICTIONARY_PREFIX = "W2K";

    private AppInfo() {}
}
