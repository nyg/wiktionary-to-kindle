package edu.self.w2k.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WiktionaryEntry(String word,
                              @JsonProperty("lang_code") String langCode,
                              String pos,
                              List<WiktionarySense> senses,
                              List<WiktionaryForm> forms) {}
