package edu.self.w2k.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WiktionaryEntry {

    private String word;

    @JsonProperty("lang_code")
    private String langCode;

    private List<WiktionarySense> senses;

    public List<WiktionarySense> getSenses() {
        return senses != null ? senses : List.of();
    }
}
