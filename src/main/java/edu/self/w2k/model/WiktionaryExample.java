package edu.self.w2k.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WiktionaryExample {

    private String text;

    public String getText() {
        return text;
    }
}
