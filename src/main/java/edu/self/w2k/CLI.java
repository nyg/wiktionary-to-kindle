package edu.self.w2k;

import java.util.logging.Logger;

import edu.self.w2k.util.DumpUtil;
import edu.self.w2k.util.WiktionaryUtil;

public class CLI {

    private static final Logger LOG = Logger.getLogger(CLI.class.getName());

    public static void main(String[] args) {

        if (args == null || args.length == 0) {
            LOG.severe("Arguments required!");
            return;
        }

        String action = args[0];
        String lang = "en";

        try {
            lang = args[1];
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // nothing
        }

        LOG.info(String.format("Executing: %s %s", action, lang));

        // download
        if (action.matches("dl|download")) {
            DumpUtil.download();
        }

        // generate [lang]
        if (action.equals("generate")) {
            WiktionaryUtil.generateDictionary(lang);
        }
    }
}
