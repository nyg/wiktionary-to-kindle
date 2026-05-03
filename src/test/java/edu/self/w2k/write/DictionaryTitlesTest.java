package edu.self.w2k.write;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DictionaryTitlesTest {

    @Test
    void autoTitle_knownLanguageCodes() {
        String title = DictionaryTitles.autoTitle("fr", "en");
        assertTrue(title.contains("French"), "should contain 'French', got: " + title);
        assertTrue(title.contains("English"), "should contain 'English', got: " + title);
        assertTrue(title.endsWith("Dictionary"), "should end with 'Dictionary'");
    }

    @Test
    void autoTitle_unknownCodeFallsBackToUpperCase() {
        String title = DictionaryTitles.autoTitle("xx", "en");
        assertTrue(title.contains("XX"), "unknown code should fall back to upper-case, got: " + title);
    }
}
