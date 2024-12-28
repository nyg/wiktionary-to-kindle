package edu.self.w2k;

import java.util.logging.Logger;

import edu.self.w2k.util.DumpUtil;
import edu.self.w2k.util.WiktionaryUtil;

public class CLI {

    private static final Logger LOG = Logger.getLogger(CLI.class.getName());

    public static void main(String[] args) {

        // Remove limit of 50'000'000 when parsing XML
        System.setProperty("entityExpansionLimit", "0");
        System.setProperty("totalEntitySizeLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");

        if (args == null || args.length == 0) {
            LOG.severe("Arguments required!");
            return;
        }

        String action = args[0];
        String lang = "en";
        String date = "latest";

        try {
            lang = args[1];
            date = args[2];
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // nothing
        }

        LOG.info(String.format("Executing: %s %s %s", action, lang, date));

        // download [lang] [date]
        if (action.matches("dl|download")) {
            DumpUtil.download(lang, date);
        }

        // parse [lang] [date]
        if (action.equals("parse")) {
            DumpUtil.parse(lang, date);
        }

        // generate [lang]
        if (action.equals("generate")) {
            WiktionaryUtil.generateDictionary(lang);
        }

        // clean [lang] [date]
        if (action.matches("cl|clean")) {
            DumpUtil.clean(lang, date);
        }
    }
}
