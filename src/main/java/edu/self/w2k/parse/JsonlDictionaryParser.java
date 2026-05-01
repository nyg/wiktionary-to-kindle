package edu.self.w2k.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import edu.self.w2k.model.WiktionaryEntry;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

@Slf4j
public class JsonlDictionaryParser implements DictionaryParser {

    @Override
    public Stream<WiktionaryEntry> parse(Path dumpFile, String lang) throws IOException {

        log.info("Parsing dump for lang={}", lang);
        ObjectReader reader = new ObjectMapper().readerFor(WiktionaryEntry.class);
        List<WiktionaryEntry> entries = new ArrayList<>();
        int matched = 0;

        try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(dumpFile));
             BufferedReader lines = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8))) {

            String line;
            while ((line = lines.readLine()) != null) {
                WiktionaryEntry entry = reader.readValue(line);
                if (!lang.equals(entry.getLangCode()) || entry.getWord() == null) {
                    continue;
                }
                entries.add(entry);
                matched++;
                if (matched % 10_000 == 0) {
                    log.info("{} entries matched…", matched);
                }
            }
        }

        log.info("Done. {} entries matched for lang={}", matched, lang);
        return entries.stream();
    }
}
