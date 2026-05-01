package edu.self.w2k.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlDefinitionRendererTest {

    private final DefinitionRenderer renderer = new HtmlDefinitionRenderer();

    @Test
    void buildDefinition_basicGloss() {
        var senses = List.of(parseSense("{\"glosses\":[\"a dog\"],\"examples\":[]}"));
        String def = renderer.render(senses);
        assertNotNull(def);
        assertTrue(def.contains("<ol>"), "should wrap in <ol>");
        assertTrue(def.contains("<li><span>a dog</span>"), "should contain gloss");
        assertFalse(def.contains("<ul>"), "no examples → no <ul>");
    }

    @Test
    void buildDefinition_xmlEscaping() {
        var senses = List.of(parseSense("{\"glosses\":[\"bread & butter <food>\"],\"examples\":[]}"));
        String def = renderer.render(senses);
        assertNotNull(def);
        assertTrue(def.contains("bread &amp; butter &lt;food&gt;"), "XML characters should be escaped");
    }

    @Test
    void buildDefinition_withExample() {
        var senses = List.of(parseSense("{\"glosses\":[\"run\"],\"examples\":[{\"text\":\"He runs fast.\"}]}"));
        String def = renderer.render(senses);
        assertNotNull(def);
        assertTrue(def.contains("<ul>"), "examples present → <ul>");
        assertTrue(def.contains("<li>He runs fast.</li>"));
    }

    @Test
    void buildDefinition_stripsNewlines() {
        var senses = List.of(parseSense("{\"glosses\":[\"line1\\nline2\"],\"examples\":[]}"));
        String def = renderer.render(senses);
        assertNotNull(def);
        assertTrue(def.contains("line1; line2"), "newlines should be replaced with '; '");
    }

    @Test
    void buildDefinition_multipleGlossesInOneSense() {
        var senses = List.of(parseSense("{\"glosses\":[\"first meaning\",\"second meaning\"],\"examples\":[]}"));
        String def = renderer.render(senses);
        assertNotNull(def);
        assertTrue(def.contains("first meaning"));
        assertTrue(def.contains("second meaning"));
    }

    @Test
    void buildDefinition_nullSensesSkipped() {
        var senses = List.of(parseSense("{\"glosses\":[],\"examples\":[]}"));
        assertNull(renderer.render(senses), "form_of-only entries should return null");
    }

    @Test
    void buildDefinition_blankGlossSkipped() {
        var senses = List.of(parseSense("{\"glosses\":[\"  \"],\"examples\":[]}"));
        assertNull(renderer.render(senses), "blank gloss should be skipped → null");
    }

    @Test
    void buildDefinition_blankExampleSkipped() {
        var senses = List.of(parseSense("{\"glosses\":[\"gloss\"],\"examples\":[{\"text\":\"  \"}]}"));
        String def = renderer.render(senses);
        assertNotNull(def);
        assertFalse(def.contains("<ul>"), "blank example should be skipped → no <ul>");
    }

    @Test
    void buildDefinition_greekContent() {
        var senses = List.of(parseSense("{\"glosses\":[\"σκύλος (dog)\"],\"examples\":[{\"text\":\"Ο σκύλος τρέχει.\"}]}"));
        String def = renderer.render(senses);
        assertNotNull(def);
        assertTrue(def.contains("σκύλος (dog)"), "Greek gloss should pass through unchanged");
        assertTrue(def.contains("Ο σκύλος τρέχει."), "Greek example should pass through unchanged");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static edu.self.w2k.model.WiktionarySense parseSense(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readerFor(edu.self.w2k.model.WiktionarySense.class)
                    .readValue(json);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
