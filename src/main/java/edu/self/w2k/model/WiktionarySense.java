package edu.self.w2k.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WiktionarySense(List<String> glosses,
                              List<WiktionaryExample> examples) {}
