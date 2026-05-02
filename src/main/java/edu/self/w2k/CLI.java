package edu.self.w2k;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import edu.self.w2k.command.DownloadCommand;
import edu.self.w2k.command.GenerateCommand;
import edu.self.w2k.convert.CalibreEbookConverter;
import edu.self.w2k.download.KaikkiDumpDownloader;
import edu.self.w2k.parse.JsonlDictionaryParser;
import edu.self.w2k.render.HtmlDefinitionRenderer;
import edu.self.w2k.write.DictionaryTitles;
import edu.self.w2k.write.DictionaryWriter;
import edu.self.w2k.write.OutputFormat;
import edu.self.w2k.write.epub.EpubDictionaryWriter;
import edu.self.w2k.write.mobi.MobiDictionaryWriter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
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
    static final Path DUMPS_DIR = Path.of("dumps");

    @Spec
    CommandSpec spec;

    static void main(String[] args) {
        System.exit(new CommandLine(new CLI()).execute(args));
    }

    /**
     * Finds the latest dump file for a given language in the dumps directory.
     * Searches for files matching {@code raw-wiktextract-data-{lang}-*.jsonl.gz} and returns
     * the lexicographically latest (YYYY-MM-DD format sorts correctly).
     */
    static Optional<Path> findLatestDump(String lang) {
        String prefix = "raw-wiktextract-data-" + lang + "-";
        String suffix = ".jsonl.gz";
        Path latest = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DUMPS_DIR,
                prefix + "*" + suffix)) {
            for (Path path : stream) {
                if (latest == null || path.getFileName().toString().compareTo(latest.getFileName().toString()) > 0) {
                    latest = path;
                }
            }
        }
        catch (Exception e) {
            return Optional.empty();
        }
        return Optional.ofNullable(latest);
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(System.out);
        return 0;
    }

    @Command(
            name = "download",
            aliases = {"dl"},
            description = "Download Wiktionary dump from kaikki.org.",
            mixinStandardHelpOptions = true)
    static class Download implements Callable<Integer> {

        @Parameters(
                index = "0",
                arity = "0..1",
                defaultValue = "en",
                description = "Wiktionary edition language code (ISO 639-1, default: ${DEFAULT-VALUE})")
        private String lang;

        @Override
        public Integer call() {
            new DownloadCommand(new KaikkiDumpDownloader(lang)).run();
            return 0;
        }
    }

    @Command(
            name = "generate",
            aliases = {"gen"},
            description = "Generate Kindle dictionary from downloaded dump.",
            mixinStandardHelpOptions = true)
    static class Generate implements Callable<Integer> {

        @Parameters(
                index = "0",
                arity = "1",
                paramLabel = "DUMP_LANG",
                description = "Wiktionary edition language code (ISO 639-1)")
        private String dumpLang;

        @Parameters(
                index = "1",
                arity = "1",
                description = "Language to filter entries by (ISO 639-1)")
        private String wordLang;

        @Option(names = {"-f", "--format"}, defaultValue = "epub",
                description = "Output format: epub (default) or mobi (requires Calibre)")
        private OutputFormat format;

        @Option(names = {"--ebook-convert"},
                description = "Path to ebook-convert binary (Calibre). Only used with --format mobi.")
        private Path ebookConvertPath;

        @Override
        public Integer call() throws Exception {
            Optional<Path> dumpFile = findLatestDump(dumpLang);
            if (dumpFile.isEmpty()) {
                log.error("No dump found for language {} in {}", dumpLang, DUMPS_DIR);
                return 1;
            }
            String title = DictionaryTitles.autoTitle(wordLang, dumpLang);
            DictionaryWriter writer = buildWriter();
            new GenerateCommand(
                    new JsonlDictionaryParser(),
                    new HtmlDefinitionRenderer(),
                    writer,
                    dumpFile.get(),
                    DICTIONARIES_DIR,
                    wordLang,
                    dumpLang,
                    title
            ).run();
            return 0;
        }

        private DictionaryWriter buildWriter() {
            EpubDictionaryWriter epubWriter = new EpubDictionaryWriter();
            return switch (format) {
                case EPUB -> epubWriter;
                case MOBI -> new MobiDictionaryWriter(epubWriter,
                        new CalibreEbookConverter(Optional.ofNullable(ebookConvertPath)));
            };
        }
    }
}
