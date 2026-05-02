package edu.self.w2k.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WiktionarySense(List<String> glosses, List<WiktionaryExample> examples) {}
