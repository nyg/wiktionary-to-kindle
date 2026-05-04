package edu.self.w2k.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WiktionaryForm(String form,
                             List<String> tags,
                             String article,
                             String source) {}
