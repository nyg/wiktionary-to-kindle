package edu.self.w2k.dump;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DumpCatalogTest {

    @TempDir
    Path tmp;

    private DumpCatalog unit;

    @BeforeEach
    void setUp() {
        unit = new DumpCatalog(tmp);
    }

    @Test
    void should_list_dumps_newest_first_when_several_exist() throws Exception {
        // Given
        dump("el", "2026-05-01");
        dump("el", "2026-07-24");
        dump("fr", "2026-06-01");

        // When
        var dumps = unit.list();

        // Then
        assertThat(dumps).extracting(DumpFile::lang, DumpFile::generated)
                .containsExactly(tuple("el", "2026-07-24"),
                                 tuple("fr", "2026-06-01"),
                                 tuple("el", "2026-05-01"));
    }

    @Test
    void should_report_size_on_disk_when_listing() throws Exception {
        // Given
        Files.writeString(tmp.resolve("raw-wiktextract-data-el-2026-07-24.jsonl.gz"),
                          "1234567890", StandardCharsets.UTF_8);

        // When
        var dumps = unit.list();

        // Then
        assertThat(dumps).singleElement()
                .extracting(DumpFile::sizeBytes).isEqualTo(10L);
    }

    @Test
    void should_ignore_unrelated_files_when_listing() throws Exception {
        // Given
        dump("el", "2026-07-24");
        Files.createFile(tmp.resolve("notes.txt"));
        Files.createFile(tmp.resolve("raw-wiktextract-data-el.jsonl.gz.part"));
        Files.createFile(tmp.resolve("raw-wiktextract-data-el-unknown.jsonl.gz"));

        // When
        var dumps = unit.list();

        // Then
        assertThat(dumps).singleElement()
                .extracting(DumpFile::lang).isEqualTo("el");
    }

    @Test
    void should_return_empty_list_when_dumps_dir_does_not_exist() {
        // Given
        DumpCatalog absent = new DumpCatalog(tmp.resolve("nope"));

        // When / Then
        assertThat(absent.list()).isEmpty();
        assertThat(absent.latestFor("el")).isEmpty();
    }

    @Test
    void should_filter_by_language_when_listing_for_one_edition() throws Exception {
        // Given
        dump("el", "2026-07-24");
        dump("fr", "2026-07-24");

        // When / Then
        assertThat(unit.listFor("el")).singleElement()
                .extracting(DumpFile::lang).isEqualTo("el");
        assertThat(unit.listFor("de")).isEmpty();
    }

    @Test
    void should_pick_most_recent_date_when_finding_latest() throws Exception {
        // Given
        dump("el", "2026-05-01");
        Path newest = dump("el", "2026-07-24");
        dump("el", "2026-06-15");

        // When
        Optional<Path> latest = unit.latestFor("el");

        // Then
        assertThat(latest).contains(newest);
    }

    @Test
    void should_not_match_other_languages_when_finding_latest() throws Exception {
        // Given
        dump("fr", "2026-07-24");

        // When / Then
        assertThat(unit.latestFor("el")).isEmpty();
    }

    @Test
    void should_still_find_undated_dump_when_finding_latest() throws Exception {
        // Given a dump named when kaikki omitted last-modified: not listable, but still usable
        Path undated = tmp.resolve("raw-wiktextract-data-el-unknown.jsonl.gz");
        Files.createFile(undated);

        // When / Then
        assertThat(unit.list()).isEmpty();
        assertThat(unit.latestFor("el")).contains(undated);
    }

    @Test
    void should_delete_dump_and_report_it_when_present() throws Exception {
        // Given
        dump("el", "2026-07-24");
        DumpFile target = unit.list().getFirst();

        // When
        boolean deleted = unit.delete(target);

        // Then
        assertThat(deleted).isTrue();
        assertThat(target.path()).doesNotExist();
        assertThat(unit.list()).isEmpty();
    }

    @Test
    void should_report_no_deletion_when_dump_is_already_gone() throws Exception {
        // Given
        DumpFile ghost = new DumpFile(tmp.resolve("raw-wiktextract-data-el-2026-07-24.jsonl.gz"),
                                      "el", LocalDate.of(2026, 7, 24), 0);

        // When / Then
        assertThat(unit.delete(ghost)).isFalse();
    }

    private Path dump(String lang, String date) throws Exception {
        Path path = tmp.resolve("raw-wiktextract-data-%s-%s.jsonl.gz".formatted(lang, date));
        Files.createFile(path);
        return path;
    }

    private static org.assertj.core.groups.Tuple tuple(String lang, String date) {
        return org.assertj.core.api.Assertions.tuple(lang, LocalDate.parse(date));
    }
}
