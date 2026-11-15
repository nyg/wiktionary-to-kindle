package edu.self.w2k.render;

import java.util.List;
import java.util.Map;

import edu.self.w2k.model.WiktionaryForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PassiveFormsTest {

    @Test
    void should_group_passive_forms_under_the_lemma_when_the_headline_names_one() {
        // Given
        List<WiktionaryForm> forms = List.of(
                new WiktionaryForm("sikóno", List.of("romanization"), null, null),
                new WiktionaryForm("σηκώνομαι", List.of("passive"), null, null),
                new WiktionaryForm("σηκώνομαι", List.of("first-person", "passive", "present", "singular"), null, "conjugation"),
                new WiktionaryForm("σήκωσε", List.of("active", "past", "singular", "third-person"), null, "conjugation"),
                new WiktionaryForm("σηκώθηκε", List.of("passive", "past", "singular", "third-person"), null, "conjugation"),
                new WiktionaryForm("σηκώθηκε", List.of("passive", "perfective"), null, "conjugation"),
                new WiktionaryForm("να σηκωθεί", List.of("passive", "subjunctive"), null, "conjugation"));

        // When
        Map<String, List<String>> result = PassiveForms.byLemma(forms);

        // Then
        assertThat(result).containsExactly(Map.entry("σηκώνομαι", List.of("σηκώθηκε")));
    }

    @Test
    void should_return_nothing_when_no_form_is_tagged_passive_alone() {
        // Given
        List<WiktionaryForm> forms = List.of(
                new WiktionaryForm("amatur", List.of("indicative", "passive", "present"), null, null),
                new WiktionaryForm("amat", List.of("active", "indicative", "present"), null, null),
                new WiktionaryForm("amor", null, null, null));

        // When
        Map<String, List<String>> result = PassiveForms.byLemma(forms);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_leave_out_a_form_when_another_voice_spells_it_the_same() {
        // Given
        List<WiktionaryForm> forms = List.of(
                new WiktionaryForm("πλένομαι", List.of("passive"), null, null),
                new WiktionaryForm("πλύνετε", List.of("active", "imperative"), null, "conjugation"),
                new WiktionaryForm("πλύνετε", List.of("passive", "imperative"), null, "conjugation"));

        // When
        Map<String, List<String>> result = PassiveForms.byLemma(forms);

        // Then
        assertThat(result).isEmpty();
    }
}
