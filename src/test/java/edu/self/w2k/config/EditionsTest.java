package edu.self.w2k.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EditionsTest {

    @Test
    void should_return_english_when_edition_is_simple_english() {
        // When
        String result = Editions.languageOf("simple");

        // Then
        assertThat(result).isEqualTo("en");
    }

    @Test
    void should_return_the_code_itself_when_edition_is_a_language() {
        // When
        String result = Editions.languageOf("el");

        // Then
        assertThat(result).isEqualTo("el");
    }
}
