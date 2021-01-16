package edu.self.w2k;

import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.self.w2k.util.DumpUtil;
import edu.self.w2k.util.WiktionaryUtil;

/**
 * Entry point for the command-line interface.
 */
public class CLI {

    private static final Logger LOG = Logger.getLogger(CLI.class.getName());

    private static final String[] DATE = { "d", "date", "DATE" };
    private static final String[] SOURCE_LANG = { "s", "source-language", "SOURCE LANGUAGE" };
    private static final String[] TARGET_LANG = { "t", "target-language", "TARGET LANGUAGE" };
    private static final String[] COMMAND = { "c", "command", "COMMAND NAME" };

    public static void main(String[] args) {

        /* Define options. */

        Options options = new Options();
        options
            .addOption(
                Option
                    .builder(COMMAND[0])
                    .longOpt(COMMAND[1])
                    .required()
                    .hasArg()
                    .argName(COMMAND[2])
                    .desc("One of the following: download (or dl), parse, dict or clean.")
                    .build());

        options
            .addOption(
                Option
                    .builder(SOURCE_LANG[0])
                    .longOpt(SOURCE_LANG[1])
                    .hasArg()
                    .argName(SOURCE_LANG[2])
                    .desc("Either the language of the dump or the source source language of the dictionary to build. Defaults to \"en\".")
                    .build());

        options
            .addOption(
                Option.builder(TARGET_LANG[0]).longOpt(TARGET_LANG[1]).hasArg().argName(TARGET_LANG[2]).desc("Target language.").build());

        options
            .addOption(
                Option
                    .builder(DATE[0])
                    .longOpt(DATE[1])
                    .hasArg()
                    .argName(DATE[2])
                    .desc("The date identifying the dump (format: YYYYMMDD or simply \"latest\"). Defaults to \"latest\"")
                    .build());

        /* Parse arguments. */

        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(options, args);
        }
        catch (ParseException exp) {
            printHelp(options);
            return;
        }

        String command = cmd.getOptionValue(COMMAND[0]);
        String date = cmd.getOptionValue(DATE[0], "latest");
        String sourceLanguage = cmd.getOptionValue(SOURCE_LANG[0], "en");
        String targetLanguage = cmd.getOptionValue(TARGET_LANG[0], "en");

        LOG.info(String.format("Will execute: %s %s %s", command, sourceLanguage, targetLanguage, date));

        switch (command) {

            case "dl":
            case "download":
                DumpUtil.download(sourceLanguage, date);
                break;

            case "parse":
                DumpUtil.parse(sourceLanguage, date);
                break;

            case "dict":
                WiktionaryUtil.generateDictionary(sourceLanguage, targetLanguage, date);

            case "clean":
                DumpUtil.clean(sourceLanguage, date);
                break;

            default:
                printHelp(options);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("w2k", options, true);
    }
}
