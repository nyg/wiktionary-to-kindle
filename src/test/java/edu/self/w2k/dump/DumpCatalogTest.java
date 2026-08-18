package edu.self.w2k.dump;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class DumpCatalogTest {

    @TempDir
    Path tmp;

    private DumpCatalog unit;
    private ListAppender<ILoggingEvent> logged;

    @BeforeEach
    void setUp() {
        unit = new DumpCatalog(tmp);
        logged = new ListAppender<>();
        logged.start();
        logger().addAppender(logged);
    }

    @AfterEach
    void tearDown() {
        logger().detachAppender(logged);
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
    void should_stay_quiet_when_dumps_dir_does_not_exist() {
        // Given a first run: the dumps folder is created by the first download, not before it
        DumpCatalog absent = new DumpCatalog(tmp.resolve("nope"));

        // When
        absent.list();

        // Then
        assertThat(logged.list).noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.WARN));
    }

    @Test
    void should_name_the_cause_rather_than_the_path_when_the_dumps_dir_is_not_a_directory()
            throws Exception {
        // Given
        Path file = tmp.resolve("not-a-directory");
        Files.createFile(file);
        DumpCatalog unusable = new DumpCatalog(file);

        // When
        assertThat(unusable.list()).isEmpty();

        // Then the path is named once, by the template, and the cause explains the failure
        assertThat(logged.list).singleElement()
                .satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage())
                            .contains(NotDirectoryException.class.getSimpleName())
                            .satisfies(message -> assertThat(occurrencesOf(file.toString(), message))
                                    .isEqualTo(1));
                });
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

    private static int occurrencesOf(String needle, String haystack) {
        int count = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) {
            count++;
        }
        return count;
    }

    private static ch.qos.logback.classic.Logger logger() {
        return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DumpCatalog.class);
    }

    private static org.assertj.core.groups.Tuple tuple(String lang, String date) {
        return org.assertj.core.api.Assertions.tuple(lang, LocalDate.parse(date));
    }
}
