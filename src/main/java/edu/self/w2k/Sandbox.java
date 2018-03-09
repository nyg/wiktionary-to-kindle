package edu.self.w2k;

import java.io.File;
import java.util.Arrays;

import de.tudarmstadt.ukp.jwktl.JWKTL;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEdition;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEntry;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryExample;
import de.tudarmstadt.ukp.jwktl.api.IWiktionarySense;
import de.tudarmstadt.ukp.jwktl.api.filter.WiktionaryEntryFilter;
import de.tudarmstadt.ukp.jwktl.api.util.Language;

public class Sandbox {

    private static final String DB_DIRECTORY = "db";

    public static void main(String[] args) throws Exception {

        File wiktionaryDirectory = new File(DB_DIRECTORY);
        IWiktionaryEdition wkt = JWKTL.openEdition(wiktionaryDirectory);

        WiktionaryEntryFilter entryFilter = new WiktionaryEntryFilter();
        entryFilter.setAllowedWordLanguages(Language.findByName("Greek"));

        wkt.getAllEntries(entryFilter).forEach(Sandbox::run);
    }

    private static void run(IWiktionaryEntry entry) {

        String[] words = new String[] { "βασίλειο", "έκπληξη", "πλούτος", "κατάσταση", "μέχρι", "γύρω" };
        if (Arrays.asList(words).contains(entry.getWord())) {

            StringBuilder html = new StringBuilder();

            //System.out.println("* " + entry.getWord());

            html.append("<ol>");
            for (IWiktionarySense sense : entry.getSenses()) {

                //System.out.println("  - " + sense.getGloss());
                html.append("<li><span>").append(sense.getGloss()).append("</span>");

                if (sense.getExamples() != null) {
                    html.append("<ul>");
                    for (IWiktionaryExample example : sense.getExamples()) {
                        //System.out.println("    + " + example.getExample());
                        if (example.getExample().toString().trim().length() != 0) {
                            html.append("<li>").append(example.getExample()).append("</li>");
                        }
                    }
                    html.append("</ul>");
                }

                html.append("</li>");
            }

            html.append("</ol>");

            System.out.println(entry.getWord() + "\t" + html.toString());
        }
    }
}
