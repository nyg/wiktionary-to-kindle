package edu.self.w2k.render;

import java.util.Optional;

import edu.self.w2k.model.WiktionaryEntry;

public interface DefinitionRenderer {
    /**
     * Renders a Wiktionary entry into a definition HTML string and an ordered list
     * of inflected forms to expose for tap-to-lookup.
     *
     * @return the rendered entry, or an empty Optional if the entry has no renderable glosses
     */
    Optional<RenderedEntry> render(WiktionaryEntry entry);
}
