package edu.self.w2k.kaikki;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

public final class KaikkiHtml {

    private static final Pattern EDITION_HREF = Pattern.compile("href=\"([a-z]+)wiktionary/\"");

    private static final String ENGLISH_EDITION_HREF = "href=\"dictionary/\"";

    private static final Pattern LANGUAGE_ENTRY = Pattern.compile(
            "<li><a href=\"[^\"]*/index\\.html\">(.+) \\((\\d+) senses?\\)</a></li>");

    private static final String COMBINED_PSEUDO_LANGUAGE = "All languages combined";

    private KaikkiHtml() {}

    public static List<String> parseEditions(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        if (html.contains(ENGLISH_EDITION_HREF)) {
            codes.add("en");
        }
        Matcher matcher = EDITION_HREF.matcher(html);
        while (matcher.find()) {
            codes.add(matcher.group(1));
        }
        return codes.stream().sorted().toList();
    }

    public static List<KaikkiLanguage> parseLanguages(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<KaikkiLanguage> languages = new ArrayList<>();
        for (String line : html.lines().toList()) {
            Matcher matcher = LANGUAGE_ENTRY.matcher(line.strip());
            if (!matcher.matches()) {
                continue;
            }
            String name = StringEscapeUtils.unescapeHtml4(matcher.group(1)).strip();
            if (name.isEmpty() || name.equals(COMBINED_PSEUDO_LANGUAGE)) {
                continue;
            }
            languages.add(new KaikkiLanguage(name, parseSenses(matcher.group(2))));
        }
        return List.copyOf(languages);
    }

    public static String editionPath(String edition) {
        return "en".equals(edition) ? "dictionary" : edition + "wiktionary";
    }

    private static long parseSenses(String digits) {
        try {
            return Long.parseLong(digits);
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }
}
