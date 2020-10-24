package edu.self.w2k;

import edu.self.w2k.util.DumpUtil;
import edu.self.w2k.util.WiktionaryUtil;

import java.util.logging.Logger;

public class CLI {

    private final static Logger LOG = Logger.getLogger(CLI.class.getName());

    public static void main(String[] args) {

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
