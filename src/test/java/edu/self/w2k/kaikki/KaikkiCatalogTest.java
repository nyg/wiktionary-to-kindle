package edu.self.w2k.kaikki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KaikkiCatalogTest {

    @TempDir
    Path cacheRoot;

    @Mock
    PageFetcher fetcher;

    @Test
    void should_fetch_and_cache_editions_when_nothing_is_cached() throws Exception {
        // Given
        when(fetcher.fetch(any())).thenReturn("""
                <li><a href="dictionary/">Dictionaries</a></li>
                <li><a href="frwiktionary/">French wiktionary edition (frwiktionary)</a></li>
                """);
        KaikkiCatalog unit = new KaikkiCatalog(fetcher, cacheRoot, Duration.ofDays(7));

        // When
        List<String> editions = unit.editions();

        // Then
        assertThat(editions).containsExactly("en", "fr");
        assertThat(cacheRoot.resolve("editions.txt")).content(StandardCharsets.UTF_8).contains("fr");
    }

    @Test
    void should_serve_the_cache_without_fetching_when_it_is_inside_the_ttl() throws Exception {
        // Given
        Files.writeString(cacheRoot.resolve("editions.txt"), "de\nel\n", StandardCharsets.UTF_8);
        KaikkiCatalog unit = new KaikkiCatalog(fetcher, cacheRoot, Duration.ofDays(7));

        // When
        List<String> editions = unit.editions();

        // Then
        assertThat(editions).containsExactly("de", "el");
        verify(fetcher, never()).fetch(any());
    }

    @Test
    void should_refetch_when_the_cache_is_older_than_the_ttl() throws Exception {
        // Given
        Path cacheFile = cacheRoot.resolve("editions.txt");
        Files.writeString(cacheFile, "de\n", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(cacheFile, FileTime.from(Instant.now().minus(Duration.ofDays(30))));
        when(fetcher.fetch(any())).thenReturn("<li><a href=\"plwiktionary/\">Polish</a></li>");
        KaikkiCatalog unit = new KaikkiCatalog(fetcher, cacheRoot, Duration.ofDays(7));

        // When
        List<String> editions = unit.editions();

        // Then
        assertThat(editions).containsExactly("pl");
    }

    @Test
    void should_fall_back_to_a_stale_cache_when_the_fetch_fails() throws Exception {
        // Given
        Path cacheFile = cacheRoot.resolve("editions.txt");
        Files.writeString(cacheFile, "de\nfr\n", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(cacheFile, FileTime.from(Instant.now().minus(Duration.ofDays(30))));
        when(fetcher.fetch(any())).thenThrow(new IOException("offline"));
        KaikkiCatalog unit = new KaikkiCatalog(fetcher, cacheRoot, Duration.ofDays(7));

        // When
        List<String> editions = unit.editions();

        // Then
        assertThat(editions).containsExactly("de", "fr");
    }

    @Test
    void should_return_nothing_when_the_fetch_fails_and_no_cache_exists() throws Exception {
        // Given
        when(fetcher.fetch(any())).thenThrow(new IOException("offline"));
        KaikkiCatalog unit = new KaikkiCatalog(fetcher, cacheRoot, Duration.ofDays(7));

        // When
        List<String> editions = unit.editions();

        // Then
        assertThat(editions).isEmpty();
    }

    @Test
    void should_keep_the_cache_when_the_page_parses_to_nothing() throws Exception {
        // Given
        Path cacheFile = cacheRoot.resolve("editions.txt");
        Files.writeString(cacheFile, "de\n", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(cacheFile, FileTime.from(Instant.now().minus(Duration.ofDays(30))));
        when(fetcher.fetch(any())).thenReturn("<html><body>redesigned</body></html>");
        KaikkiCatalog unit = new KaikkiCatalog(fetcher, cacheRoot, Duration.ofDays(7));

        // When
        List<String> editions = unit.editions();

        // Then
        assertThat(editions).containsExactly("de");
        assertThat(cacheFile).content(StandardCharsets.UTF_8).isEqualTo("de\n");
    }

    @Test
    void should_round_trip_names_and_sense_counts_through_the_language_cache() throws Exception {
        // Given
        when(fetcher.fetch(any())).thenReturn("""
                <li><a href="Fran%C3%A7ais/index.html">Français (2653194 senses)</a></li>
                """);
        KaikkiCatalog unit = new KaikkiCatalog(fetcher, cacheRoot, Duration.ofDays(7));
        unit.languagesFor("fr");

        // When
        List<KaikkiLanguage> cached = new KaikkiCatalog(fetcher, cacheRoot, Duration.ofDays(7))
                .languagesFor("fr");

        // Then
        assertThat(cached).containsExactly(new KaikkiLanguage("Français", 2653194));
        verify(fetcher).fetch(URI.create("https://kaikki.org/frwiktionary/"));
    }

    @Test
    void should_skip_cache_lines_that_no_longer_parse() throws Exception {
        // Given
        Files.writeString(cacheRoot.resolve("languages-fr.txt"),
                "Français\t2653194\nbroken line with no count\nAllemand\tnot-a-number\n\nRusse\t363095\n",
                StandardCharsets.UTF_8);
        KaikkiCatalog unit = new KaikkiCatalog(fetcher, cacheRoot, Duration.ofDays(7));

        // When
        List<KaikkiLanguage> languages = unit.languagesFor("fr");

        // Then
        assertThat(languages).containsExactly(
                new KaikkiLanguage("Français", 2653194),
                new KaikkiLanguage("Russe", 363095));
    }

    @Test
    void should_drop_cached_edition_codes_that_are_not_plausible() throws Exception {
        // Given
        Files.writeString(cacheRoot.resolve("editions.txt"), "fr\n../etc\nde\n", StandardCharsets.UTF_8);
        KaikkiCatalog unit = new KaikkiCatalog(fetcher, cacheRoot, Duration.ofDays(7));

        // When
        List<String> editions = unit.editions();

        // Then
        assertThat(editions).containsExactly("fr", "de");
    }

    @Test
    void should_still_return_the_fetched_list_when_the_cache_cannot_be_written() throws Exception {
        // Given
        Path unwritable = cacheRoot.resolve("blocked");
        Files.writeString(unwritable, "not a directory");
        when(fetcher.fetch(any())).thenReturn("<li><a href=\"plwiktionary/\">Polish</a></li>");
        KaikkiCatalog unit = new KaikkiCatalog(fetcher, unwritable.resolve("catalog"), Duration.ofDays(7));

        // When
        List<String> editions = unit.editions();

        // Then
        assertThat(editions).containsExactly("pl");
    }

    @Test
    void should_refuse_an_edition_code_that_could_escape_the_cache_directory() throws Exception {
        // Given
        KaikkiCatalog unit = new KaikkiCatalog(fetcher, cacheRoot, Duration.ofDays(7));

        // When
        List<KaikkiLanguage> languages = unit.languagesFor("../../etc/passwd");

        // Then
        assertThat(languages).isEmpty();
        verify(fetcher, never()).fetch(any());
    }
}
