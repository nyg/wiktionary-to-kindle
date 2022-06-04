package com.plainbash;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WordForm {
    private String infinitiveForm = "";
    private String description = "";
    private final boolean valid;

    public WordForm(String[] terms) {
        valid = terms.length >= 2;
        if (valid) {
            for (String term : terms) {
                if (!term.contains("=") && !term.contains("-form of")) {
                    infinitiveForm = term;
                }
            }
            description = Arrays.stream(terms).filter(s -> !s.contains("-form of") && !s.equalsIgnoreCase(infinitiveForm)).collect(Collectors.joining("|"));
        }
    }

    public String getInfinitiveForm() {
        return infinitiveForm;
    }

    public String getDescription() {
        return description;
    }

    public boolean isValid() {
        return valid;
    }
}
