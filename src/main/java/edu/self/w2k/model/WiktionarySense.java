package edu.self.w2k.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WiktionarySense(List<String> glosses,
                              List<WiktionaryExample> examples,
                              @JsonProperty("form_of") List<WiktionaryFormOf> formOf) {}
