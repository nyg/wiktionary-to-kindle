package edu.self.w2k.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.progress.CountingInputStream;
import edu.self.w2k.progress.ProgressListener;
import edu.self.w2k.progress.ProgressListener.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonlDictionaryParser implements DictionaryParser {

    /** Progress is emitted at most once per this many bytes of compressed input. */
    private static final long PROGRESS_INTERVAL_BYTES = 4 * 1024 * 1024L;

    private final ProgressListener progress;

    public JsonlDictionaryParser() {
        this(ProgressListener.NOOP);
    }

    public JsonlDictionaryParser(ProgressListener progress) {
        this.progress = progress;
    }

    @Override
    public Stream<WiktionaryEntry> parse(Path dumpFile, String lang) throws IOException {
        log.info("Parsing dump for lang={}", lang);

        ObjectReader reader = new ObjectMapper()
                .setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
                .readerFor(WiktionaryEntry.class);

        // Count on the compressed side: the uncompressed size of a gzip member is not knowable up
        // front, but the file size is, so bytes-of-input gives a genuine 0-100% figure.
        long compressedSize = Files.size(dumpFile);
        InputStream counting = new CountingInputStream(
                Files.newInputStream(dumpFile),
                PROGRESS_INTERVAL_BYTES,
                read -> progress.onProgress(Stage.PARSE, read, compressedSize));

        GZIPInputStream gzip = new GZIPInputStream(counting);
        BufferedReader lines = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8));

        return lines.lines()
                .map(line -> {
                    try {
                        return (WiktionaryEntry) reader.readValue(line);
                    }
                    catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .filter(entry -> lang.equals(entry.langCode()) && entry.word() != null && !entry.word().isBlank())
                .onClose(() -> {
                    progress.onProgress(Stage.PARSE, compressedSize, compressedSize);
                    try {
                        lines.close();
                    }
                    catch (IOException e) {
                        log.warn("Failed to close GZIP reader: {}", e.getLocalizedMessage());
                    }
                });
    }
}
