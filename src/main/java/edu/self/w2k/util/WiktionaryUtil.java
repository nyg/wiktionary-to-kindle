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
import de.tudarmstadt.ukp.jwktl.parser.en.components.template.ENTemplateHandler;

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

            //            String[] words = new String[] { "βασίλειο", "έκπληξη", "πλούτος", "κατάσταση", "μέχρι", "γύρω" };
            //            if (Arrays.asList(words).contains(entry.getWord())) {

            //System.out.println("* " + entry.getWord());

            lexicon.append(entry.getWord()).append("\t<ol>");
            for (IWiktionarySense sense : entry.getSenses()) {

                //System.out.println("  - " + sense.getGloss());
                lexicon.append("<li><span>").append(sense.getGloss()).append("</span>");

                if (sense.getExamples() != null) {
                    lexicon.append("<ul>");
                    for (IWiktionaryExample example : sense.getExamples()) {
                        //System.out.println("    + " + example.getExample());
                        if (example.getExample().toString().trim().length() != 0) {
                            lexicon.append("<li>").append(example.getExample()).append("</li>");
                        }
                    }
                    lexicon.append("</ul>");
                }

                lexicon.append("</li>");
            }

            lexicon.append("</ol>\n");

            //System.out.println(entry.getWord() + "\t" + lexicon.toString());
            //            }
        }

        File file = new File("dictionaries/lexicon.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.append(lexicon);
        }
        catch (IOException e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        LOG.info("Done.");
        LOG.info("Unhandled templates:");
        ENTemplateHandler.UNHANDLED_TEMPLATES.forEach(LOG::info);
    }

    private WiktionaryUtil() {
        throw new RuntimeException("Class should not be instantiated.");
    }
}
