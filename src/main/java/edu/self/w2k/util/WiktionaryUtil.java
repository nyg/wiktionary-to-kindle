package edu.self.w2k.util;

import de.tudarmstadt.ukp.jwktl.JWKTL;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEdition;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEntry;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryExample;
import de.tudarmstadt.ukp.jwktl.api.IWiktionarySense;
import de.tudarmstadt.ukp.jwktl.api.filter.WiktionaryEntryFilter;
import de.tudarmstadt.ukp.jwktl.api.util.ILanguage;
import de.tudarmstadt.ukp.jwktl.api.util.Language;
import org.apache.commons.text.StringEscapeUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WiktionaryUtil {

    private static final Logger LOG = Logger.getLogger(WiktionaryUtil.class.getName());

    public static void generateDictionary(String lang) {

        LOG.info("Language selected: " + lang + ".");
        ILanguage languageCode = Language.findByCode(lang);
        if (languageCode == null) {
            LOG.severe(String.format("Error: language code %s does not exists!", lang));
            return;
        }

        LOG.info("Opening dictionary file…");
        File wiktionaryDirectory = new File(DumpUtil.DB_DIRECTORY);
        IWiktionaryEdition wkt = JWKTL.openEdition(wiktionaryDirectory);

        WiktionaryEntryFilter entryFilter = new WiktionaryEntryFilter();
        entryFilter.setAllowedWordLanguages(languageCode);

        int count = 0;
        File file = new File("dictionaries/lexicon.txt");
        try (BufferedWriter lexicon = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {

            LOG.info("Generating entries…");
            for (IWiktionaryEntry entry : wkt.getAllEntries(entryFilter)) {

                count++;
                if (count % 1000 == 0) {
                    LOG.info(count + " entries.");
                }

                lexicon.write(entry.getWord());
                lexicon.write("\t<ol>");
                for (IWiktionarySense sense : entry.getSenses()) {

                    lexicon.write("<li><span>");
                    lexicon.write(StringEscapeUtils.escapeXml10(sense.getGloss().toString().replaceAll("[\n\r]", "; ")));
                    lexicon.write("</span>");

                    if (sense.getExamples() != null) {

                        lexicon.write("<ul>");

                        for (IWiktionaryExample example : sense.getExamples()) {
                            if (example.getExample().toString().trim().length() != 0) {
                                lexicon.write("<li>");
                                lexicon.write(StringEscapeUtils.escapeXml10(example.getExample().toString().replaceAll("[\n\r]", "; ")));
                                lexicon.write("</li>");
                            }
                        }

                        lexicon.write("</ul>");
                    }

                    lexicon.write("</li>");
                }

                lexicon.write("</ol>\n");
            }
        }
        catch (IOException e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
            return;
        }

        LOG.info("Done.");
    }

    private WiktionaryUtil() {
        throw new RuntimeException("Class should not be instantiated.");
    }
}
