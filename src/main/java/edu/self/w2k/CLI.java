package edu.self.w2k;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import edu.self.w2k.command.DownloadCommand;
import edu.self.w2k.command.GenerateCommand;
import edu.self.w2k.config.AppInfo;
import edu.self.w2k.config.AppVersion;
import edu.self.w2k.config.Preferences;
import edu.self.w2k.download.DumpDownloader;
import edu.self.w2k.download.KaikkiDumpDownloader;
import edu.self.w2k.dump.DumpCatalog;
import edu.self.w2k.kindling.KindlingCliResolver;
import edu.self.w2k.kindling.KindlingDictionaryConverter;
import edu.self.w2k.kindling.KindlingDownloader;
import edu.self.w2k.kindling.KindlingRelease;
import edu.self.w2k.parse.JsonlDictionaryParser;
import edu.self.w2k.pipeline.DictionaryPipeline;
import edu.self.w2k.pipeline.DictionaryPipeline.DownloaderFactory;
import edu.self.w2k.progress.ProgressListener;
import edu.self.w2k.render.HtmlDefinitionRenderer;
import edu.self.w2k.write.DictionaryTitles;
import edu.self.w2k.write.DictionaryWriter;
import edu.self.w2k.write.opf.OpfDictionaryWriter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Slf4j
@Command(name = AppInfo.SLUG,
         mixinStandardHelpOptions = true,
         versionProvider = CLI.BuildVersion.class,
         description = "Converts Wiktionary data into Kindle-compatible dictionaries.",
         subcommands = {CLI.Download.class, CLI.Generate.class, CommandLine.HelpCommand.class})
public class CLI implements Callable<Integer> {

    private static final String DUMPS_DIR_DESCRIPTION =
            "Directory holding the kaikki.org dumps (default: the dumps folder from the app's preferences)";

    /**
     * Reports the version Maven filtered into the build, rather than a literal in this annotation
     * that would keep saying 1.0.0 after every release.
     */
    static class BuildVersion implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[] {AppInfo.SLUG + " " + AppVersion.get()};
        }
    }

    @Spec
    CommandSpec spec;

    static void main(String[] args) {
        System.exit(new CommandLine(new CLI()).execute(args));
    }

    static Optional<Path> findLatestDump(String lang, Path dumpsDir) {
        return new DumpCatalog(dumpsDir).latestFor(lang);
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

        @Option(names = "--dumps-dir", paramLabel = "DIR", description = DUMPS_DIR_DESCRIPTION)
        private Path dumpsDir;

        /**
         * Injectable so tests can drive {@link #call()} without the real downloader reaching
         * kaikki.org, the same seam {@link DictionaryPipeline} uses.
         */
        DownloaderFactory downloaderFactory = KaikkiDumpDownloader::new;

        @Override
        public Integer call() {
            Path dumps = dumpsDir(Preferences.load());
            return run(downloaderFactory.create(lang, dumps, ProgressListener.NOOP));
        }

        int run(DumpDownloader downloader) {
            try {
                new DownloadCommand(downloader).run();
                return 0;
            }
            catch (IOException e) {
                log.error("Download failed: {}", e.getLocalizedMessage());
                return 1;
            }
        }

        Path dumpsDir(Preferences preferences) {
            return dumpsDir != null ? dumpsDir : preferences.dumpsDir();
        }
    }

    @Command(name = "generate",
             aliases = {"gen"},
             description = "Generate Kindle dictionary from downloaded dump.",
             mixinStandardHelpOptions = true,
             defaultValueProvider = Generate.KindlingVersionDefault.class)
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

        @Option(names = "--dumps-dir", paramLabel = "DIR", description = DUMPS_DIR_DESCRIPTION)
        private Path dumpsDir;

        @Option(names = "--dictionaries-dir",
                paramLabel = "DIR",
                description = "Directory to write the dictionary into "
                        + "(default: the dictionaries folder from the app's preferences)")
        private Path dictionariesDir;

        @Option(names = "--kindling-cli",
                description = "Path to a pre-installed kindling-cli binary. Skips download.")
        private Path kindlingCliPath;

        @Option(names = "--kindling-version",
                description = "Kindling release tag to download (default: ${DEFAULT-VALUE})")
        private String kindlingVersion;

        static class KindlingVersionDefault implements CommandLine.IDefaultValueProvider {

            @Override
            public String defaultValue(CommandLine.Model.ArgSpec argSpec) {
                if (argSpec.isOption()
                        && "--kindling-version".equals(((CommandLine.Model.OptionSpec) argSpec).longestName())) {
                    return KindlingRelease.load().version();
                }
                return null;
            }
        }

        @Override
        public Integer call() throws Exception {
            Preferences preferences = Preferences.load();
            Path dumps = dumpsDir(preferences);
            Path dictionaries = dictionariesDir(preferences);

            Optional<Path> dumpFile = findLatestDump(dumpLang, dumps);
            if (dumpFile.isEmpty()) {
                log.error("No dump found for language {} in {}", dumpLang, dumps);
                return 1;
            }

            String title = DictionaryTitles.autoTitle(wordLang, dumpLang);
            KindlingDownloader downloader = new KindlingDownloader();
            KindlingCliResolver resolver = new KindlingCliResolver(
                    kindlingVersion, Optional.ofNullable(kindlingCliPath), downloader);
            DictionaryWriter writer = new KindlingDictionaryConverter(
                    new OpfDictionaryWriter(), resolver, KindlingDictionaryConverter.defaultRunner());

            new GenerateCommand(
                    new JsonlDictionaryParser(),
                    new HtmlDefinitionRenderer(),
                    writer,
                    dumpFile.get(),
                    dictionaries,
                    wordLang, dumpLang, title
            ).run();
            return 0;
        }

        Path dumpsDir(Preferences preferences) {
            return dumpsDir != null ? dumpsDir : preferences.dumpsDir();
        }

        Path dictionariesDir(Preferences preferences) {
            return dictionariesDir != null ? dictionariesDir : preferences.dictionariesDir();
        }
    }
}
