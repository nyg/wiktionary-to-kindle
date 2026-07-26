package edu.self.w2k.download;

import java.nio.file.Path;

/**
 * Outcome of a successful {@link DumpDownloader#download()}.
 * <p>
 * A skipped download is a success, not a failure: {@code alreadyPresent} distinguishes "the dump was
 * already on disk" from "the dump was transferred now", so callers can report accurately without
 * having to re-glob the dumps directory. Genuine failures are signalled by an exception.
 *
 * @param dumpPath       the dump on disk, ready to be parsed
 * @param alreadyPresent {@code true} when the transfer was skipped because the file already existed
 */
public record DownloadResult(Path dumpPath, boolean alreadyPresent) {}
