package edu.self.w2k.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import edu.self.w2k.model.WiktionaryExample;

final class Examples {

    static final int MAX_PER_SENSE = 3;

    private Examples() {}

    static void appendTo(StringBuilder sb, List<WiktionaryExample> examples) {
        List<WiktionaryExample> shown = shortest(examples);
        if (shown.isEmpty()) {
            return;
        }
        sb.append("<ul>");
        for (WiktionaryExample example : shown) {
            sb.append("<li>").append(emphasised(example.text(), example.boldTextOffsets()));
            String translation = translationOf(example);
            if (!translation.isEmpty()) {
                sb.append(" — <i>").append(HtmlText.escape(translation)).append("</i>");
            }
            sb.append("</li>");
        }
        sb.append("</ul>");
    }

    private static List<WiktionaryExample> shortest(List<WiktionaryExample> examples) {
        List<WiktionaryExample> withText = examples.stream()
                .filter(example -> example != null && !HtmlText.isBlank(example.text()))
                .toList();
        if (withText.size() <= MAX_PER_SENSE) {
            return withText;
        }
        int longestKept = withText.stream()
                .mapToInt(example -> example.text().length())
                .sorted()
                .skip(MAX_PER_SENSE - 1L)
                .findFirst()
                .orElseThrow();
        return withText.stream()
                .filter(example -> example.text().length() <= longestKept)
                .limit(MAX_PER_SENSE)
                .toList();
    }

    private static String translationOf(WiktionaryExample example) {
        if (!HtmlText.isBlank(example.translation())) {
            return example.translation().strip();
        }
        return HtmlText.isBlank(example.english()) ? "" : example.english().strip();
    }

    // kaikki counts bold_text_offsets in code points, as Python indexes strings; Java indexes UTF-16 units.
    static String emphasised(String text, List<List<Integer>> boldTextOffsets) {
        int length = text.codePointCount(0, text.length());
        StringBuilder sb = new StringBuilder();
        int cursor = 0;
        for (int[] range : validRanges(boldTextOffsets, length)) {
            int start = text.offsetByCodePoints(0, range[0]);
            int end = text.offsetByCodePoints(0, range[1]);
            if (start < cursor) {
                continue;
            }
            sb.append(HtmlText.escape(text.substring(cursor, start)))
                    .append("<b>").append(HtmlText.escape(text.substring(start, end))).append("</b>");
            cursor = end;
        }
        return sb.append(HtmlText.escape(text.substring(cursor))).toString();
    }

    private static List<int[]> validRanges(List<List<Integer>> offsets, int length) {
        List<int[]> ranges = new ArrayList<>();
        for (List<Integer> offset : offsets) {
            if (offset == null || offset.size() != 2 || offset.get(0) == null || offset.get(1) == null) {
                continue;
            }
            int start = offset.get(0);
            int end = offset.get(1);
            if (start >= 0 && start < end && end <= length) {
                ranges.add(new int[] {start, end});
            }
        }
        ranges.sort(Comparator.comparingInt(range -> range[0]));
        return ranges;
    }
}
