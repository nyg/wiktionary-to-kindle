package edu.self.w2k.render;

import java.util.List;
import java.util.Map;

public final class InflectionTagAbbreviator {

    private static final Map<String, String> ABBREV = Map.ofEntries(
            Map.entry("singular", "sg."),
            Map.entry("plural", "pl."),
            Map.entry("nominative", "nom."),
            Map.entry("genitive", "gen."),
            Map.entry("accusative", "acc."),
            Map.entry("dative", "dat."),
            Map.entry("vocative", "voc."),
            Map.entry("masculine", "m."),
            Map.entry("feminine", "f."),
            Map.entry("neuter", "n."));

    private InflectionTagAbbreviator() {}

    public static String abbreviate(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(ABBREV.getOrDefault(tag, tag));
        }
        return sb.toString();
    }
}
