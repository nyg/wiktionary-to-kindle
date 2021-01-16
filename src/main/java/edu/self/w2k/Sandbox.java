package edu.self.w2k;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tudarmstadt.ukp.jwktl.JWKTL;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEdition;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEntry;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryExample;
import de.tudarmstadt.ukp.jwktl.api.IWiktionarySense;
import de.tudarmstadt.ukp.jwktl.api.filter.WiktionaryEntryFilter;
import de.tudarmstadt.ukp.jwktl.api.util.Language;

public class Sandbox {

    private static final String DB_DIRECTORY = "db";
    private static final Logger LOG = Logger.getLogger(Sandbox.class.getName());
    private static int counter = 0;

    public static void main(String[] args) throws Exception {

        File wiktionaryDirectory = new File(DB_DIRECTORY);
        IWiktionaryEdition wkt = JWKTL.openEdition(wiktionaryDirectory);

        WiktionaryEntryFilter entryFilter = new WiktionaryEntryFilter();
        entryFilter.setAllowedWordLanguages(Language.findByName("Greek"));

        counter = 0;
        wkt.getAllEntries(entryFilter).forEach(Sandbox::runEtyl);
        LOG.info(counter + " entries.");
    }

    private static final Pattern PATTERN = Pattern.compile("\\{\\{etyl\\|(.*?)\\|(.*?)\\}\\} \\{\\{m\\|.*?\\|(.*?)\\}\\}");

    private static void runEtyl(IWiktionaryEntry entry) {

        if (entry.getWordEtymology() == null) {
            return;
        }

        String etymology = entry.getWordEtymology().getText();

        if (etymology.contains("{{etyl")) {
            counter++;

            LOG.info(entry.getWord());
            LOG.info(etymology);

            Matcher matcher = PATTERN.matcher(etymology);
            StringBuffer sb = new StringBuffer();

            if (matcher.find()) {
                matcher.appendReplacement(sb, "{{inh|$2|$1|$3}}");
            }
            else {
                LOG.info("Not found.");
            }

            LOG.info(matcher.appendTail(sb).toString());
            LOG.info("-------------------------------");
        }
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
