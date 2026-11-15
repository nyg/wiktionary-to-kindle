package edu.self.w2k.render;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.self.w2k.model.WiktionaryForm;

/**
 * The passive forms a verb lists that belong under its passive lemma. English Wiktionary names
 * that lemma on a Greek verb's headline, as a form tagged {@code passive} and nothing else, and
 * then conjugates both voices in the active verb's table: σηκώθηκε sits under σηκώνω ("to lift")
 * although its own lemma is σηκώνομαι ("to get up").
 */
final class PassiveForms {

    private static final String PASSIVE = "passive";

    private PassiveForms() {}

    static Map<String, List<String>> byLemma(List<WiktionaryForm> forms) {
        Optional<String> lemma = forms.stream()
                .filter(form -> List.of(PASSIVE).equals(form.tags()))
                .map(PassiveForms::textOf)
                .filter(HtmlDefinitionRenderer::isUsableLookupKey)
                .findFirst();
        if (lemma.isEmpty()) {
            return Map.of();
        }
        Set<String> sharedWithAnotherVoice = forms.stream()
                .filter(form -> !isPassive(form))
                .map(PassiveForms::textOf)
                .collect(Collectors.toSet());
        List<String> passiveOnly = forms.stream()
                .filter(PassiveForms::isPassive)
                .map(PassiveForms::textOf)
                .filter(HtmlDefinitionRenderer::isUsableLookupKey)
                .filter(text -> !sharedWithAnotherVoice.contains(text) && !text.equals(lemma.get()))
                .distinct()
                .toList();
        return passiveOnly.isEmpty() ? Map.of() : Map.of(lemma.get(), passiveOnly);
    }

    private static boolean isPassive(WiktionaryForm form) {
        return form.tags() != null && form.tags().contains(PASSIVE);
    }

    private static String textOf(WiktionaryForm form) {
        return form.form().strip();
    }
}
