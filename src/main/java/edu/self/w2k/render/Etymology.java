package edu.self.w2k.render;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.self.w2k.model.WiktionaryEntry;

final class Etymology {

    private static final String TREE_HEADING = "Etymology tree";
    private static final int LONGEST_TREE_LINE = 60;
    private static final String BULLET = "• ";

    private static final Pattern MAGIC_WORD = Pattern.compile("__[A-Z]+__");
    private static final Pattern LANGUAGE_MARKER = Pattern.compile("\\s*\\^\\([^)]*\\)");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern LIST_MARKER = Pattern.compile("^[*#:]+\\s*");

    private Etymology() {}

    static void appendTo(StringBuilder sb, WiktionaryEntry entry) {
        List<String> lines = linesOf(entry);
        if (!lines.isEmpty()) {
            sb.append("<div>[")
                    .append(lines.stream().map(HtmlText::escape).collect(Collectors.joining("<br/>")))
                    .append("]</div>");
        }
    }

    static List<String> linesOf(WiktionaryEntry entry) {
        Stream<String> raw = HtmlText.isBlank(entry.etymologyText())
                ? entry.etymologyTexts().stream().filter(Objects::nonNull).flatMap(String::lines)
                : withoutTree(entry.etymologyText());
        return raw.map(Etymology::cleaned).filter(line -> !line.isEmpty()).toList();
    }

    private static String cleaned(String line) {
        String text = MAGIC_WORD.matcher(line).replaceAll("");
        text = LANGUAGE_MARKER.matcher(text).replaceAll("");
        text = WHITESPACE.matcher(text).replaceAll(" ").strip();
        Matcher marker = LIST_MARKER.matcher(text);
        if (!marker.find()) {
            return text;
        }
        String item = text.substring(marker.end());
        return item.isEmpty() ? "" : BULLET + item;
    }

    // The English edition prints the ancestry tree, one short node per line, ahead of the prose.
    private static Stream<String> withoutTree(String text) {
        if (!text.startsWith(TREE_HEADING)) {
            return text.lines();
        }
        return text.lines()
                .skip(1)
                .dropWhile(line -> line.length() < LONGEST_TREE_LINE && !line.endsWith("."));
    }
}
