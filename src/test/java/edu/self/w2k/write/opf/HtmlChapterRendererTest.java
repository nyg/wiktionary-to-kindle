package edu.self.w2k.write.opf;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import edu.self.w2k.model.LexiconEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HtmlChapterRendererTest {

    @Test
    void should_render_kindle_idx_entries_when_called() {
        // Given
        List<Map.Entry<String, List<LexiconEntry>>> entries = List.of(
                Map.entry("apple", List.of(new LexiconEntry("apple", "<ol><li>fruit</li></ol>", List.of()))),
                Map.entry("banana", List.of(new LexiconEntry("banana", "<ol><li>tropical fruit</li></ol>", List.of())))
        );

        // When
        byte[] bytes = HtmlChapterRenderer.render(entries);
        String html = new String(bytes, StandardCharsets.UTF_8);

        // Then
        assertThat(html)
                .contains("xmlns:idx")
                .contains("xmlns:mbp")
                .contains("<idx:entry name=\"word\"")
                .contains("<idx:orth value=\"apple\">")
                .contains("<b>apple</b>");
    }

    @Test
    void should_combine_definitions_with_semicolons_when_multiple_entries_share_key() {
        // Given
        List<Map.Entry<String, List<LexiconEntry>>> entries = List.of(
                Map.entry("word", List.of(
                        new LexiconEntry("word", "def1", List.of()),
                        new LexiconEntry("word", "def2", List.of())
                ))
        );

        // When
        byte[] bytes = HtmlChapterRenderer.render(entries);
        String html = new String(bytes, StandardCharsets.UTF_8);

        // Then
        assertThat(html).contains("def1; def2");
    }

    @Test
    void should_emit_idx_infl_block_inside_idx_orth_when_inflection_forms_present() {
        // Given
        List<Map.Entry<String, List<LexiconEntry>>> entries = List.of(
                Map.entry("σύντροφος", List.of(new LexiconEntry(
                        "σύντροφος",
                        "<ol><li>Compagnon.</li></ol>",
                        List.of("σύντροφοι", "συντρόφου", "συντρόφους"))))
        );

        // When
        byte[] bytes = HtmlChapterRenderer.render(entries);
        String html = new String(bytes, StandardCharsets.UTF_8);

        // Then
        assertThat(html)
                .contains("<idx:orth value=\"σύντροφος\"><b>σύντροφος</b><idx:infl>")
                .contains("<idx:iform value=\"σύντροφοι\"/>")
                .contains("<idx:iform value=\"συντρόφου\"/>")
                .contains("<idx:iform value=\"συντρόφους\"/>")
                .contains("</idx:infl></idx:orth>");
    }

    @Test
    void should_omit_idx_infl_when_inflection_forms_empty() {
        // Given
        List<Map.Entry<String, List<LexiconEntry>>> entries = List.of(
                Map.entry("apple", List.of(new LexiconEntry("apple", "<ol><li>fruit</li></ol>", List.of())))
        );

        // When
        byte[] bytes = HtmlChapterRenderer.render(entries);
        String html = new String(bytes, StandardCharsets.UTF_8);

        // Then
        assertThat(html).doesNotContain("<idx:infl");
    }

    @Test
    void should_xml_escape_iform_values_when_emitting() {
        // Given
        List<Map.Entry<String, List<LexiconEntry>>> entries = List.of(
                Map.entry("x", List.of(new LexiconEntry("x", "<ol><li>def</li></ol>", List.of("a&b", "<c>"))))
        );

        // When
        byte[] bytes = HtmlChapterRenderer.render(entries);
        String html = new String(bytes, StandardCharsets.UTF_8);

        // Then
        assertThat(html)
                .contains("<idx:iform value=\"a&amp;b\"/>")
                .contains("<idx:iform value=\"&lt;c&gt;\"/>");
    }

    @Test
    void should_union_inflection_forms_with_dedup_when_homograph_collision() {
        // Given — two LexiconEntrys for the same headword with overlapping forms
        List<Map.Entry<String, List<LexiconEntry>>> entries = List.of(
                Map.entry("apple", List.of(
                        new LexiconEntry("apple", "def1", List.of("apples", "apple's")),
                        new LexiconEntry("apple", "def2", List.of("apples", "appling"))
                ))
        );

        // When
        byte[] bytes = HtmlChapterRenderer.render(entries);
        String html = new String(bytes, StandardCharsets.UTF_8);

        // Then — apples appears once, in original order, plus apple's then appling
        int firstApples = html.indexOf("<idx:iform value=\"apples\"/>");
        int secondApples = html.indexOf("<idx:iform value=\"apples\"/>", firstApples + 1);
        assertThat(firstApples).isPositive();
        assertThat(secondApples).isEqualTo(-1);
        assertThat(html)
                .contains("<idx:iform value=\"apple&apos;s\"/>")
                .contains("<idx:iform value=\"appling\"/>");
    }
}
