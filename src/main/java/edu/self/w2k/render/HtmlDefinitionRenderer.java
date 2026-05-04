package edu.self.w2k.render;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;

import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.model.WiktionaryExample;
import edu.self.w2k.model.WiktionaryForm;
import edu.self.w2k.model.WiktionarySense;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HtmlDefinitionRenderer implements DefinitionRenderer {

    private static final String EQUIV_POUR_MARKER = "équiv-pour";
    private static final int VISIBLE_FORMS_THRESHOLD = 30;

    // wiktextract emits Greek article-cells (η, οι, του, …) as standalone forms rows
    // alongside real inflections; indexing them as iforms ties common tokens to
    // hundreds of headwords and crashes the Kindle popup on long-press.
    private static final Set<String> NON_LOOKUP_FORMS = Set.of(
            "ο", "η", "το", "οι", "τα",
            "του", "της", "των",
            "τον", "την", "τη", "τους", "τις");

    @Override
    public Optional<RenderedEntry> render(WiktionaryEntry entry) {
        StringBuilder sb = new StringBuilder();
        boolean hasGloss = appendDefinitions(sb, entry.senses());
        if (!hasGloss) {
            return Optional.empty();
        }

        List<WiktionaryForm> filtered = filterForms(entry.forms());
        List<String> inflectionForms = collectInflectionForms(filtered);

        if (shouldRenderVisibleTable(entry.pos(), filtered)) {
            appendFormsTable(sb, filtered);
        }

        return Optional.of(new RenderedEntry(sb.toString(), inflectionForms));
    }

    private static boolean appendDefinitions(StringBuilder sb, List<WiktionarySense> senses) {
        sb.append("<ol>");
        boolean hasGloss = false;

        for (WiktionarySense sense : senses) {
            if (sense == null) {
                continue;
            }
            List<String> glosses = sense.glosses();
            if (glosses.isEmpty()) {
                continue;
            }

            for (String gloss : glosses) {
                if (gloss == null || gloss.isBlank()) {
                    continue;
                }
                hasGloss = true;
                sb.append("<li><span>");
                sb.append(StringEscapeUtils.escapeXml10(gloss.replaceAll("[\n\r]", "; ")));
                sb.append("</span>");

                List<WiktionaryExample> examples = sense.examples();
                boolean hasExample = false;
                StringBuilder exSb = new StringBuilder("<ul>");
                for (WiktionaryExample ex : examples) {
                    if (ex == null) {
                        continue;
                    }
                    String text = ex.text();
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    hasExample = true;
                    exSb.append("<li>");
                    exSb.append(StringEscapeUtils.escapeXml10(text.replaceAll("[\n\r]", "; ")));
                    exSb.append("</li>");
                }
                exSb.append("</ul>");
                if (hasExample) {
                    sb.append(exSb);
                }

                sb.append("</li>");
            }
        }

        sb.append("</ol>");
        return hasGloss;
    }

    private static List<WiktionaryForm> filterForms(List<WiktionaryForm> forms) {
        if (forms == null || forms.isEmpty()) {
            return List.of();
        }
        List<WiktionaryForm> kept = new ArrayList<>(forms.size());
        for (WiktionaryForm form : forms) {
            if (form == null) {
                continue;
            }
            String text = form.form();
            if (text == null || text.isBlank()) {
                continue;
            }
            String source = form.source();
            if (source != null && source.contains(EQUIV_POUR_MARKER)) {
                continue;
            }
            kept.add(form);
        }
        return kept;
    }

    private static List<String> collectInflectionForms(List<WiktionaryForm> filtered) {
        if (filtered.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>(filtered.size());
        for (WiktionaryForm form : filtered) {
            String text = form.form().strip();
            if (isUsableLookupKey(text)) {
                seen.add(text);
            }
        }
        return List.copyOf(seen);
    }

    private static boolean isUsableLookupKey(String text) {
        if (text.isEmpty() || NON_LOOKUP_FORMS.contains(text)) {
            return false;
        }
        boolean hasLetter = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || c == '(' || c == ')') {
                return false;
            }
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
        }
        return hasLetter;
    }

    private static boolean shouldRenderVisibleTable(String pos, List<WiktionaryForm> filtered) {
        if (filtered.isEmpty()) {
            return false;
        }
        if (pos != null && "verb".equalsIgnoreCase(pos)) {
            return false;
        }
        return filtered.size() <= VISIBLE_FORMS_THRESHOLD;
    }

    private static void appendFormsTable(StringBuilder sb, List<WiktionaryForm> filtered) {
        sb.append("<p><i>Forms:</i></p><ul>");
        for (WiktionaryForm form : filtered) {
            sb.append("<li>");
            String tagAbbrev = InflectionTagAbbreviator.abbreviate(form.tags());
            if (!tagAbbrev.isEmpty()) {
                sb.append(StringEscapeUtils.escapeXml10(tagAbbrev));
                sb.append(": ");
            }
            String article = form.article();
            if (article != null && !article.isBlank()) {
                sb.append(StringEscapeUtils.escapeXml10(article.strip()));
                sb.append(' ');
            }
            sb.append(StringEscapeUtils.escapeXml10(form.form().strip()));
            sb.append("</li>");
        }
        sb.append("</ul>");
    }
}
