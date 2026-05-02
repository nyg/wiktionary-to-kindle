package edu.self.w2k.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

class HtmlDefinitionRendererTest {

    private final DefinitionRenderer renderer = new HtmlDefinitionRenderer();

    @Test
    void buildDefinition_basicGloss() {
        var senses = List.of(parseSense("{\"glosses\":[\"a dog\"],\"examples\":[]}"));
        var def = renderer.render(senses);
        assertTrue(def.isPresent());
        assertTrue(def.get().contains("<ol>"), "should wrap in <ol>");
        assertTrue(def.get().contains("<li><span>a dog</span>"), "should contain gloss");
        assertFalse(def.get().contains("<ul>"), "no examples → no <ul>");
    }

    @Test
    void buildDefinition_xmlEscaping() {
        var senses = List.of(parseSense("{\"glosses\":[\"bread & butter <food>\"],\"examples\":[]}"));
        var def = renderer.render(senses);
        assertTrue(def.isPresent());
        assertTrue(def.get().contains("bread &amp; butter &lt;food&gt;"), "XML characters should be escaped");
    }

    @Test
    void buildDefinition_withExample() {
        var senses = List.of(parseSense("{\"glosses\":[\"run\"],\"examples\":[{\"text\":\"He runs fast.\"}]}"));
        var def = renderer.render(senses);
        assertTrue(def.isPresent());
        assertTrue(def.get().contains("<ul>"), "examples present → <ul>");
        assertTrue(def.get().contains("<li>He runs fast.</li>"));
    }

    @Test
    void buildDefinition_stripsNewlines() {
        var senses = List.of(parseSense("{\"glosses\":[\"line1\\nline2\"],\"examples\":[]}"));
        var def = renderer.render(senses);
        assertTrue(def.isPresent());
        assertTrue(def.get().contains("line1; line2"), "newlines should be replaced with '; '");
    }

    @Test
    void buildDefinition_multipleGlossesInOneSense() {
        var senses = List.of(parseSense("{\"glosses\":[\"first meaning\",\"second meaning\"],\"examples\":[]}"));
        var def = renderer.render(senses);
        assertTrue(def.isPresent());
        assertTrue(def.get().contains("first meaning"));
        assertTrue(def.get().contains("second meaning"));
    }

    @Test
    void buildDefinition_nullSensesSkipped() {
        var senses = List.of(parseSense("{\"glosses\":[],\"examples\":[]}"));
        assertTrue(renderer.render(senses).isEmpty(), "form_of-only entries should return empty Optional");
    }

    @Test
    void buildDefinition_blankGlossSkipped() {
        var senses = List.of(parseSense("{\"glosses\":[\"  \"],\"examples\":[]}"));
        assertTrue(renderer.render(senses).isEmpty(), "blank gloss should be skipped → empty Optional");
    }

    @Test
    void buildDefinition_blankExampleSkipped() {
        var senses = List.of(parseSense("{\"glosses\":[\"gloss\"],\"examples\":[{\"text\":\"  \"}]}"));
        var def = renderer.render(senses);
        assertTrue(def.isPresent());
        assertFalse(def.get().contains("<ul>"), "blank example should be skipped → no <ul>");
    }

    @Test
    void buildDefinition_greekContent() {
        var senses = List.of(parseSense("{\"glosses\":[\"σκύλος (dog)\"],\"examples\":[{\"text\":\"Ο σκύλος τρέχει.\"}]}"));
        var def = renderer.render(senses);
        assertTrue(def.isPresent());
        assertTrue(def.get().contains("σκύλος (dog)"), "Greek gloss should pass through unchanged");
        assertTrue(def.get().contains("Ο σκύλος τρέχει."), "Greek example should pass through unchanged");
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
