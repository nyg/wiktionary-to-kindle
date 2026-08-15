package edu.self.w2k.kaikki;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KaikkiHtmlTest {

    @Test
    void should_read_every_edition_code_when_given_the_homepage() {
        // Given
        String html = """
                <li><a href="dictionary/">List of all machine-readable dictionaries</a></li>
                <li><a href="zhwiktionary/">Chinese wiktionary edition (zhwiktionary)</a></li>
                <li><a href="frwiktionary/">French wiktionary edition (frwiktionary)</a></li>
                <li><a href="cswiktionary/">Czech wiktionary edition (cswiktionary)</a></li>
                """;

        // When
        List<String> editions = KaikkiHtml.parseEditions(html);

        // Then
        assertThat(editions).containsExactly("cs", "en", "fr", "zh");
    }

    @Test
    void should_read_a_multi_letter_edition_code_when_the_edition_is_simple_english() {
        // Given
        String html = """
                <li><a href="simplewiktionary/">Simple English wiktionary edition (simplewiktionary)</a></li>
                """;

        // When
        List<String> editions = KaikkiHtml.parseEditions(html);

        // Then
        assertThat(editions).containsExactly("simple");
    }

    @Test
    void should_ignore_per_language_links_when_they_sit_under_the_dictionary_path() {
        // Given
        String html = """
                <li><a href="dictionary/Arabic/">Arabic</a></li>
                <li><a href="dictionary/rawdata.html">Raw data</a></li>
                """;

        // When
        List<String> editions = KaikkiHtml.parseEditions(html);

        // Then
        assertThat(editions).isEmpty();
    }

    @Test
    void should_read_names_and_sense_counts_when_given_an_edition_page() {
        // Given
        String html = """
                <li><a href="Fran%C3%A7ais/index.html">Français (2653194 senses)</a></li>
                <li><a href="Allemand/index.html">Allemand (2200333 senses)</a></li>
                """;

        // When
        List<KaikkiLanguage> languages = KaikkiHtml.parseLanguages(html);

        // Then
        assertThat(languages).containsExactly(
                new KaikkiLanguage("Français", 2653194),
                new KaikkiLanguage("Allemand", 2200333));
    }

    @Test
    void should_keep_the_whole_name_when_the_name_itself_contains_parentheses() {
        // Given
        String html = """
                <li><a href="Norv%C3%A9gien%20%28bokm%C3%A5l%29/index.html">Norvégien (bokmål) (1722 senses)</a></li>
                """;

        // When
        List<KaikkiLanguage> languages = KaikkiHtml.parseLanguages(html);

        // Then
        assertThat(languages).containsExactly(new KaikkiLanguage("Norvégien (bokmål)", 1722));
    }

    @Test
    void should_decode_html_entities_when_a_name_contains_an_apostrophe() {
        // Given
        String html = """
                <li><a href="K%27iche%27/index.html">K&#x27;iche&#x27; (505 senses)</a></li>
                """;

        // When
        List<KaikkiLanguage> languages = KaikkiHtml.parseLanguages(html);

        // Then
        assertThat(languages).containsExactly(new KaikkiLanguage("K'iche'", 505));
    }

    @Test
    void should_drop_the_combined_pseudo_language_when_it_is_not_listed_first() {
        // Given
        String html = """
                <li><a href="English/index.html">English (72625 senses)</a></li>
                <li><a href="All%20languages%20combined/index.html">All languages combined (72625 senses)</a></li>
                """;

        // When
        List<KaikkiLanguage> languages = KaikkiHtml.parseLanguages(html);

        // Then
        assertThat(languages).containsExactly(new KaikkiLanguage("English", 72625));
    }

    @Test
    void should_ignore_breadcrumb_links_when_they_carry_no_sense_count() {
        // Given
        String html = """
                <ul class="breadcrumb"><li><a href="https://kaikki.org/index.html">Home</a></li></ul>
                <li><a href="Latin/index.html">Latin (1008452 senses)</a></li>
                """;

        // When
        List<KaikkiLanguage> languages = KaikkiHtml.parseLanguages(html);

        // Then
        assertThat(languages).containsExactly(new KaikkiLanguage("Latin", 1008452));
    }

    @Test
    void should_return_nothing_when_the_markup_no_longer_matches() {
        // Given
        String html = "<div class=\"languages\"><span data-lang=\"fr\">Français</span></div>";

        // When / Then
        assertThat(KaikkiHtml.parseLanguages(html)).isEmpty();
        assertThat(KaikkiHtml.parseEditions(html)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void should_return_nothing_when_the_page_is_absent(String html) {
        // When / Then
        assertThat(KaikkiHtml.parseEditions(html)).isEmpty();
        assertThat(KaikkiHtml.parseLanguages(html)).isEmpty();
    }

    @Test
    void should_route_english_to_the_dictionary_path_when_building_an_edition_url() {
        // When / Then
        assertThat(KaikkiHtml.editionPath("en")).isEqualTo("dictionary");
        assertThat(KaikkiHtml.editionPath("fr")).isEqualTo("frwiktionary");
        assertThat(KaikkiHtml.editionPath("simple")).isEqualTo("simplewiktionary");
    }
}
