package edu.self.w2k.dump;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads and prunes the dumps directory: what has been downloaded, how big it is, and how old.
 * <p>
 * {@link #latestFor(String)} carries the behaviour previously inlined in {@code CLI.findLatestDump},
 * which that method now delegates to.
 */
@Slf4j
@RequiredArgsConstructor
public class DumpCatalog {

    private final Path dumpsDir;

    /**
     * Every recognisable dump, newest generation date first, then by language.
     * <p>
     * Filenames that do not carry a parseable {@code YYYY-MM-DD} are skipped — this can only happen
     * when kaikki.org omitted {@code last-modified} and the downloader fell back to naming the file
     * {@code -unknown}. {@link #latestFor(String)} still finds such a file, so a dump that cannot be
     * listed here remains usable for generation.
     */
    public List<DumpFile> list() {
        List<DumpFile> dumps = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(dumpsDir, DumpFile.PREFIX + "*" + DumpFile.SUFFIX)) {
            for (Path path : stream) {
                DumpFile.parse(path, sizeOf(path))
                        .ifPresentOrElse(dumps::add,
                                         () -> log.debug("Ignoring unrecognised dump filename: {}", path));
            }
        }
        catch (IOException e) {
            log.warn("Could not list dumps in {}: {}", dumpsDir, e.getLocalizedMessage());
            return List.of();
        }

        dumps.sort(Comparator.comparing(DumpFile::generated).reversed()
                           .thenComparing(DumpFile::lang));
        return dumps;
    }

    /** As {@link #list()}, restricted to one Wiktionary edition. */
    public List<DumpFile> listFor(String lang) {
        return list().stream().filter(dump -> dump.lang().equals(lang)).toList();
    }

    /**
     * The most recent dump for {@code lang}, chosen by filename: the ISO date sorts chronologically,
     * so the lexicographically greatest name is the newest.
     */
    public Optional<Path> latestFor(String lang) {
        Path latest = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dumpsDir, DumpFile.globFor(lang))) {
            for (Path path : stream) {
                if (latest == null || path.getFileName().toString()
                        .compareTo(latest.getFileName().toString()) > 0) {
                    latest = path;
                }
            }
        }
        catch (Exception _) {
            return Optional.empty();
        }
        return Optional.ofNullable(latest);
    }

    /**
     * Deletes a dump.
     *
     * @return {@code true} if the file was there and is now gone
     */
    public boolean delete(DumpFile dump) throws IOException {
        boolean deleted = Files.deleteIfExists(dump.path());
        if (deleted) {
            log.info("Deleted dump {}", dump.path());
        }
        return deleted;
    }

    private static long sizeOf(Path path) {
        try {
            return Files.size(path);
        }
        catch (IOException e) {
            log.debug("Could not read size of {}: {}", path, e.getLocalizedMessage());
            return -1;
        }
    }
}
