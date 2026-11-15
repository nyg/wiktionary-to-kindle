package edu.self.w2k.render;

import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

final class HtmlText {

    private static final Pattern LINE_BREAKS = Pattern.compile("[\\n\\r]+");

    private HtmlText() {}

    static String escape(String text) {
        return StringEscapeUtils.escapeXml10(LINE_BREAKS.matcher(text).replaceAll("; "));
    }

    static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
