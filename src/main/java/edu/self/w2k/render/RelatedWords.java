package edu.self.w2k.render;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.model.WiktionaryLinkage;
import edu.self.w2k.model.WiktionarySense;

final class RelatedWords {

    static final int MAX_WORDS = 6;

    private RelatedWords() {}

    static void appendTo(StringBuilder sb, WiktionaryEntry entry) {
        appendLine(sb, "≈", wordsOf(entry, entry.synonyms(), WiktionarySense::synonyms));
        appendLine(sb, "≠", wordsOf(entry, entry.antonyms(), WiktionarySense::antonyms));
    }

    private static void appendLine(StringBuilder sb, String sign, List<String> words) {
        if (!words.isEmpty()) {
            sb.append("<div>").append(sign).append(' ').append(HtmlText.escape(String.join(", ", words))).append("</div>");
        }
    }

    private static List<String> wordsOf(WiktionaryEntry entry, List<WiktionaryLinkage> ofEntry,
                                        Function<WiktionarySense, List<WiktionaryLinkage>> ofSense) {
        Stream<WiktionaryLinkage> ofSenses = entry.senses().stream()
                .filter(Objects::nonNull)
                .flatMap(sense -> ofSense.apply(sense).stream());
        return Stream.concat(ofEntry.stream(), ofSenses)
                .filter(linkage -> linkage != null && !HtmlText.isBlank(linkage.word()))
                .map(linkage -> linkage.word().strip())
                .filter(word -> !word.equals(entry.word()))
                .distinct()
                .limit(MAX_WORDS)
                .toList();
    }
}
