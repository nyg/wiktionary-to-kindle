package edu.self.w2k.util;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

import de.tudarmstadt.ukp.jwktl.JWKTL;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEdition;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEntry;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryExample;
import de.tudarmstadt.ukp.jwktl.api.IWiktionarySense;
import de.tudarmstadt.ukp.jwktl.api.filter.WiktionaryEntryFilter;
import de.tudarmstadt.ukp.jwktl.api.util.Language;
import edu.self.w2k.opf.Dictionary;
import edu.self.w2k.opf.Word;

public final class WiktionaryUtil {

    private final static Logger LOG = Logger.getLogger(WiktionaryUtil.class.getName());

    public static void generateDictionary(String sourceLanguage, String targetLanguage, String date) {

        File wiktionaryDirectory = Path.of(DumpUtil.DB_DIRECTORY, targetLanguage, date).toFile();
        LOG.info(wiktionaryDirectory.toString());
        IWiktionaryEdition wkt = JWKTL.openEdition(wiktionaryDirectory);

        WiktionaryEntryFilter entryFilter = new WiktionaryEntryFilter();
        entryFilter.setAllowedWordLanguages(Language.findByCode(sourceLanguage));

        Dictionary dictionary = new Dictionary(String.format("dict-%s-%s", sourceLanguage, targetLanguage), targetLanguage, sourceLanguage);

        int count = 0;

        for (IWiktionaryEntry entry : wkt.getAllEntries(entryFilter)) {

            count++;
            if (count % 1000 == 0) {
                LOG.info(count + " entries.");
            }

            StringBuilder meanings = new StringBuilder();

            meanings.append("<ol>");
            for (IWiktionarySense sense : entry.getSenses()) {

                meanings.append("<li><span>").append(sense.getGloss().toString().replaceAll("[\n\r]", "; ")).append("</span>");

                if (sense.getExamples() != null) {

                    meanings.append("<ul>");

                    for (IWiktionaryExample example : sense.getExamples()) {
                        if (example.getExample().toString().trim().length() != 0) {
                            meanings.append("<li>").append(example.getExample().toString().replaceAll("[\n\r]", "; ")).append("</li>");
                        }
                    }

                    meanings.append("</ul>");
                }

                meanings.append("</li>");
            }

            meanings.append("</ol>\n");
            Word word = new Word(entry.getWord(), meanings.toString());
            dictionary.addWord(word);
        }

        dictionary.build();

        LOG.info("Done.");
        //LOG.info("Unhandled templates:");
        //ENTemplateHandler.UNHANDLED_TEMPLATES.forEach(LOG::info);
    }

    private WiktionaryUtil() {
        throw new RuntimeException("Class should not be instantiated.");
    }
}
