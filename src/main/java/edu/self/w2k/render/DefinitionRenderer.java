package edu.self.w2k.render;

import java.util.List;
import java.util.Optional;

import edu.self.w2k.model.WiktionarySense;

public interface DefinitionRenderer {
    /**
     * Renders a list of senses into a definition string.
     *
     * @return the rendered definition, or an empty Optional if the entry has no renderable glosses
     */
    Optional<String> render(List<WiktionarySense> senses);
}
