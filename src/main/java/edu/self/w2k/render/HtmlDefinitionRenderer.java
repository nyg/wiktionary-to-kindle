package edu.self.w2k.render;

import edu.self.w2k.model.WiktionaryExample;
import edu.self.w2k.model.WiktionarySense;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
public class HtmlDefinitionRenderer implements DefinitionRenderer {

    @Override
    public Optional<String> render(List<WiktionarySense> senses) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ol>");
        boolean hasGloss = false;

        for (WiktionarySense sense : senses) {
            if (sense == null) continue;
            List<String> glosses = sense.glosses();
            if (glosses.isEmpty()) continue;

            for (String gloss : glosses) {
                if (gloss == null || gloss.isBlank()) continue;
                hasGloss = true;
                sb.append("<li><span>");
                sb.append(StringEscapeUtils.escapeXml10(gloss.replaceAll("[\n\r]", "; ")));
                sb.append("</span>");

                List<WiktionaryExample> examples = sense.examples();
                boolean hasExample = false;
                StringBuilder exSb = new StringBuilder("<ul>");
                for (WiktionaryExample ex : examples) {
                    if (ex == null) continue;
                    String text = ex.text();
                    if (text == null || text.isBlank()) continue;
                    hasExample = true;
                    exSb.append("<li>");
                    exSb.append(StringEscapeUtils.escapeXml10(text.replaceAll("[\n\r]", "; ")));
                    exSb.append("</li>");
                }
                exSb.append("</ul>");
                if (hasExample) sb.append(exSb);

                sb.append("</li>");
            }
        }

        sb.append("</ol>");
        return hasGloss ? Optional.of(sb.toString()) : Optional.empty();
    }
}
