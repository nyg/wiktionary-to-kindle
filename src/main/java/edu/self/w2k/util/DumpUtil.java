package edu.self.w2k.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import de.tudarmstadt.ukp.jwktl.JWKTL;

public final class DumpUtil {

    public static final String DB_DIRECTORY = "db";

    private final static Logger LOG = Logger.getLogger(DumpUtil.class.getName());

    private static final String DUMPS_DIRECTORY = "dumps/";
    private static final String URL = "https://dumps.wikimedia.org/{lang}wiktionary/{date}/{lang}wiktionary-{date}-pages-articles.xml.bz2";

    /**
     * Downloads and saves to disk the Wiktionary dump for the given language
     * and date. If the value of date is "latest" then the latest dump will be
     * searched for.
     *
     * @param lang the Wiktionary language code
     * @param date the date of the dump (format: YYYYMMDD) or "latest"
     */
    public static void download(String lang, String date) {

        String dumpURL = URL.replace("{date}", date).replace("{lang}", lang);

        if (date.equals("latest")) {
            // find latest date via rss feed
            String rssFeedContent = getURLContent(dumpURL + "-rss.xml");
            dumpURL = rssFeedContent.replaceFirst(".*href=\"(.*?)\".*", "$1");
            dumpURL = dumpURL.replace("http:", "https:").replace("/download.", "/dumps.");
        }

        String dumpFileName = Paths.get(dumpURL).getFileName().toString();
        downloadFile(dumpURL, dumpFileName);

        // actually not needed, JWKTL appears to handle .bz2 automatically
        extract(dumpFileName);
    }

    /**
     * Parses the Wiktionary XML dump file to a database.
     *
     * @param lang the Wiktionary dump's language
     * @param date the Wiktionary dump's date, or "latest"
     */
    public static void parse(String lang, String date) {
        File dumpFile = getDumpFile(lang, date);
        Pattern datePattern = Pattern.compile("\\d{8}");
        Matcher matcher = datePattern.matcher(dumpFile.getName());
        matcher.find();
        String group = matcher.group(0);
        File dbDirectory = new File(PathUtil.path(DB_DIRECTORY, lang, group));
        JWKTL.parseWiktionaryDump(dumpFile, dbDirectory, true);
        LOG.info("Done parsing!");
    }

    /**
     * Deletes all .xml and .xml.bz2 files matching the lang and date arguments.
     *
     * @param lang
     * @param date
     */
    public static void clean(String lang, String date) {
        File dumpFile = getDumpFile(lang, date);
        dumpFile.delete();
    }

    /**
     * Downloads and saves to disk the file located at the given URL.
     *
     * @param url the URL at which the file is located
     */
    private static void downloadFile(String url, String fileName) {

        Path filePath = Paths.get(DUMPS_DIRECTORY + fileName);

        if (filePath.toFile().isFile()) {
            LOG.warning("File \"" + fileName + "\" already exists. To redownload it, delete it or run the clean command.");
            return;
        }

        LOG.info("Downloading " + url + " to " + filePath + ".");
        try (InputStream in = new URL(url).openStream()) {
            long size = Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            LOG.info(size + " bytes written to " + fileName);
        }
        catch (Exception e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Returns as a String the content located at the given URL.
     *
     * @param url the URL to get the content from
     * @return the URL's content as a String
     */
    private static String getURLContent(String url) {

        try (InputStream in = new URL(url).openStream();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in))) {

            StringBuilder sb = new StringBuilder();
            bufferedReader.lines().forEach(l -> sb.append(l));
            return sb.toString();
        }
        catch (Exception e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
            return null;
        }
    }

    /**
     * Extracts the .bz2 archive, i.e. the downloaded dump.
     *
     * @param archiveFileName the archive's filename
     */
    private static void extract(String archiveFileName) {

        Path archivePath = Paths.get(DUMPS_DIRECTORY + archiveFileName);
        Path filePath = Paths.get(archivePath.toString().replaceFirst("\\.bz2$", ""));

        if (filePath.toFile().isFile()) {
            LOG.info("File \"" + filePath + "\" already exists. To reextract it, delete it or run the clean command.");
            return;
        }

        LOG.info("Extracting " + archivePath + " to " + filePath + ".");
        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(archivePath));
             BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);
             OutputStream out = Files.newOutputStream(filePath)) {

            final int buffersize = 8192;
            final byte[] buffer = new byte[buffersize];

            int n = 0;
            while (-1 != (n = bzIn.read(buffer))) {
                out.write(buffer, 0, n);
            }

            LOG.info("Done extracting!");
        }
        catch (Exception e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    // TODO redo algo
    private static File getDumpFile(String lang, String date) {

        File resourcesDirectory = new File(DUMPS_DIRECTORY);
        String dateRegex = date.equals("latest") ? "\\d{8}" : date;
        File[] dumps = resourcesDirectory
            .listFiles((dir, name) -> name.matches(lang + "wiktionary-" + dateRegex + "-pages-articles.xml(.bz2)?"));

        if (dumps.length == 0) {
            LOG.severe("No dumps found.");
            return null;
        }

        Arrays.sort(dumps);
        File dumpFile = dumps[dumps.length - 1];

        LOG.info("Dump found: " + dumpFile.getName());
        return dumpFile;
    }

    private DumpUtil() {
        throw new RuntimeException("Class should not be instantiated.");
    }
}
