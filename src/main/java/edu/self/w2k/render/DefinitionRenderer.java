package edu.self.w2k.render;

import edu.self.w2k.model.WiktionarySense;

import java.util.List;
import java.util.Optional;

public interface DefinitionRenderer {

    /**
     * Renders a list of senses into a definition string.
     *
     * @return the rendered definition, or an empty Optional if the entry has no renderable glosses
     */
    Optional<String> render(List<WiktionarySense> senses);
}
