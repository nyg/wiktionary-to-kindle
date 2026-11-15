package edu.self.w2k.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.With;

@With
@JsonIgnoreProperties(ignoreUnknown = true)
public record WiktionarySense(List<String> glosses,
                              @JsonProperty("raw_glosses") List<String> rawGlosses,
                              List<String> tags,
                              @JsonProperty("raw_tags") List<String> rawTags,
                              List<String> topics,
                              List<WiktionaryExample> examples,
                              @JsonProperty("form_of") List<WiktionaryFormOf> formOf,
                              List<WiktionaryLinkage> synonyms,
                              List<WiktionaryLinkage> antonyms) {

    public WiktionarySense {
        glosses = Lists.orEmpty(glosses);
        rawGlosses = Lists.orEmpty(rawGlosses);
        tags = Lists.orEmpty(tags);
        rawTags = Lists.orEmpty(rawTags);
        topics = Lists.orEmpty(topics);
        examples = Lists.orEmpty(examples);
        formOf = Lists.orEmpty(formOf);
        synonyms = Lists.orEmpty(synonyms);
        antonyms = Lists.orEmpty(antonyms);
    }

    public WiktionarySense(List<String> glosses, List<WiktionaryExample> examples, List<WiktionaryFormOf> formOf) {
        this(glosses, null, null, null, null, examples, formOf, null, null);
    }
}
