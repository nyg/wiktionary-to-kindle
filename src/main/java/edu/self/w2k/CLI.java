package edu.self.w2k;

import edu.self.w2k.command.DownloadCommand;
import edu.self.w2k.command.GenerateCommand;
import edu.self.w2k.download.KaikkiDumpDownloader;
import edu.self.w2k.lexicon.TsvLexiconReader;
import edu.self.w2k.lexicon.TsvLexiconWriter;
import edu.self.w2k.opf.KindleOpfGenerator;
import edu.self.w2k.parse.JsonlDictionaryParser;
import edu.self.w2k.render.HtmlDefinitionRenderer;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class CLI {

    private static final Path DUMP_FILE    = Path.of("dumps/raw-wiktextract-data.jsonl.gz");
    private static final Path LEXICON_FILE = Path.of("dictionaries/lexicon.txt");
    private static final Path DICTIONARIES_DIR = Path.of("dictionaries");

    public static void main(String[] args) {

        if (args == null || args.length == 0) {
            log.error("No arguments provided. Usage: <action> [lang]  (actions: download, generate)");
            return;
        }

        String action = args[0];
        String lang = args.length > 1 ? args[1] : "en";

        log.info("Running: action={}, lang={}", action, lang);

        try {
            if (action.matches("dl|download")) {
                new DownloadCommand(new KaikkiDumpDownloader()).run();
            }
            else if (action.equals("generate")) {
                String title = KindleOpfGenerator.autoTitle(lang, lang);
                new GenerateCommand(
                        new JsonlDictionaryParser(),
                        new HtmlDefinitionRenderer(),
                        new TsvLexiconWriter(),
                        new TsvLexiconReader(),
                        new KindleOpfGenerator(),
                        DUMP_FILE,
                        LEXICON_FILE,
                        DICTIONARIES_DIR,
                        lang,
                        title
                ).run();
            }
            else {
                log.error("Unknown action '{}'. Usage: <action> [lang]  (actions: download, generate)", action);
            }
        }
        catch (Exception e) {
            log.error("Command failed: {}", e.getLocalizedMessage(), e);
        }
    }
}

