package edu.self.w2k.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.model.WiktionaryExample;
import edu.self.w2k.model.WiktionarySense;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Slf4j
@UtilityClass
public class WiktionaryUtil {

    public static void generateDictionary(String lang) {

        if (!Files.exists(DumpUtil.getDumpPath())) {
            log.error("Dump file not found: {}. Run 'download' first.", DumpUtil.getDumpPath());
            return;
        }

        try {
            generateDictionaryToFile(DumpUtil.getDumpPath(), lang, Path.of("dictionaries/lexicon.txt"));
        }
        catch (IOException e) {
            log.error("Failed to generate dictionary: {}", e.getLocalizedMessage(), e);
        }
    }

    /**
     * Reads the gzip-compressed JSONL dump at {@code dumpFile}, filters entries by
     * {@code lang} (ISO 639-1 lang_code), and writes the lexicon to {@code outputFile}.
     */
    public static void generateDictionaryToFile(Path dumpFile, String lang, Path outputFile) throws IOException {

        log.info("Generating dictionary for lang={}", lang);
        ObjectReader reader = new ObjectMapper().readerFor(WiktionaryEntry.class);
        int count = 0;

        try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(dumpFile));
             BufferedReader lines = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8));
             BufferedWriter lexicon = new BufferedWriter(new FileWriter(outputFile.toFile(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = lines.readLine()) != null) {

                WiktionaryEntry entry = reader.readValue(line);

                if (!lang.equals(entry.getLangCode()) || entry.getWord() == null) {
                    continue;
                }

                String definition = buildDefinition(entry.getSenses());
                if (definition == null) {
                    continue;
                }

                lexicon.write(entry.getWord());
                lexicon.write("\t");
                lexicon.write(definition);
                lexicon.write("\n");

                count++;
                if (count % 10_000 == 0) {
                    log.info("{} entries written…", count);
                }
            }
        }

        log.info("Done. {} entries written to {}", count, outputFile);
    }

    /**
     * Builds the HTML definition string from a list of senses.
     * Returns null if no renderable glosses are found (e.g. form_of-only entries).
     */
    public static String buildDefinition(List<WiktionarySense> senses) {

        StringBuilder sb = new StringBuilder();
        sb.append("<ol>");
        boolean hasGloss = false;

        for (WiktionarySense sense : senses) {
            if (sense == null) {
                continue;
            }
            List<String> glosses = sense.getGlosses();
            if (glosses.isEmpty()) {
                continue;
            }

            for (String gloss : glosses) {
                if (gloss == null || gloss.isBlank()) {
                    continue;
                }
                hasGloss = true;
                sb.append("<li><span>");
                sb.append(StringEscapeUtils.escapeXml10(gloss.replaceAll("[\n\r]", "; ")));
                sb.append("</span>");

                List<WiktionaryExample> examples = sense.getExamples();
                boolean hasExample = false;
                StringBuilder exSb = new StringBuilder("<ul>");
                for (WiktionaryExample ex : examples) {
                    if (ex == null) {
                        continue;
                    }
                    String text = ex.getText();
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    hasExample = true;
                    exSb.append("<li>");
                    exSb.append(StringEscapeUtils.escapeXml10(text.replaceAll("[\n\r]", "; ")));
                    exSb.append("</li>");
                }
                exSb.append("</ul>");
                if (hasExample) {
                    sb.append(exSb);
                }

                sb.append("</li>");
            }
        }

        sb.append("</ol>");
        return hasGloss ? sb.toString() : null;
    }
}

