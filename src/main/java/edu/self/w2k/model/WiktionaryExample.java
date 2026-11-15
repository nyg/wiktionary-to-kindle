package edu.self.w2k.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.With;

@With
@JsonIgnoreProperties(ignoreUnknown = true)
public record WiktionaryExample(String text,
                                String translation,
                                String english,
                                @JsonProperty("bold_text_offsets") List<List<Integer>> boldTextOffsets) {

    public WiktionaryExample {
        boldTextOffsets = Lists.orEmpty(boldTextOffsets);
    }

    public WiktionaryExample(String text) {
        this(text, null, null, null);
    }
}
