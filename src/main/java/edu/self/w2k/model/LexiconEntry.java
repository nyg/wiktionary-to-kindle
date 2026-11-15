package edu.self.w2k.model;

import java.util.List;
import java.util.Map;

import lombok.With;

@With
public record LexiconEntry(String word,
                           String definition,
                           List<String> inflectionForms,
                           List<String> formOfLemmas,
                           Map<String, List<String>> counterpartForms) {

    public LexiconEntry(String word, String definition, List<String> inflectionForms, List<String> formOfLemmas) {
        this(word, definition, inflectionForms, formOfLemmas, Map.of());
    }
}
