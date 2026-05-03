package edu.self.w2k;

import java.net.http.HttpClient;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;

import edu.self.w2k.command.DownloadCommand;
import edu.self.w2k.command.GenerateCommand;
import edu.self.w2k.download.KaikkiDumpDownloader;
import edu.self.w2k.kindling.KindlingCliResolver;
import edu.self.w2k.kindling.KindlingDictionaryConverter;
import edu.self.w2k.kindling.KindlingDownloader;
import edu.self.w2k.kindling.KindlingRelease;
import edu.self.w2k.parse.JsonlDictionaryParser;
import edu.self.w2k.render.HtmlDefinitionRenderer;
import edu.self.w2k.write.DictionaryTitles;
import edu.self.w2k.write.opf.OpfDictionaryWriter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Slf4j
@Command(name = "wiktionary-to-kindle",
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

    static Optional<Path> findLatestDump(String lang) {
        String prefix = "raw-wiktextract-data-" + lang + "-";
        String suffix = ".jsonl.gz";
        Path latest = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DUMPS_DIR, prefix + "*" + suffix)) {
            for (Path path : stream) {
                if (latest == null || path.getFileName().toString()
                        .compareTo(latest.getFileName().toString()) > 0) {
                    latest = path;
                }
            }
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.ofNullable(latest);
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(System.out);
        return 0;
    }

    @Command(name = "download",
             aliases = {"dl"},
             description = "Download Wiktionary dump from kaikki.org.",
             mixinStandardHelpOptions = true)
    static class Download implements Callable<Integer> {

        @Parameters(index = "0",
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

    @Command(name = "generate",
             aliases = {"gen"},
             description = "Generate Kindle dictionary from downloaded dump.",
             mixinStandardHelpOptions = true)
    static class Generate implements Callable<Integer> {

        @Parameters(index = "0",
                    arity = "1",
                    paramLabel = "DUMP_LANG",
                    description = "Wiktionary edition language code (ISO 639-1)")
        private String dumpLang;

        @Parameters(index = "1",
                    arity = "1",
                    description = "Language to filter entries by (ISO 639-1)")
        private String wordLang;

        @Option(names = "--kindling-cli",
                description = "Path to a pre-installed kindling-cli binary. Skips download.")
        private Path kindlingCliPath;

        @Option(names = "--kindling-version",
                defaultValue = KindlingRelease.DEFAULT_VERSION,
                description = "Kindling release tag to download (default: ${DEFAULT-VALUE})")
        private String kindlingVersion;

        @Override
        public Integer call() throws Exception {
            Optional<Path> dumpFile = findLatestDump(dumpLang);
            if (dumpFile.isEmpty()) {
                log.error("No dump found for language {} in {}", dumpLang, DUMPS_DIR);
                return 1;
            }

            String title = DictionaryTitles.autoTitle(wordLang, dumpLang);
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(60))
                    .build();
            KindlingDownloader downloader = new KindlingDownloader(httpClient);
            KindlingCliResolver resolver = new KindlingCliResolver(
                    kindlingVersion, Optional.ofNullable(kindlingCliPath), downloader);
            var writer = new KindlingDictionaryConverter(
                    new OpfDictionaryWriter(), resolver, KindlingDictionaryConverter.defaultRunner());

            new GenerateCommand(
                    new JsonlDictionaryParser(),
                    new HtmlDefinitionRenderer(),
                    writer,
                    dumpFile.get(),
                    DICTIONARIES_DIR,
                    wordLang, dumpLang, title
            ).run();
            return 0;
        }
    }
}
