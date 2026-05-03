package edu.self.w2k.write;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DictionaryTitlesTest {

    @Test
    void should_use_display_names_when_lang_codes_are_known() {
        // Given / When
        String result = DictionaryTitles.autoTitle("el", "en");

        // Then
        assertThat(result)
                .contains("Greek")
                .contains("English")
                .contains("–")
                .endsWith("Dictionary");
    }

    @Test
    void should_uppercase_when_lang_code_is_unknown() {
        // Given / When
        String result = DictionaryTitles.autoTitle("xx", "yy");

        // Then
        assertThat(result).startsWith("XX").endsWith("Dictionary");
    }
}
