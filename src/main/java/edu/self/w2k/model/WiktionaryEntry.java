package edu.self.w2k.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.With;

@With
@JsonIgnoreProperties(ignoreUnknown = true)
public record WiktionaryEntry(String word,
                              @JsonProperty("lang_code") String langCode,
                              String pos,
                              @JsonProperty("pos_title") String posTitle,
                              List<String> tags,
                              List<WiktionarySound> sounds,
                              @JsonProperty("etymology_text") String etymologyText,
                              @JsonProperty("etymology_texts") List<String> etymologyTexts,
                              List<WiktionarySense> senses,
                              List<WiktionaryForm> forms,
                              List<WiktionaryLinkage> synonyms,
                              List<WiktionaryLinkage> antonyms) {

    public WiktionaryEntry {
        tags = Lists.orEmpty(tags);
        sounds = Lists.orEmpty(sounds);
        etymologyTexts = Lists.orEmpty(etymologyTexts);
        senses = Lists.orEmpty(senses);
        forms = Lists.orEmpty(forms);
        synonyms = Lists.orEmpty(synonyms);
        antonyms = Lists.orEmpty(antonyms);
    }

    public WiktionaryEntry(String word, String langCode, String pos,
                           List<WiktionarySense> senses, List<WiktionaryForm> forms) {
        this(word, langCode, pos, null, null, null, null, null, senses, forms, null, null);
    }
}
