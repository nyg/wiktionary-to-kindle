package edu.self.w2k.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.model.WiktionaryExample;
import edu.self.w2k.model.WiktionaryForm;
import edu.self.w2k.model.WiktionarySense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HtmlDefinitionRendererTest {

    private final HtmlDefinitionRenderer unit = new HtmlDefinitionRenderer();

    @Test
    void should_render_html_with_glosses_and_examples_when_senses_have_content() {
        // Given
        WiktionaryExample example = new WiktionaryExample("The apple fell from the tree.");
        WiktionarySense sense = new WiktionarySense(List.of("A round fruit"), List.of(example));
        WiktionaryEntry entry = new WiktionaryEntry("apple", "en", "noun", List.of(sense), List.of());

        // When
        Optional<RenderedEntry> result = unit.render(entry);

        // Then
        assertThat(result).isPresent();
        String html = result.get().html();
        assertThat(html)
                .startsWith("<ol>")
                .contains("<li><span>")
                .contains("A round fruit")
                .contains("<ul>")
                .contains("<li>The apple fell from the tree.</li>");
        assertThat(result.get().inflectionForms()).isEmpty();
    }

    @Test
    void should_return_empty_when_no_glosses_have_content() {
        // Given
        WiktionarySense emptyGloss = new WiktionarySense(List.of("", "  "), List.of());
        WiktionarySense nullGloss = new WiktionarySense(List.of(), List.of());
        WiktionaryEntry entry = new WiktionaryEntry("x", "en", "noun", List.of(emptyGloss, nullGloss), List.of());

        // When
        Optional<RenderedEntry> result = unit.render(entry);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_escape_xml_and_replace_newlines_when_rendering() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("A & B\nC"), List.of());
        WiktionaryEntry entry = new WiktionaryEntry("x", "en", "noun", List.of(sense), List.of());

        // When
        Optional<RenderedEntry> result = unit.render(entry);

        // Then
        assertThat(result).isPresent();
        String html = result.get().html();
        assertThat(html)
                .contains("&amp;")
                .contains("; ")
                .doesNotContain("\n");
    }

    @Test
    void should_render_visible_forms_table_when_pos_is_noun() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("Compagnon."), List.of());
        WiktionaryForm pluralNom = new WiktionaryForm("σύντροφοι", List.of("plural", "nominative"), "οι", null);
        WiktionaryForm singularGen = new WiktionaryForm("συντρόφου", List.of("singular", "genitive"), "του", null);
        WiktionaryEntry entry = new WiktionaryEntry("σύντροφος", "el", "noun",
                List.of(sense), List.of(pluralNom, singularGen));

        // When
        Optional<RenderedEntry> result = unit.render(entry);

        // Then
        assertThat(result).isPresent();
        String html = result.get().html();
        assertThat(html)
                .contains("<p><i>Forms:</i></p>")
                .contains("<li>pl. nom.: οι σύντροφοι</li>")
                .contains("<li>sg. gen.: του συντρόφου</li>");
        assertThat(result.get().inflectionForms())
                .containsExactly("σύντροφοι", "συντρόφου");
    }

    @Test
    void should_skip_visible_forms_table_when_pos_is_verb() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("Avoir."), List.of());
        WiktionaryForm form = new WiktionaryForm("είχα", List.of("singular", "first-person"), null, null);
        WiktionaryEntry entry = new WiktionaryEntry("έχω", "el", "verb", List.of(sense), List.of(form));

        // When
        Optional<RenderedEntry> result = unit.render(entry);

        // Then
        assertThat(result).isPresent();
        String html = result.get().html();
        assertThat(html).doesNotContain("Forms:");
        // iform list still returned for verb lookup
        assertThat(result.get().inflectionForms()).containsExactly("είχα");
    }

    @Test
    void should_skip_visible_forms_table_when_more_than_threshold_forms() {
        // Given
        List<WiktionaryForm> manyForms = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            manyForms.add(new WiktionaryForm("form" + i, List.of("plural"), null, null));
        }
        WiktionarySense sense = new WiktionarySense(List.of("Definition."), List.of());
        WiktionaryEntry entry = new WiktionaryEntry("x", "el", "noun", List.of(sense), manyForms);

        // When
        Optional<RenderedEntry> result = unit.render(entry);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().html()).doesNotContain("Forms:");
        assertThat(result.get().inflectionForms()).hasSize(31);
    }

    @Test
    void should_filter_equiv_pour_forms_from_both_iform_and_visible_table() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("Compagnon."), List.of());
        WiktionaryForm trueInflection = new WiktionaryForm("σύντροφοι", List.of("plural", "nominative"), "οι", null);
        WiktionaryForm equivPour = new WiktionaryForm("συντρόφισσα", List.of("feminine"),
                null, "form line template 'équiv-pour'");
        WiktionaryEntry entry = new WiktionaryEntry("σύντροφος", "el", "noun",
                List.of(sense), List.of(trueInflection, equivPour));

        // When
        Optional<RenderedEntry> result = unit.render(entry);

        // Then
        assertThat(result).isPresent();
        String html = result.get().html();
        assertThat(html).contains("σύντροφοι").doesNotContain("συντρόφισσα");
        assertThat(result.get().inflectionForms()).containsExactly("σύντροφοι");
    }

    @Test
    void should_skip_visible_table_when_forms_empty() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("Definition."), List.of());
        WiktionaryEntry entry = new WiktionaryEntry("x", "en", "noun", List.of(sense), List.of());

        // When
        Optional<RenderedEntry> result = unit.render(entry);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().html()).doesNotContain("Forms:");
        assertThat(result.get().inflectionForms()).isEmpty();
    }

    @Test
    void should_dedupe_inflection_forms_preserving_order() {
        // Given (kaikki may list the same form twice with different tag combinations)
        WiktionarySense sense = new WiktionarySense(List.of("Definition."), List.of());
        WiktionaryForm a = new WiktionaryForm("σύντροφοι", List.of("plural", "nominative"), "οι", null);
        WiktionaryForm b = new WiktionaryForm("σύντροφοι", List.of("plural", "vocative"), null, null);
        WiktionaryEntry entry = new WiktionaryEntry("σύντροφος", "el", "noun", List.of(sense), List.of(a, b));

        // When
        Optional<RenderedEntry> result = unit.render(entry);

        // Then — iform list deduplicated
        assertThat(result).isPresent();
        assertThat(result.get().inflectionForms()).containsExactly("σύντροφοι");
        // Visible table keeps both rows so the reader sees both functions
        assertThat(result.get().html())
                .contains("pl. nom.")
                .contains("pl. voc.");
    }

    @Test
    void should_xml_escape_form_article_and_tag_in_visible_table() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("Definition."), List.of());
        WiktionaryForm form = new WiktionaryForm("a&b", List.of("custom<tag>"), "<art>", null);
        WiktionaryEntry entry = new WiktionaryEntry("x", "en", "noun", List.of(sense), List.of(form));

        // When
        Optional<RenderedEntry> result = unit.render(entry);

        // Then
        assertThat(result).isPresent();
        String html = result.get().html();
        assertThat(html)
                .contains("a&amp;b")
                .contains("&lt;art&gt;")
                .contains("custom&lt;tag&gt;");
    }
}
