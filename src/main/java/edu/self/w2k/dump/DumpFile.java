package edu.self.w2k.dump;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A downloaded kaikki.org dump, described by its filename.
 *
 * @param path        the file on disk
 * @param lang        Wiktionary edition language code
 * @param generated   the dump's generation date, from the filename
 * @param sizeBytes   size on disk, or {@code -1} if it could not be read
 */
public record DumpFile(Path path, String lang, LocalDate generated, long sizeBytes) {

    static final String PREFIX = "raw-wiktextract-data-";
    static final String SUFFIX = ".jsonl.gz";

    /**
     * Matches {@code raw-wiktextract-data-{lang}-{YYYY-MM-DD}.jsonl.gz}. The language code is
     * non-greedy up to the final date group so codes containing a hyphen (e.g. {@code zh-min-nan})
     * are captured whole.
     */
    private static final Pattern NAME_PATTERN =
            Pattern.compile(Pattern.quote(PREFIX) + "(?<lang>.+)-(?<date>\\d{4}-\\d{2}-\\d{2})" + Pattern.quote(SUFFIX));

    /** Glob selecting every dump for {@code lang}; the ISO date makes filenames sort chronologically. */
    public static String globFor(String lang) {
        return PREFIX + lang + "-*" + SUFFIX;
    }

    /**
     * Parses {@code path}'s filename, or returns empty when it is not a dump name. {@code sizeBytes}
     * is supplied by the caller, which already has the directory listing open.
     */
    public static Optional<DumpFile> parse(Path path, long sizeBytes) {
        Matcher matcher = NAME_PATTERN.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new DumpFile(path,
                                            matcher.group("lang"),
                                            LocalDate.parse(matcher.group("date")),
                                            sizeBytes));
        }
        catch (RuntimeException _) {
            // A syntactically well-formed but impossible date, e.g. 2026-02-31.
            return Optional.empty();
        }
    }
}
