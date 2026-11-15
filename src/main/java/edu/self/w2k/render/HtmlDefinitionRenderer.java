package edu.self.w2k.render;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;

import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.model.WiktionaryForm;
import edu.self.w2k.model.WiktionaryFormOf;
import edu.self.w2k.model.WiktionarySense;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HtmlDefinitionRenderer implements DefinitionRenderer {

    private static final int VISIBLE_FORMS_THRESHOLD = 30;

    private static final String LIST_ITEM_END = "</li>";

    // Rows the English and Greek editions list among the forms to name the table's template, not a form.
    private static final Set<String> TEMPLATE_ROW_TAGS = Set.of("inflection-template", "table-tags", "class");

    private final String edition;
    private final LabelSource labelSource;

    public HtmlDefinitionRenderer() {
        this("");
    }

    public HtmlDefinitionRenderer(String edition) {
        this.edition = edition == null ? "" : edition;
        this.labelSource = LabelSource.forEdition(this.edition);
    }

    @Override
    public Optional<RenderedEntry> render(WiktionaryEntry entry) {
        SenseOutline senses = SenseOutline.of(entry.senses(), labelSource);
        if (senses.isEmpty()) {
            return Optional.empty();
        }

        StringBuilder sb = new StringBuilder();
        Headline.appendTo(sb, edition, entry);
        senses.appendTo(sb);
        Etymology.appendTo(sb, entry);
        RelatedWords.appendTo(sb, entry);

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

            List<String> senseLemmas = lemmaWords(sense.formOf());
            if (senseLemmas.isEmpty()) {
                // a sense with its own meaning — not a form-of-only entry
                return List.of();
            }
            lemmas.addAll(senseLemmas);
        }

        return hasRenderableSense ? List.copyOf(lemmas) : List.of();
    }

    private static List<String> lemmaWords(List<WiktionaryFormOf> formOf) {
        if (formOf == null || formOf.isEmpty()) {
            return List.of();
        }
        List<String> words = new ArrayList<>(formOf.size());
        for (WiktionaryFormOf ref : formOf) {
            String word = ref == null ? null : ref.word();
            if (word != null && !word.isBlank()) {
                words.add(word.strip());
            }
        }
        return words;
    }

    private static boolean hasRenderableGloss(WiktionarySense sense) {
        for (String gloss : sense.glosses()) {
            if (gloss != null && !gloss.isBlank()) {
                return true;
            }
        }
        return false;
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
            if (text == null || text.isBlank() || isTemplateRow(form)) {
                continue;
            }
            kept.add(form);
        }
        return kept;
    }

    private static boolean isTemplateRow(WiktionaryForm form) {
        return form.tags() != null && form.tags().stream().anyMatch(TEMPLATE_ROW_TAGS::contains);
    }

    private static List<String> collectInflectionForms(List<WiktionaryForm> filtered) {
        if (filtered.isEmpty()) {
            return List.of();
        }
        Set<String> seen = LinkedHashSet.newLinkedHashSet(filtered.size());
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
            sb.append(LIST_ITEM_END);
        }
        sb.append("</ul>");
    }
}
