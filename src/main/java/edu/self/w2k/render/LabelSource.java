package edu.self.w2k.render;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import edu.self.w2k.model.WiktionarySense;

enum LabelSource {

    // The English edition already writes each sense's labels as the prefix of its raw gloss.
    RAW_GLOSSES,
    // The Greek edition repeats every normalised tag in raw_tags, in Greek: showing both says it twice.
    RAW_TAGS,
    ALL_TAGS;

    private static final Set<String> STRUCTURAL_TAGS = Set.of("form-of", "alt-of", "no-gloss", "error-lua-exec");

    static LabelSource forEdition(String edition) {
        if ("en".equals(edition)) {
            return RAW_GLOSSES;
        }
        if ("el".equals(edition)) {
            return RAW_TAGS;
        }
        return ALL_TAGS;
    }

    List<String> labelsOf(WiktionarySense sense) {
        return switch (this) {
            case RAW_GLOSSES -> List.of();
            case RAW_TAGS -> distinct(sense.rawTags().stream());
            case ALL_TAGS -> distinct(Stream.concat(sense.rawTags().stream(),
                    Stream.concat(readable(sense.tags()), readable(sense.topics()))));
        };
    }

    List<String> displayGlosses(WiktionarySense sense, List<String> glosses) {
        List<String> raw = sense.rawGlosses().stream().filter(gloss -> !HtmlText.isBlank(gloss)).toList();
        return this == RAW_GLOSSES && raw.size() == glosses.size() ? raw : glosses;
    }

    private static Stream<String> readable(List<String> normalisedTags) {
        return normalisedTags.stream()
                .filter(tag -> tag != null && !STRUCTURAL_TAGS.contains(tag))
                .map(tag -> tag.replace('-', ' '));
    }

    private static List<String> distinct(Stream<String> labels) {
        return labels.filter(label -> !HtmlText.isBlank(label)).map(String::strip).distinct().toList();
    }
}
