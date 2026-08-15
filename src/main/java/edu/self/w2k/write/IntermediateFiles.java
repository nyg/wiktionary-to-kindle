package edu.self.w2k.write;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * The working directory a single generation writes its HTML, OPF, NCX and cover into.
 * <p>
 * These files exist only to feed kindling-cli, and one dictionary produces dozens of them. Left
 * beside the {@code .mobi} they buried the one file a user actually wants, so they go into
 * {@code <dictionaries>/intermediate/<base name>/} instead — one directory per dictionary, which
 * keeps two editions' runs apart and makes the cleanup a single recursive delete rather than a
 * name-prefix sweep over a shared folder.
 */
public final class IntermediateFiles {

    public static final String DIR_NAME = "intermediate";

    private IntermediateFiles() {
    }

    public static Path dirFor(Path dictionariesDir, String srcLang, String trgLang) {
        return dictionariesDir.resolve(DIR_NAME).resolve(DictionaryTitles.baseName(srcLang, trgLang));
    }

    /**
     * Deletes {@code dir} and its contents, then the shared {@code intermediate} parent if that
     * leaves it empty — another dictionary's working directory keeps it.
     */
    public static void delete(Path dir) throws IOException {
        if (Files.notExists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
        deleteIfEmpty(dir.getParent());
    }

    private static void deleteIfEmpty(Path dir) throws IOException {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> entries = Files.list(dir)) {
            if (entries.findAny().isEmpty()) {
                Files.deleteIfExists(dir);
            }
        }
    }
}
