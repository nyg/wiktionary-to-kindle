package edu.self.w2k.model;

import java.util.List;

public record LexiconEntry(String word,
                           String definition,
                           List<String> inflectionForms) {}
