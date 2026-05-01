package edu.self.w2k.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WiktionaryEntry {

    private String word;

    @JsonProperty("lang_code")
    private String langCode;

    private List<WiktionarySense> senses;

    public String getWord() {
        return word;
    }

    public String getLangCode() {
        return langCode;
    }

    public List<WiktionarySense> getSenses() {
        return senses != null ? senses : List.of();
    }
}
