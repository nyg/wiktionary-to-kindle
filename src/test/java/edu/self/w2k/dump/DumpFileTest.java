package edu.self.w2k.dump;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DumpFileTest {

    @Test
    void should_extract_lang_and_date_when_name_is_well_formed() {
        // When
        var parsed = DumpFile.parse(Path.of("/d/raw-wiktextract-data-el-2026-07-24.jsonl.gz"), 42);

        // Then
        assertThat(parsed).isPresent();
        assertThat(parsed.get().lang()).isEqualTo("el");
        assertThat(parsed.get().generated()).isEqualTo(LocalDate.of(2026, 7, 24));
        assertThat(parsed.get().sizeBytes()).isEqualTo(42);
    }

    @Test
    void should_capture_whole_code_when_lang_contains_hyphens() {
        // When
        var parsed = DumpFile.parse(Path.of("raw-wiktextract-data-zh-min-nan-2026-07-24.jsonl.gz"), 0);

        // Then
        assertThat(parsed).isPresent();
        assertThat(parsed.get().lang()).isEqualTo("zh-min-nan");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "raw-wiktextract-data-el-unknown.jsonl.gz",   // kaikki omitted last-modified
            "raw-wiktextract-data-el.jsonl.gz.part",      // in-flight download
            "raw-wiktextract-data-el-2026-07.jsonl.gz",   // incomplete date
            "raw-wiktextract-data--2026-07-24.jsonl.gz",  // empty lang
            "raw-wiktextract-data-el-2026-02-31.jsonl.gz",// impossible date
            "notes.txt",
    })
    void should_return_empty_when_name_is_not_a_dump(String fileName) {
        // When / Then
        assertThat(DumpFile.parse(Path.of(fileName), 0)).isEmpty();
    }

    @Test
    void should_build_glob_scoped_to_one_language() {
        // When
        String glob = DumpFile.globFor("el");

        // Then
        assertThat(glob).isEqualTo("raw-wiktextract-data-el-*.jsonl.gz");
    }
}
