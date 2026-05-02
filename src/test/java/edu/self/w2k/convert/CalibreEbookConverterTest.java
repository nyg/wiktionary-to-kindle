package edu.self.w2k.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class CalibreEbookConverterTest {

    // ── findEbookConvert ──────────────────────────────────────────────────────

    @Test
    void findEbookConvert_overrideWins(@TempDir Path tempDir) throws IOException {
        Path fakeExe = tempDir.resolve("ebook-convert");
        Files.createFile(fakeExe);
        fakeExe.toFile().setExecutable(true);

        Path result = CalibreEbookConverter.findEbookConvert(Optional.of(fakeExe));
        assertEquals(fakeExe, result);
    }

    @Test
    void findEbookConvert_overrideNotExecutable_throwsException(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("nonexistent-ebook-convert");
        assertThrows(EbookConverterNotFoundException.class,
                () -> CalibreEbookConverter.findEbookConvert(Optional.of(nonExistent)));
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    void findEbookConvert_noCalibra_throwsException() {
        // Assumes Calibre is not installed in the test environment.
        // Uses a PATH that contains no ebook-convert.
        assertThrows(EbookConverterNotFoundException.class,
                () -> {
                    String saved = System.getProperty("os.name");
                    try {
                        CalibreEbookConverter.findEbookConvert(Optional.empty());
                    } catch (EbookConverterNotFoundException e) {
                        throw e; // re-throw to satisfy assertThrows
                    } finally {
                        // no-op
                    }
                });
    }

    // ── convert ───────────────────────────────────────────────────────────────

    @Test
    void convert_buildsCorrectArguments(@TempDir Path tempDir) throws IOException {
        Path fakeExe = tempDir.resolve("ebook-convert");
        Files.createFile(fakeExe);
        fakeExe.toFile().setExecutable(true);

        Path inputEpub = tempDir.resolve("dictionary-el-en.epub");
        Files.createFile(inputEpub);

        List<List<String>> captured = new ArrayList<>();
        CalibreEbookConverter converter = new CalibreEbookConverter(
                Optional.of(fakeExe),
                cmd -> { captured.add(new ArrayList<>(cmd)); return 0; });

        converter.convert(inputEpub, tempDir, "mobi");

        assertEquals(1, captured.size());
        List<String> cmd = captured.getFirst();
        assertEquals(fakeExe.toString(), cmd.get(0), "first arg is the executable");
        assertTrue(cmd.get(1).endsWith("dictionary-el-en.epub"), "second arg is input epub");
        assertTrue(cmd.get(2).endsWith("dictionary-el-en.mobi"), "third arg is output mobi");
    }

    @Test
    void convert_nonZeroExitThrowsIOException(@TempDir Path tempDir) throws IOException {
        Path fakeExe = tempDir.resolve("ebook-convert");
        Files.createFile(fakeExe);
        fakeExe.toFile().setExecutable(true);

        Path inputEpub = tempDir.resolve("test.epub");
        Files.createFile(inputEpub);

        CalibreEbookConverter converter = new CalibreEbookConverter(
                Optional.of(fakeExe),
                cmd -> 1);

        assertThrows(IOException.class, () -> converter.convert(inputEpub, tempDir, "mobi"));
    }

    @Test
    void exceptionMessage_mentionsCalibre() {
        String msg = new EbookConverterNotFoundException().getMessage();
        assertTrue(msg.toLowerCase().contains("calibre"),
                "exception message must mention Calibre");
    }
}
