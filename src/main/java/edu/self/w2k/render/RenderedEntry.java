package edu.self.w2k.render;

import java.util.List;
import java.util.Map;

public record RenderedEntry(String html,
                            List<String> inflectionForms,
                            List<String> formOfLemmas,
                            Map<String, List<String>> counterpartForms) {

    public RenderedEntry(String html, List<String> inflectionForms, List<String> formOfLemmas) {
        this(html, inflectionForms, formOfLemmas, Map.of());
    }
}
