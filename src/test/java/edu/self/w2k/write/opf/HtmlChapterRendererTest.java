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
                Map.entry("apple", List.of(new LexiconEntry("apple", "<ol><li>fruit</li></ol>"))),
                Map.entry("banana", List.of(new LexiconEntry("banana", "<ol><li>tropical fruit</li></ol>")))
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
                        new LexiconEntry("word", "def1"),
                        new LexiconEntry("word", "def2")
                ))
        );

        // When
        byte[] bytes = HtmlChapterRenderer.render(entries);
        String html = new String(bytes, StandardCharsets.UTF_8);

        // Then
        assertThat(html).contains("def1; def2");
    }
}
