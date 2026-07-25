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
import edu.self.w2k.model.WiktionaryFormOf;
import edu.self.w2k.model.WiktionarySense;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HtmlDefinitionRenderer implements DefinitionRenderer {

    private static final int VISIBLE_FORMS_THRESHOLD = 30;

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

        return Optional.of(new RenderedEntry(sb.toString(), inflectionForms, collectFormOfLemmas(entry.senses())));
    }

    /**
     * Returns the distinct lemma words this entry is an inflection of, but only when the entry is
     * "form-of-only", i.e. every sense with a renderable gloss carries a {@code form_of} reference.
     * Mixed entries (at least one sense with an independent meaning) return an empty list so they
     * are kept as regular headwords.
     */
    private static List<String> collectFormOfLemmas(List<WiktionarySense> senses) {
        Set<String> lemmas = new LinkedHashSet<>();
        boolean hasRenderableSense = false;

        for (WiktionarySense sense : senses) {
            if (sense == null || !hasRenderableGloss(sense)) {
                continue;
            }
            hasRenderableSense = true;

            boolean hasLemma = false;
            List<WiktionaryFormOf> formOf = sense.formOf();
            if (formOf != null) {
                for (WiktionaryFormOf ref : formOf) {
                    if (ref == null) {
                        continue;
                    }
                    String word = ref.word();
                    if (word == null || word.isBlank()) {
                        continue;
                    }
                    hasLemma = true;
                    lemmas.add(word.strip());
                }
            }
            if (!hasLemma) {
                // a sense with its own meaning — not a form-of-only entry
                return List.of();
            }
        }

        return hasRenderableSense ? List.copyOf(lemmas) : List.of();
    }

    private static boolean hasRenderableGloss(WiktionarySense sense) {
        for (String gloss : sense.glosses()) {
            if (gloss != null && !gloss.isBlank()) {
                return true;
            }
        }
        return false;
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

    public static boolean isUsableLookupKey(String text) {
        if (text.isEmpty()) {
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
