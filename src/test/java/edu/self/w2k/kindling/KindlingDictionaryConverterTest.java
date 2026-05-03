package edu.self.w2k.kindling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.self.w2k.model.LexiconEntry;
import edu.self.w2k.write.opf.OpfDictionaryWriter;

class KindlingDictionaryConverterTest {

    private static final HttpFetcher NO_OP_FETCHER = new HttpFetcher() {
        @Override
        public Path getFile(URI uri, Path dest) throws IOException {
            throw new IOException("Unexpected getFile: " + uri);
        }

        @Override
        public String getString(URI uri) throws IOException {
            throw new IOException("Unexpected getString: " + uri);
        }
    };

    /**
     * Resolver subclass that always returns a specified fake binary path.
     */
    private static KindlingCliResolver fixedBinResolver(Path fakeBin) {
        return new KindlingCliResolver("v0", Optional.empty(), new KindlingDownloader(NO_OP_FETCHER),
                name -> Optional.empty(),
                (v, p) -> null) {
            @Override
            public Path resolve() {
                return fakeBin;
            }
        };
    }

    private static TreeMap<String, List<LexiconEntry>> buildDefs(String... words) {
        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();
        for (String word : words) {
            defs.put(word, List.of(new LexiconEntry(word, "<ol><li>def of " + word + "</li></ol>")));
        }
        return defs;
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void write_capturesCorrectCommand(@TempDir Path tempDir) throws Exception {
        Path fakeBin = tempDir.resolve("kindling-cli");
        Files.createFile(fakeBin);
        fakeBin.toFile().setExecutable(true);

        List<List<String>> captured = new ArrayList<>();
        KindlingDictionaryConverter.ProcessRunner runner = cmd -> {
            captured.add(new ArrayList<>(cmd));
            return 0;
        };

        KindlingDictionaryConverter converter = new KindlingDictionaryConverter(
                new OpfDictionaryWriter(), fixedBinResolver(fakeBin), runner);

        Path result = converter.write(buildDefs("alpha", "beta"), "el", "en", "Title", tempDir);
        assertTrue(result.toString().endsWith("dictionary-el-en.mobi"), "returned path must be the mobi file");

        assertEquals(1, captured.size(), "exactly one command must be run");
        List<String> cmd = captured.getFirst();
        assertEquals(fakeBin.toString(), cmd.get(0), "first arg is the kindling binary");
        assertEquals("build", cmd.get(1), "second arg is 'build'");
        assertTrue(cmd.get(2).endsWith("dictionary-el-en.opf"), "third arg is the OPF path");
        assertEquals("-o", cmd.get(3), "fourth arg is -o");
        assertTrue(cmd.get(4).endsWith("dictionary-el-en.mobi"), "fifth arg is the MOBI output path");
    }

    @Test
    void write_returnsPathEndingInMobi(@TempDir Path tempDir) throws Exception {
        Path fakeBin = tempDir.resolve("kindling-cli");
        Files.createFile(fakeBin);
        fakeBin.toFile().setExecutable(true);

        KindlingDictionaryConverter converter = new KindlingDictionaryConverter(
                new OpfDictionaryWriter(), fixedBinResolver(fakeBin), cmd -> 0);

        Path result = converter.write(buildDefs("alpha"), "el", "en", "Title", tempDir);
        assertTrue(result.toString().endsWith("dictionary-el-en.mobi"), "result must end in .mobi");
    }

    @Test
    void write_opfAndHtmlFilesExist(@TempDir Path tempDir) throws Exception {
        Path fakeBin = tempDir.resolve("kindling-cli");
        Files.createFile(fakeBin);

        KindlingDictionaryConverter converter = new KindlingDictionaryConverter(
                new OpfDictionaryWriter(), fixedBinResolver(fakeBin), cmd -> 0);

        converter.write(buildDefs("alpha", "beta", "gamma"), "el", "en", "Title", tempDir);

        assertTrue(Files.exists(tempDir.resolve("dictionary-el-en.opf")), "OPF file must exist");

        List<Path> htmlFiles = new ArrayList<>();
        try (var stream = Files.newDirectoryStream(tempDir, "*.html")) {
            for (Path p : stream) htmlFiles.add(p);
        }
        assertTrue(htmlFiles.size() >= 1, "at least one HTML file must exist");
    }

    // ── non-zero exit → IOException ───────────────────────────────────────────

    @Test
    void write_nonZeroExit_throwsIOException(@TempDir Path tempDir) throws Exception {
        Path fakeBin = tempDir.resolve("kindling-cli");
        Files.createFile(fakeBin);

        KindlingDictionaryConverter converter = new KindlingDictionaryConverter(
                new OpfDictionaryWriter(), fixedBinResolver(fakeBin), cmd -> 1);

        assertThrows(IOException.class,
                () -> converter.write(buildDefs("alpha"), "el", "en", "Title", tempDir));
    }
}
