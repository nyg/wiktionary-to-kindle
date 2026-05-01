package edu.self.w2k;

import edu.self.w2k.util.DumpUtil;
import edu.self.w2k.util.OpfUtil;
import edu.self.w2k.util.WiktionaryUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class CLI {

    private static final Path LEXICON_FILE = Path.of("dictionaries/lexicon.txt");
    private static final Path DICTIONARIES_DIR = Path.of("dictionaries");

    public static void main(String[] args) {

        if (args == null || args.length == 0) {
            log.error("No arguments provided. Usage: <action> [srcLang [trgLang [title]]]  (actions: download, generate)");
            return;
        }

        String action = args[0];
        String srcLang = args.length > 1 ? args[1] : "en";
        String trgLang = args.length > 2 ? args[2] : "en";
        String title   = args.length > 3 ? args[3] : OpfUtil.autoTitle(srcLang, trgLang);

        log.info("Running: action={}, srcLang={}, trgLang={}", action, srcLang, trgLang);

        if (action.matches("dl|download")) {
            DumpUtil.download();
        }
        else if (action.equals("generate")) {
            WiktionaryUtil.generateDictionary(srcLang);
            generateOpf(srcLang, trgLang, title);
        }
        else {
            log.error("Unknown action '{}'. Usage: <action> [srcLang [trgLang [title]]]  (actions: download, generate)", action);
        }
    }

    private static void generateOpf(String srcLang, String trgLang, String title) {
        try {
            OpfUtil.generateOpf(LEXICON_FILE, srcLang, trgLang, title, DICTIONARIES_DIR);
        }
        catch (IOException e) {
            log.error("Failed to generate OPF: {}", e.getLocalizedMessage(), e);
        }
    }
}
