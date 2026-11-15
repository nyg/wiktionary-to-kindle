package edu.self.w2k.render;

import java.util.List;
import java.util.regex.Pattern;

import edu.self.w2k.model.WiktionaryEntry;

final class Etymology {

    private static final String TREE_HEADING = "Etymology tree";
    private static final int LONGEST_TREE_LINE = 60;

    private static final Pattern MAGIC_WORD = Pattern.compile("__[A-Z]+__");
    private static final Pattern LANGUAGE_MARKER = Pattern.compile("\\s*\\^\\([^)]*\\)");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private Etymology() {}

    static void appendTo(StringBuilder sb, WiktionaryEntry entry) {
        String text = textOf(entry);
        if (!text.isEmpty()) {
            sb.append("<div>[").append(HtmlText.escape(text)).append("]</div>");
        }
    }

    static String textOf(WiktionaryEntry entry) {
        String raw = HtmlText.isBlank(entry.etymologyText())
                ? String.join(" ", entry.etymologyTexts())
                : withoutTree(entry.etymologyText());
        String cleaned = MAGIC_WORD.matcher(raw).replaceAll("");
        cleaned = LANGUAGE_MARKER.matcher(cleaned).replaceAll("");
        return WHITESPACE.matcher(cleaned).replaceAll(" ").strip();
    }

    // The English edition prints the ancestry tree, one short node per line, ahead of the prose.
    private static String withoutTree(String text) {
        if (!text.startsWith(TREE_HEADING)) {
            return text;
        }
        List<String> prose = text.lines()
                .skip(1)
                .dropWhile(line -> line.length() < LONGEST_TREE_LINE && !line.endsWith("."))
                .toList();
        return String.join(" ", prose);
    }
}
