package edu.self.w2k.render;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.model.WiktionarySense;
import edu.self.w2k.model.WiktionarySound;

final class Headline {

    private static final Map<String, String> GRAMMAR = Map.ofEntries(
            Map.entry("masculine", "m."),
            Map.entry("feminine", "f."),
            Map.entry("neuter", "n."),
            Map.entry("common", "c."),
            Map.entry("common-gender", "c."),
            Map.entry("plural", "pl."),
            Map.entry("invariable", "inv."),
            Map.entry("transitive", "tr."),
            Map.entry("intransitive", "intr."),
            Map.entry("perfective", "pf."),
            Map.entry("imperfective", "impf."));

    private static final String IPA_DELIMITERS = "/[\\";

    private Headline() {}

    static void appendTo(StringBuilder sb, String edition, WiktionaryEntry entry) {
        List<String> grammar = new ArrayList<>();
        String pos = PartOfSpeech.label(edition, entry.pos(), entry.posTitle());
        if (!pos.isEmpty()) {
            grammar.add(pos);
        }
        String abbreviations = String.join(" ", grammarOf(entry));
        if (!abbreviations.isEmpty()) {
            grammar.add(abbreviations);
        }
        String pronunciation = pronunciationOf(entry.sounds());
        if (grammar.isEmpty() && pronunciation.isEmpty()) {
            return;
        }

        sb.append("<div>");
        if (!grammar.isEmpty()) {
            sb.append("<i>").append(HtmlText.escape(String.join(", ", grammar))).append("</i>");
        }
        if (!grammar.isEmpty() && !pronunciation.isEmpty()) {
            sb.append(' ');
        }
        sb.append(HtmlText.escape(pronunciation)).append("</div>");
    }

    private static Set<String> grammarOf(WiktionaryEntry entry) {
        List<String> tags = entry.tags().isEmpty() ? tagsSharedByEverySense(entry.senses()) : entry.tags();
        Set<String> abbreviations = new LinkedHashSet<>();
        for (String tag : tags) {
            String abbreviation = tag == null ? null : GRAMMAR.get(tag);
            if (abbreviation != null) {
                abbreviations.add(abbreviation);
            }
        }
        return abbreviations;
    }

    // The English edition has no entry-level tags: a noun's gender is repeated on each of its senses.
    private static List<String> tagsSharedByEverySense(List<WiktionarySense> senses) {
        List<String> shared = null;
        for (WiktionarySense sense : senses) {
            if (sense == null || sense.glosses().isEmpty()) {
                continue;
            }
            if (shared == null) {
                shared = new ArrayList<>(sense.tags());
            }
            else {
                shared.retainAll(sense.tags());
            }
        }
        return shared == null ? List.of() : shared;
    }

    private static String pronunciationOf(List<WiktionarySound> sounds) {
        for (WiktionarySound sound : sounds) {
            if (sound == null || HtmlText.isBlank(sound.ipa())) {
                continue;
            }
            String ipa = sound.ipa().strip();
            return IPA_DELIMITERS.indexOf(ipa.charAt(0)) >= 0 ? ipa : "/" + ipa + "/";
        }
        return "";
    }
}
