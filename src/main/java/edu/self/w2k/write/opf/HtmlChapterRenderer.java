package edu.self.w2k.write.opf;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;

import edu.self.w2k.model.LexiconEntry;

class HtmlChapterRenderer {

    static final String KINDLE_NS = "https://kindlegen.s3.amazonaws.com/AmazonKindlePublishingGuidelines.pdf";
    static final int ENTRIES_PER_CHAPTER = 10_000;

    private HtmlChapterRenderer() {}

    static byte[] render(List<Map.Entry<String, List<LexiconEntry>>> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <html xmlns:mbp="%s"
                      xmlns:idx="%s">
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
                </head>
                <body>
                    <mbp:pagebreak/>
                    <mbp:frameset>
                        <mbp:slave-frame display="bottom" device="all" breadth="auto" leftmargin="0" rightmargin="0" bottommargin="0" topmargin="0">
                            <div align="center" bgcolor="yellow">
                                <a onclick="index_search()">Dictionary Search</a>
                            </div>
                        </mbp:slave-frame>
                        <mbp:pagebreak/>
                """.formatted(KINDLE_NS, KINDLE_NS));

        for (var entry : entries) {
            appendEntry(sb, entry.getValue());
        }

        sb.append("""
                    </mbp:frameset>
                </body>
                </html>
                """);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendEntry(StringBuilder sb, List<LexiconEntry> lexiconEntries) {
        // value must match the visible display term so kindling can locate entries in the text blob
        String displayTerm = StringEscapeUtils.escapeXml10(lexiconEntries.getFirst().word());
        StringBuilder combinedDef = new StringBuilder();
        for (int i = 0; i < lexiconEntries.size(); i++) {
            if (i > 0) combinedDef.append("; ");
            combinedDef.append(lexiconEntries.get(i).definition());
        }

        String inflectionMarkup = renderInflections(lexiconEntries);

        sb.append("""
                        <idx:entry name="word" scriptable="yes">
                            <idx:orth value="%s"><b>%s</b>%s</idx:orth>
                            %s
                        </idx:entry>
                """.formatted(displayTerm, displayTerm, inflectionMarkup, combinedDef));
    }

    private static String renderInflections(List<LexiconEntry> lexiconEntries) {
        Set<String> unioned = new LinkedHashSet<>();
        for (LexiconEntry entry : lexiconEntries) {
            List<String> forms = entry.inflectionForms();
            if (forms == null) {
                continue;
            }
            for (String form : forms) {
                if (form != null && !form.isBlank()) {
                    unioned.add(form);
                }
            }
        }
        if (unioned.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<idx:infl>");
        for (String form : unioned) {
            sb.append("<idx:iform value=\"")
                    .append(StringEscapeUtils.escapeXml10(form))
                    .append("\"/>");
        }
        sb.append("</idx:infl>");
        return sb.toString();
    }
}
