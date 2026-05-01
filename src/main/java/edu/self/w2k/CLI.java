package edu.self.w2k;

import edu.self.w2k.util.DumpUtil;
import edu.self.w2k.util.WiktionaryUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CLI {

    public static void main(String[] args) {

        if (args == null || args.length == 0) {
            log.error("No arguments provided. Usage: <action> [lang]  (actions: download, generate)");
            return;
        }

        String action = args[0];
        String lang = args.length > 1 ? args[1] : "en";

        log.info("Running: action={}, lang={}", action, lang);

        if (action.matches("dl|download")) {
            DumpUtil.download();
        }
        else if (action.equals("generate")) {
            WiktionaryUtil.generateDictionary(lang);
        }
        else {
            log.error("Unknown action '{}'. Usage: <action> [lang]  (actions: download, generate)", action);
        }
    }
}
