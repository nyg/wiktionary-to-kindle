package edu.self.w2k.render;

import java.util.List;
import java.util.Optional;

import edu.self.w2k.model.WiktionaryExample;
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

        // When
        Optional<String> result = unit.render(List.of(sense));

        // Then
        assertThat(result).isPresent();
        String html = result.get();
        assertThat(html)
                .startsWith("<ol>")
                .contains("<li><span>")
                .contains("A round fruit")
                .contains("<ul>")
                .contains("<li>The apple fell from the tree.</li>")
                .endsWith("</ol>");
    }

    @Test
    void should_return_empty_when_no_glosses_have_content() {
        // Given
        WiktionarySense emptyGloss = new WiktionarySense(List.of("", "  "), List.of());
        WiktionarySense nullGloss = new WiktionarySense(List.of(), List.of());

        // When
        Optional<String> result = unit.render(List.of(emptyGloss, nullGloss));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_escape_xml_and_replace_newlines_when_rendering() {
        // Given
        WiktionarySense sense = new WiktionarySense(List.of("A & B\nC"), List.of());

        // When
        Optional<String> result = unit.render(List.of(sense));

        // Then
        assertThat(result).isPresent();
        String html = result.get();
        assertThat(html)
                .contains("&amp;")
                .contains("; ")
                .doesNotContain("\n");
    }
}
