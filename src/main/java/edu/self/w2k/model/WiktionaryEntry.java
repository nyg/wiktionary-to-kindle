package edu.self.w2k.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WiktionaryEntry(
        String word,
        @JsonProperty("lang_code") String langCode,
        List<WiktionarySense> senses) {}
