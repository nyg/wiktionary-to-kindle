package edu.self.w2k.lexicon;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

@Slf4j
public class TsvLexiconReader implements LexiconReader {

    @Override
    public TreeMap<String, List<LexiconEntry>> read(Path lexiconFile) throws IOException {

        TreeMap<String, List<LexiconEntry>> defs = new TreeMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(lexiconFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                int tab = line.indexOf('\t');
                if (tab < 0) {
                    log.warn("Skipping malformed line (no tab): {}", line);
                    continue;
                }
                String word = line.substring(0, tab).strip();
                String definition = line.substring(tab + 1);
                String key = normaliseKey(word);
                if (key.isEmpty()) {
                    log.warn("Skipping entry with empty key: {}", word);
                    continue;
                }
                defs.computeIfAbsent(key, k -> new ArrayList<>()).add(new LexiconEntry(word, definition));
            }
        }

        log.info("{} unique keys read from lexicon", defs.size());
        return defs;
    }

    /**
     * Normalises a word into a lookup key: lowercase, strip, replace {@code "} with {@code '},
     * and escape {@code <}/{@code >} for the Kindle index.
     */
    static String normaliseKey(String word) {
        return word
                .replace('"', '\'')
                .replace("<", "\\<")
                .replace(">", "\\>")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
