package edu.self.w2k.config;

public final class Editions {

    private static final String SIMPLE_ENGLISH = "simple";

    private Editions() {}

    // Every kaikki edition code is a language code except "simple", which names the Simple English site.
    public static String languageOf(String edition) {
        return SIMPLE_ENGLISH.equals(edition) ? "en" : edition;
    }
}
