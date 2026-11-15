package edu.self.w2k.render;

import java.util.Map;
import java.util.regex.Pattern;

final class PartOfSpeech {

    private static final Pattern HOMOGRAPH_NUMBER = Pattern.compile("\\s+\\d+$");

    private static final Map<String, String> ENGLISH = Map.ofEntries(
            Map.entry("adj", "adjective"),
            Map.entry("adv", "adverb"),
            Map.entry("name", "proper noun"),
            Map.entry("pron", "pronoun"),
            Map.entry("prep", "preposition"),
            Map.entry("postp", "postposition"),
            Map.entry("conj", "conjunction"),
            Map.entry("intj", "interjection"),
            Map.entry("interj", "interjection"),
            Map.entry("num", "numeral"),
            Map.entry("det", "determiner"),
            Map.entry("abbrev", "abbreviation"));

    private static final Map<String, String> GREEK = Map.ofEntries(
            Map.entry("noun", "ουσιαστικό"),
            Map.entry("name", "κύριο όνομα"),
            Map.entry("verb", "ρήμα"),
            Map.entry("adj", "επίθετο"),
            Map.entry("adv", "επίρρημα"),
            Map.entry("pron", "αντωνυμία"),
            Map.entry("prep", "πρόθεση"),
            Map.entry("conj", "σύνδεσμος"),
            Map.entry("intj", "επιφώνημα"),
            Map.entry("num", "αριθμητικό"),
            Map.entry("article", "άρθρο"),
            Map.entry("particle", "μόριο"),
            Map.entry("abbrev", "συντομογραφία"));

    private static final Map<String, String> RUSSIAN = Map.ofEntries(
            Map.entry("noun", "существительное"),
            Map.entry("name", "имя собственное"),
            Map.entry("verb", "глагол"),
            Map.entry("adj", "прилагательное"),
            Map.entry("adv", "наречие"),
            Map.entry("pron", "местоимение"),
            Map.entry("prep", "предлог"),
            Map.entry("conj", "союз"),
            Map.entry("intj", "междометие"),
            Map.entry("interj", "междометие"),
            Map.entry("num", "числительное"),
            Map.entry("particle", "частица"),
            Map.entry("abbrev", "сокращение"));

    // The editions whose dumps carry no pos_title, and so no part of speech in their own language.
    private static final Map<String, Map<String, String>> BY_EDITION = Map.of("el", GREEK, "ru", RUSSIAN);

    private PartOfSpeech() {}

    static String label(String edition, String pos, String posTitle) {
        if (!HtmlText.isBlank(posTitle)) {
            return HOMOGRAPH_NUMBER.matcher(posTitle.strip()).replaceFirst("");
        }
        if (HtmlText.isBlank(pos)) {
            return "";
        }
        String localised = BY_EDITION.getOrDefault(edition, Map.of()).get(pos);
        return localised != null ? localised : ENGLISH.getOrDefault(pos, pos);
    }
}
