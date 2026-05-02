package edu.self.w2k;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import edu.self.w2k.command.DownloadCommand;
import edu.self.w2k.command.GenerateCommand;
import edu.self.w2k.download.KaikkiDumpDownloader;
import edu.self.w2k.opf.KindleOpfGenerator;
import edu.self.w2k.parse.JsonlDictionaryParser;
import edu.self.w2k.render.HtmlDefinitionRenderer;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Slf4j
@Command(
        name = "wiktionary-to-kindle",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Converts Wiktionary data into Kindle-compatible dictionaries.",
        subcommands = {CLI.Download.class, CLI.Generate.class, CommandLine.HelpCommand.class})
public class CLI implements Callable<Integer> {

    static final Path DICTIONARIES_DIR = Path.of("dictionaries");

    @Spec
    CommandSpec spec;

    static void main(String[] args) {
        System.exit(new CommandLine(new CLI()).execute(args));
    }

    static Path dumpFile(String lang) {
        return Path.of("dumps/raw-wiktextract-data-" + lang + ".jsonl.gz");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(System.out);
        return 0;
    }

    @Command(name = "download", aliases = {"d"},
            description = "Download Wiktionary dump from kaikki.org.",
            mixinStandardHelpOptions = true)
    static class Download implements Callable<Integer> {

        @Parameters(index = "0", arity = "0..1", defaultValue = "en",
                description = "Wiktionary edition language code (ISO 639-1, default: ${DEFAULT-VALUE})")
        private String lang;

        @Override
        public Integer call() {
            new DownloadCommand(new KaikkiDumpDownloader(lang, dumpFile(lang))).run();
            return 0;
        }
    }

    @Command(name = "generate", aliases = {"g"},
            description = "Generate Kindle dictionary from downloaded dump.",
            mixinStandardHelpOptions = true)
    static class Generate implements Callable<Integer> {

        @Parameters(index = "0", arity = "0..1", defaultValue = "en",
                description = "Language code (ISO 639-1, default: ${DEFAULT-VALUE})")
        private String lang;

        @Override
        public Integer call() throws Exception {
            String title = KindleOpfGenerator.autoTitle(lang, lang);
            new GenerateCommand(
                    new JsonlDictionaryParser(),
                    new HtmlDefinitionRenderer(),
                    new KindleOpfGenerator(),
                    dumpFile(lang),
                    DICTIONARIES_DIR,
                    lang,
                    title
            ).run();
            return 0;
        }
    }
}
