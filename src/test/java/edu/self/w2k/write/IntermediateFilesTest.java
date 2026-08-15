package edu.self.w2k.write;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class IntermediateFilesTest {

    @TempDir
    Path dictionariesDir;

    @Test
    void should_give_each_dictionary_its_own_directory_under_intermediate() {
        // When
        Path first = IntermediateFiles.dirFor(dictionariesDir, "en", "el");
        Path second = IntermediateFiles.dirFor(dictionariesDir, "fr", "el");

        // Then
        assertThat(first)
                .isEqualTo(dictionariesDir.resolve("intermediate").resolve("w2k-dictionary-en-el"))
                .isNotEqualTo(second);
    }

    @Test
    void should_delete_the_directory_and_its_contents_when_deleting() throws Exception {
        // Given
        Path dir = IntermediateFiles.dirFor(dictionariesDir, "en", "el");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("w2k-dictionary-en-el-0.html"), "<html/>");
        Files.writeString(dir.resolve("w2k-dictionary-en-el.opf"), "<package/>");

        // When
        IntermediateFiles.delete(dir);

        // Then
        assertThat(dir).doesNotExist();
    }

    @Test
    void should_delete_the_shared_parent_when_it_is_left_empty() throws Exception {
        // Given
        Path dir = IntermediateFiles.dirFor(dictionariesDir, "en", "el");
        Files.createDirectories(dir);

        // When
        IntermediateFiles.delete(dir);

        // Then
        assertThat(dictionariesDir.resolve(IntermediateFiles.DIR_NAME)).doesNotExist();
        assertThat(dictionariesDir).exists();
    }

    @Test
    void should_keep_the_shared_parent_when_another_dictionary_still_uses_it() throws Exception {
        // Given
        Path dir = IntermediateFiles.dirFor(dictionariesDir, "en", "el");
        Path other = IntermediateFiles.dirFor(dictionariesDir, "fr", "el");
        Files.createDirectories(dir);
        Files.createDirectories(other);

        // When
        IntermediateFiles.delete(dir);

        // Then
        assertThat(other).exists();
        assertThat(dictionariesDir.resolve(IntermediateFiles.DIR_NAME)).exists();
    }

    @Test
    void should_do_nothing_when_the_directory_is_absent() throws Exception {
        // Given a run that never got as far as writing anything
        Path dir = IntermediateFiles.dirFor(dictionariesDir, "en", "el");

        // When
        IntermediateFiles.delete(dir);

        // Then
        assertThat(dir).doesNotExist();
    }
}
