package edu.self.w2k.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PartOfSpeechTest {

    @Test
    void should_use_the_edition_title_without_its_homograph_number_when_one_is_given() {
        // When
        String result = PartOfSpeech.label("fr", "noun", "Nom commun 2");

        // Then
        assertThat(result).isEqualTo("Nom commun");
    }

    @Test
    void should_name_the_part_of_speech_in_greek_when_the_greek_edition_gives_no_title() {
        // When
        String result = PartOfSpeech.label("el", "adj", null);

        // Then
        assertThat(result).isEqualTo("επίθετο");
    }

    @Test
    void should_name_the_part_of_speech_in_russian_when_the_russian_edition_gives_no_title() {
        // When
        String result = PartOfSpeech.label("ru", "verb", "");

        // Then
        assertThat(result).isEqualTo("глагол");
    }

    @Test
    void should_spell_out_the_kaikki_code_when_no_title_and_no_translation_exist() {
        // When
        String result = PartOfSpeech.label("en", "adv", null);

        // Then
        assertThat(result).isEqualTo("adverb");
    }

    @Test
    void should_pass_the_code_through_when_it_is_already_a_word() {
        // When
        String result = PartOfSpeech.label("el", "symbol", null);

        // Then
        assertThat(result).isEqualTo("symbol");
    }

    @Test
    void should_return_nothing_when_neither_code_nor_title_is_known() {
        // When
        String result = PartOfSpeech.label("en", " ", null);

        // Then
        assertThat(result).isEmpty();
    }
}
