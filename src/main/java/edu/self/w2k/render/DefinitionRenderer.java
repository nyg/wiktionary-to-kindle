package edu.self.w2k.render;

import edu.self.w2k.model.WiktionarySense;

import java.util.List;

public interface DefinitionRenderer {

    /**
     * Renders a list of senses into a definition string.
     *
     * @return the rendered definition, or {@code null} if the entry has no renderable glosses
     */
    String render(List<WiktionarySense> senses);
}
