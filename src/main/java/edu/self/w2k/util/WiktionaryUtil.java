package edu.self.w2k.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import edu.self.w2k.model.WiktionaryEntry;
import edu.self.w2k.model.WiktionaryExample;
import edu.self.w2k.model.WiktionarySense;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public final class WiktionaryUtil {

    private static final Logger LOG = Logger.getLogger(WiktionaryUtil.class.getName());

    public static void generateDictionary(String lang) {

        if (!Files.exists(DumpUtil.getDumpPath())) {
            LOG.severe("Dump file not found: " + DumpUtil.getDumpPath() + ". Run 'download' first.");
            return;
        }

        try {
            generateDictionaryToFile(DumpUtil.getDumpPath(), lang, Path.of("dictionaries/lexicon.txt"));
        }
        catch (IOException e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Reads the gzip-compressed JSONL dump at {@code dumpFile}, filters entries by
     * {@code lang} (ISO 639-1 lang_code), and writes the lexicon to {@code outputFile}.
     */
    public static void generateDictionaryToFile(Path dumpFile, String lang, Path outputFile) throws IOException {

        LOG.info("Language selected: " + lang + ".");
        ObjectReader reader = new ObjectMapper().readerFor(WiktionaryEntry.class);
        int count = 0;

        try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(dumpFile));
             BufferedReader lines = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8));
             BufferedWriter lexicon = new BufferedWriter(new FileWriter(outputFile.toFile(), StandardCharsets.UTF_8))) {

            LOG.info("Generating entries…");
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
                if (count % 1000 == 0) {
                    LOG.info(count + " entries written.");
                }
            }
        }

        LOG.info("Done. " + count + " entries written.");
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

    private WiktionaryUtil() {
        throw new RuntimeException("Class should not be instantiated.");
    }
}

