package edu.self.w2k.util;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class URLUtil {

    private final static Logger LOG = Logger.getLogger(URLUtil.class.getName());

    public static String resolveRedirects(String url) {

        //LOG.info("Resolving: " + url);

        try {
            int responseCode = 0;
            String newURL = url;

            do {
                HttpURLConnection connection = (HttpURLConnection) new URL(newURL).openConnection();
                connection.setRequestMethod("HEAD");
                connection.setInstanceFollowRedirects(false);

                responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    newURL = connection.getHeaderField("Location");
                    //LOG.info(responseCode + ": " + newURL);
                }
            }
            while (responseCode != HttpURLConnection.HTTP_OK);

            return newURL;
        }
        catch (Exception e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
            return null;
        }
    }

    private URLUtil() {
        throw new RuntimeException("Class should not be instantiated.");
    }

    public static void main(String[] args) {
        resolveRedirects("http://download.wikimedia.org/enwiktionary/20180301/enwiktionary-20180301-page_restrictions.sql.gz");
    }
}
