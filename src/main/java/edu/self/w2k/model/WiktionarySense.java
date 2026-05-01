package edu.self.w2k.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WiktionarySense {

    private List<String> glosses;

    private List<WiktionaryExample> examples;

    public List<String> getGlosses() {
        return glosses != null ? glosses : List.of();
    }

    public List<WiktionaryExample> getExamples() {
        return examples != null ? examples : List.of();
    }
}
