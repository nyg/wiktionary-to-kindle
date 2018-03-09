package edu.self.w2k.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.tudarmstadt.ukp.jwktl.JWKTL;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEdition;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEntry;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryExample;
import de.tudarmstadt.ukp.jwktl.api.IWiktionarySense;
import de.tudarmstadt.ukp.jwktl.api.filter.WiktionaryEntryFilter;
import de.tudarmstadt.ukp.jwktl.api.util.Language;

public final class WiktionaryUtil {

    private final static Logger LOG = Logger.getLogger(WiktionaryUtil.class.getName());

    public static void generateDictionary(String lang) {

        File wiktionaryDirectory = new File(DumpUtil.DB_DIRECTORY);
        IWiktionaryEdition wkt = JWKTL.openEdition(wiktionaryDirectory);

        WiktionaryEntryFilter entryFilter = new WiktionaryEntryFilter();
        entryFilter.setAllowedWordLanguages(Language.findByCode(lang));

        int count = 0;
        StringBuilder lexicon = new StringBuilder();

        for (IWiktionaryEntry entry : wkt.getAllEntries(entryFilter)) {

            count++;
            if (count % 1000 == 0) {
                LOG.info(count + " entries.");
            }

            lexicon.append(entry.getWord()).append("\t<ol>");
            for (IWiktionarySense sense : entry.getSenses()) {

                lexicon.append("<li><span>").append(sense.getGloss().toString().replaceAll("[\n\r]", "; ")).append("</span>");

                if (sense.getExamples() != null) {

                    lexicon.append("<ul>");

                    for (IWiktionaryExample example : sense.getExamples()) {
                        if (example.getExample().toString().trim().length() != 0) {
                            lexicon.append("<li>").append(example.getExample().toString().replaceAll("[\n\r]", "; ")).append("</li>");
                        }
                    }

                    lexicon.append("</ul>");
                }

                lexicon.append("</li>");
            }

            lexicon.append("</ol>\n");
        }

        File file = new File("dictionaries/lexicon.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.append(lexicon);
        }
        catch (IOException e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        LOG.info("Done.");
        //LOG.info("Unhandled templates:");
        //ENTemplateHandler.UNHANDLED_TEMPLATES.forEach(LOG::info);
    }

    private WiktionaryUtil() {
        throw new RuntimeException("Class should not be instantiated.");
    }
}
