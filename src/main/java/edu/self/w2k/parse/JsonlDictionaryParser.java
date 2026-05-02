package edu.self.w2k.parse;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import edu.self.w2k.model.WiktionaryEntry;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

@Slf4j
public class JsonlDictionaryParser implements DictionaryParser {

    @Override
    public Stream<WiktionaryEntry> parse(Path dumpFile, String lang) throws IOException {

        log.info("Parsing dump for lang={}", lang);

        ObjectReader reader = new ObjectMapper()
                .setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
                .readerFor(WiktionaryEntry.class);

        GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(dumpFile));
        BufferedReader lines = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8));

        AtomicLong matched = new AtomicLong();

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
                .peek(e -> {
                    long count = matched.incrementAndGet();
                    if (count % 10_000 == 0) {
                        log.info("{} entries matched…", count);
                    }
                })
                .onClose(() -> {
                    try {
                        lines.close();
                    }
                    catch (IOException e) {
                        log.warn("Failed to close GZIP reader: {}", e.getLocalizedMessage());
                    }
                });
    }
}
