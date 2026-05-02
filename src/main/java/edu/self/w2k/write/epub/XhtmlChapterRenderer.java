package edu.self.w2k.write.epub;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import edu.self.w2k.model.LexiconEntry;

class XhtmlChapterRenderer {

    static final String KINDLE_NS = "https://kindlegen.s3.amazonaws.com/AmazonKindlePublishingGuidelines.pdf";

    private XhtmlChapterRenderer() {}

    static byte[] render(List<Map.Entry<String, List<LexiconEntry>>> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml"\
                 xmlns:mbp="%s"\
                 xmlns:idx="%s">
                <head>
                    <meta http-equiv="Content-Type" content="application/xhtml+xml; charset=utf-8"/>
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
            appendEntry(sb, entry.getKey(), entry.getValue());
        }

        sb.append("""
                    </mbp:frameset>
                </body>
                </html>
                """);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendEntry(StringBuilder sb, String key, List<LexiconEntry> lexiconEntries) {
        String displayTerm = lexiconEntries.getFirst().word();
        StringBuilder combinedDef = new StringBuilder();
        for (int i = 0; i < lexiconEntries.size(); i++) {
            if (i > 0) {
                combinedDef.append("; ");
            }
            combinedDef.append(lexiconEntries.get(i).definition());
        }

        sb.append("""
                        <idx:entry name="word" scriptable="yes">
                            <idx:orth value="%s"><strong>%s</strong></idx:orth>
                            %s
                        </idx:entry>
                """.formatted(key, displayTerm, combinedDef));
    }
}
